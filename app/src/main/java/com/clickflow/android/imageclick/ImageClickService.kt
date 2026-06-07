package com.clickflow.android.imageclick

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.service.ForegroundNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Looks at the screen via the Accessibility screenshot API and taps where the saved
 * picture(s) are found. Supports several templates at once ("multitap"): every active
 * template is searched for in the SAME screenshot and tapped within the same scan cycle,
 * each with its own repeat counter. No screen-recording prompt. Android limits screenshots
 * to about one per second, so the scan interval is kept above that to stay reliable.
 */
class ImageClickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false
    private var stopChip: View? = null
    private var wm: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                val ids = parseTemplateIds(intent)
                if (ids.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(this, "Нужен Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                ForegroundNotifications.start(this, ForegroundNotifications.ID_IMAGE, "ClickFlow: автотап по фото")
                scope.launch {
                    val service = ClickFlowAccessibilityService.awaitInstance()
                    if (service == null) {
                        val msg = if (ClickFlowAccessibilityService.isEnabledInSettings(this@ImageClickService))
                            "ClickFlow включён, но служба не запущена. Выключи и снова включи ClickFlow в спец. возможностях."
                        else "Включи Accessibility в настройках"
                        Toast.makeText(this@ImageClickService, msg, Toast.LENGTH_LONG).show()
                        stopSelf(); return@launch
                    }
                    begin(ids)
                }
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun parseTemplateIds(intent: Intent): List<String> {
        intent.getStringExtra(EXTRA_TEMPLATE_IDS)?.let { joined ->
            return joined.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        }
        val single = intent.getStringExtra(EXTRA_TEMPLATE_ID)
        return if (single.isNullOrBlank()) emptyList() else listOf(single)
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showStopChip() {
        try {
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            val chip = TextView(this).apply {
                text = "\u25a0 Стоп фото"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(30, 18, 30, 18)
                background = GradientDrawable().apply { cornerRadius = 32f; setColor(0xF2D32F2F.toInt()) }
                setOnClickListener { stopSelf() }
            }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 120 }
            manager.addView(chip, lp)
            stopChip = chip
            wm = manager
        } catch (_: Throwable) {}
    }

    private fun removeStopChip() {
        stopChip?.let { chip -> runCatching { wm?.removeView(chip) } }
        stopChip = null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun begin(templateIds: List<String>) {
        val all = ImageClickTemplateStore.loadTemplates(this)
        val targets = templateIds
            .mapNotNull { id -> all.firstOrNull { it.id == id }?.normalized() }
            .mapNotNull { meta ->
                val bmp = BitmapFactory.decodeFile(meta.filePath) ?: return@mapNotNull null
                ImageTarget(meta, bmp)
            }
            .toMutableList()
        if (targets.isEmpty()) { stopSelf(); return }
        val service = ClickFlowAccessibilityService.liveInstance ?: run {
            targets.forEach { it.bitmap.recycle() }
            stopSelf(); return
        }

        running = true
        showStopChip()
        // Scan as fast as the fastest target wants, but never faster than the screenshot limit.
        val scanInterval = maxOf(targets.minOf { it.meta.intervalMs }, 1100L)
        scope.launch {
            delay(800) // let the setup screen disappear before the first screenshot
            var captureFails = 0
            while (running) {
                val bitmap = capture(service)
                if (bitmap == null) {
                    // takeScreenshot can transiently fail (rate limit) but should recover; if it keeps
                    // failing the feature would otherwise loop forever doing nothing, so tell the user.
                    captureFails++
                    if (captureFails >= 5) {
                        Toast.makeText(this@ImageClickService, "Не удаётся сделать скриншот экрана. Проверь спец. возможности (Accessibility) и попробуй снова.", Toast.LENGTH_LONG).show()
                        running = false
                        break
                    }
                    delay(scanInterval)
                    continue
                }
                captureFails = 0
                // Heavy pixel matching runs off the main thread so the UI never blocks. Every active
                // target is searched for in this one screenshot; we collect the tap points first.
                val hits = withContext(Dispatchers.Default) {
                    targets.mapNotNull { t ->
                        val regionLeftPx = (bitmap.width * t.meta.regionLeft).toInt()
                        val regionTopPx = (bitmap.height * t.meta.regionTop).toInt()
                        val regionRightPx = (bitmap.width * t.meta.regionRight).toInt()
                        val regionBottomPx = (bitmap.height * t.meta.regionBottom).toInt()
                        val match = BitmapTemplateMatcher.findBest(
                            screen = bitmap,
                            template = t.bitmap,
                            threshold = t.meta.threshold,
                            regionLeftPx = regionLeftPx,
                            regionTopPx = regionTopPx,
                            regionRightPx = regionRightPx,
                            regionBottomPx = regionBottomPx,
                            scaleMin = t.meta.scaleMin,
                            scaleMax = t.meta.scaleMax,
                        )
                        if (match == null) {
                            null
                        } else {
                            val tapX = match.x + (match.width * t.meta.tapX).toInt()
                            val tapY = match.y + (match.height * t.meta.tapY).toInt()
                            Triple(t, tapX, tapY)
                        }
                    }
                }
                // Tap each found target on the main thread and advance its own counter.
                for ((t, tapX, tapY) in hits) {
                    service.performSingleTap(tapX, tapY, 70L)
                    t.taps++
                }
                // Retire targets that have reached their own repeat count.
                val finished = targets.filter { !it.infinite && it.taps >= it.maxTaps }
                finished.forEach { it.bitmap.recycle() }
                targets.removeAll(finished.toSet())
                bitmap.recycle()
                if (targets.isEmpty()) { running = false; break }
                delay(scanInterval) // respect the ~1/sec screenshot limit
            }
            targets.forEach { it.bitmap.recycle() }
            stopSelf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(service: ClickFlowAccessibilityService): Bitmap? = suspendCancellableCoroutine { cont ->
        stopChip?.visibility = View.INVISIBLE
        service.captureScreenBitmap { bitmap ->
            stopChip?.let { it.post { it.visibility = View.VISIBLE } }
            if (cont.isActive) cont.resume(bitmap)
        }
    }

    override fun onDestroy() {
        running = false
        removeStopChip()
        ForegroundNotifications.stop(this)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.imageclick.START"
        const val ACTION_STOP = "com.clickflow.android.imageclick.STOP"
        const val EXTRA_TEMPLATE_ID = "template_id"
        const val EXTRA_TEMPLATE_IDS = "template_ids"
    }
}

/** Runtime state for one template inside a multi-target run. */
private class ImageTarget(
    val meta: ImageClickTemplate,
    val bitmap: Bitmap,
) {
    val infinite: Boolean = meta.infinite || meta.continuous
    val maxTaps: Int = meta.repeatCount.coerceAtLeast(1)
    var taps: Int = 0
}
