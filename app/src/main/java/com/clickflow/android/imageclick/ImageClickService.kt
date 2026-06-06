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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Looks at the screen via the Accessibility screenshot API and taps where the saved
 * picture is found. No screen-recording prompt. Android limits screenshots to about
 * one per second, so the scan interval is kept above that to stay reliable.
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
                val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
                if (templateId.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(this, "\u041d\u0443\u0436\u0435\u043d Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                if (ClickFlowAccessibilityService.liveInstance == null) {
                    Toast.makeText(this, "\u0412\u043a\u043b\u044e\u0447\u0438 Accessibility", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                begin(templateId)
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showStopChip() {
        try {
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            val chip = TextView(this).apply {
                text = "\u25a0 \u0421\u0442\u043e\u043f \u0444\u043e\u0442\u043e"
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
    private fun begin(templateId: String) {
        val templateMeta = ImageClickTemplateStore.loadTemplates(this).firstOrNull { it.id == templateId }?.normalized() ?: run { stopSelf(); return }
        val templateBitmap = BitmapFactory.decodeFile(templateMeta.filePath) ?: run { stopSelf(); return }
        val service = ClickFlowAccessibilityService.liveInstance ?: run { stopSelf(); return }

        running = true
        showStopChip()
        val infinite = templateMeta.infinite || templateMeta.continuous
        val maxTaps = templateMeta.repeatCount.coerceAtLeast(1)
        val scanInterval = maxOf(templateMeta.intervalMs, 1100L)
        var taps = 0
        scope.launch {
            delay(800) // let the setup screen disappear before the first screenshot
            while (running) {
                val bitmap = capture(service)
                if (bitmap != null) {
                    val regionLeftPx = (bitmap.width * templateMeta.regionLeft).toInt()
                    val regionTopPx = (bitmap.height * templateMeta.regionTop).toInt()
                    val regionRightPx = (bitmap.width * templateMeta.regionRight).toInt()
                    val regionBottomPx = (bitmap.height * templateMeta.regionBottom).toInt()
                    val match = BitmapTemplateMatcher.findBest(
                        screen = bitmap,
                        template = templateBitmap,
                        threshold = templateMeta.threshold,
                        regionLeftPx = regionLeftPx,
                        regionTopPx = regionTopPx,
                        regionRightPx = regionRightPx,
                        regionBottomPx = regionBottomPx,
                        scaleMin = templateMeta.scaleMin,
                        scaleMax = templateMeta.scaleMax,
                    )
                    if (match != null) {
                        val tapX = match.x + (match.width * templateMeta.tapX).toInt()
                        val tapY = match.y + (match.height * templateMeta.tapY).toInt()
                        service.performSingleTap(tapX, tapY, 70L)
                        taps++
                        if (!infinite && taps >= maxTaps) { running = false; bitmap.recycle(); break }
                    }
                    bitmap.recycle()
                }
                delay(scanInterval) // respect the ~1/sec screenshot limit
            }
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
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.imageclick.START"
        const val ACTION_STOP = "com.clickflow.android.imageclick.STOP"
        const val EXTRA_TEMPLATE_ID = "template_id"
    }
}
