package com.clickflow.android.textclick

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
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
import com.clickflow.android.core.Premium
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.service.ForegroundNotifications
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Reads on-screen text via the Accessibility screenshot + ML Kit OCR and taps the
 * matching word/line. Supports several texts at once ("multitap"): one OCR pass per
 * scan, then every configured text found in it is tapped, each with its own repeat
 * counter. No screen-recording prompt. Screenshots are throttled by Android to ~1/sec,
 * so the scan interval stays above that.
 */
class TextClickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false
    private var stopChip: View? = null
    private var wm: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(this, "Нужен Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                ForegroundNotifications.start(this, ForegroundNotifications.ID_TEXT, "ClickFlow: автотап по тексту")
                scope.launch {
                    val service = ClickFlowAccessibilityService.awaitInstance()
                    if (service == null) {
                        val msg = if (ClickFlowAccessibilityService.isEnabledInSettings(this@TextClickService))
                            "ClickFlow включён, но служба не запущена. Выключи и снова включи ClickFlow в спец. возможностях."
                        else "Включи Accessibility в настройках"
                        Toast.makeText(this@TextClickService, msg, Toast.LENGTH_LONG).show()
                        stopSelf(); return@launch
                    }
                    begin()
                }
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
                text = "\u25a0 Стоп текст"
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
    private fun begin() {
        val config = TextClickStore.load(this)
        val queries = config.queries.filter { it.isNotBlank() }.take(Premium.targetLimit(this))
        if (queries.isEmpty()) { stopSelf(); return }
        val service = ClickFlowAccessibilityService.liveInstance ?: run { stopSelf(); return }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        running = true
        showStopChip()
        val infinite = config.infinite || config.continuous
        val maxTaps = config.repeatCount.coerceAtLeast(1)
        val scanInterval = maxOf(config.intervalMs, 1100L)
        val taps = HashMap<String, Int>()
        val remaining = queries.toMutableList()
        scope.launch {
            delay(800)
            var captureFails = 0
            while (running) {
                val bitmap = capture(service)
                if (bitmap == null) {
                    // takeScreenshot can transiently fail (rate limit) but should recover; if it keeps
                    // failing the feature would otherwise loop forever doing nothing, so tell the user.
                    captureFails++
                    if (captureFails >= 5) {
                        Toast.makeText(this@TextClickService, "Не удаётся сделать скриншот экрана. Проверь спец. возможности (Accessibility) и попробуй снова.", Toast.LENGTH_LONG).show()
                        running = false
                        break
                    }
                    delay(scanInterval)
                    continue
                }
                captureFails = 0
                val result = runCatching { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await() }.getOrNull()
                if (result != null) {
                    val finished = mutableListOf<String>()
                    for (q in remaining) {
                        val box = findTextBox(result, q, config) ?: continue
                        service.performSingleTap(box.centerX(), box.centerY(), 70L)
                        val n = (taps[q] ?: 0) + 1
                        taps[q] = n
                        if (!infinite && n >= maxTaps) finished.add(q)
                    }
                    remaining.removeAll(finished.toSet())
                }
                bitmap.recycle()
                if (remaining.isEmpty()) { running = false; break }
                delay(scanInterval)
            }
            recognizer.close()
            stopSelf()
        }
    }

    private fun findTextBox(text: Text, rawQuery: String, config: TextClickConfig): android.graphics.Rect? {
        val query = if (config.ignoreCase) rawQuery.lowercase() else rawQuery
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val value = if (config.ignoreCase) line.text.lowercase() else line.text
                val matched = if (config.contains) value.contains(query) else value == query
                if (matched) return line.boundingBox
                for (element in line.elements) {
                    val ev = if (config.ignoreCase) element.text.lowercase() else element.text
                    val em = if (config.contains) ev.contains(query) else ev == query
                    if (em) return element.boundingBox
                }
            }
        }
        return null
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
        const val ACTION_START = "com.clickflow.android.textclick.START"
        const val ACTION_STOP = "com.clickflow.android.textclick.STOP"
    }
}
