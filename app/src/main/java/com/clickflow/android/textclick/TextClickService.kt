package com.clickflow.android.textclick

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.clickflow.android.permissions.ClickFlowAccessibilityService
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

class TextClickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                begin()
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun begin() {
        val config = TextClickStore.load(this)
        if (config.query.isBlank()) {
            stopSelf()
            return
        }
        val service = ClickFlowAccessibilityService.liveInstance ?: run {
            stopSelf()
            return
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        running = true
        scope.launch {
            while (running) {
                val bitmap = capture(service)
                if (bitmap != null) {
                    val result = runCatching { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await() }.getOrNull()
                    val box = result?.let { findTextBox(it, config) }
                    if (box != null) {
                        service.performSingleTap(box.centerX(), box.centerY(), 80L)
                        if (!config.continuous) {
                            running = false
                            bitmap.recycle()
                            break
                        }
                        delay(700)
                    }
                    bitmap.recycle()
                }
                delay(600)
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
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(service: ClickFlowAccessibilityService): Bitmap? = suspendCancellableCoroutine { cont ->
        service.captureScreenBitmap { bitmap ->
            if (cont.isActive) cont.resume(bitmap)
        }
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.textclick.START"
        const val ACTION_STOP = "com.clickflow.android.textclick.STOP"
    }
}
