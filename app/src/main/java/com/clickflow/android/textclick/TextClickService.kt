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
 * matching word/line. No screen-recording prompt. Screenshots are throttled by
 * Android to ~1/sec, so the scan interval stays above that.
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
                    Toast.makeText(this, "\u041d\u0443\u0436\u0435\u043d Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                ForegroundNotifications.start(this, ForegroundNotifications.ID_TEXT, "ClickFlow: \u0430\u0432\u0442\u043e\u0442\u0430\u043f \u043f\u043e \u0442\u0435\u043a\u0441\u0442\u0443")
                scope.launch {
                    val service = ClickFlowAccessibilityService.awaitInstance()
                    if (service == null) {
                        val msg = if (ClickFlowAccessibilityService.isEnabledInSettings(this@TextClickService))
                            "Accessibility \u0435\u0449\u0451 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f, \u043f\u043e\u043f\u0440\u043e\u0431\u0443\u0439 \u0435\u0449\u0451 \u0440\u0430\u0437"
                        else "\u0412\u043a\u043b\u044e\u0447\u0438 Accessibility \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u0445"
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
                text = "\u25a0 \u0421\u0442\u043e\u043f \u0442\u0435\u043a\u0441\u0442"
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
        if (config.query.isBlank()) { stopSelf(); return }
        val service = ClickFlowAccessibilityService.liveInstance ?: run { stopSelf(); return }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        running = true
        showStopChip()
        val infinite = config.infinite || config.continuous
        val maxTaps = config.repeatCount.coerceAtLeast(1)
        val scanInterval = maxOf(config.intervalMs, 1100L)
        var taps = 0
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
                        Toast.makeText(this@TextClickService, "\u041d\u0435 \u0443\u0434\u0430\u0451\u0442\u0441\u044f \u0441\u0434\u0435\u043b\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442 \u044d\u043a\u0440\u0430\u043d\u0430. \u041f\u0440\u043e\u0432\u0435\u0440\u044c \u0441\u043f\u0435\u0446. \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 (Accessibility) \u0438 \u043f\u043e\u043f\u0440\u043e\u0431\u0443\u0439 \u0441\u043d\u043e\u0432\u0430.", Toast.LENGTH_LONG).show()
                        running = false
                        break
                    }
                    delay(scanInterval)
                    continue
                }
                captureFails = 0
                val result = runCatching { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await() }.getOrNull()
                val box = result?.let { findTextBox(it, config) }
                if (box != null) {
                    service.performSingleTap(box.centerX(), box.centerY(), 70L)
                    taps++
                    if (!infinite && taps >= maxTaps) { running = false; bitmap.recycle(); break }
                }
                bitmap.recycle()
                delay(scanInterval)
            }
            recognizer.close()
            stopSelf()
        }
    }

    private fun findTextBox(text: Text, config: TextClickConfig): android.graphics.Rect? {
        val query = if (config.ignoreCase) config.query.lowercase() else config.query
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
