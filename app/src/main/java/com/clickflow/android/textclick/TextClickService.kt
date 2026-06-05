package com.clickflow.android.textclick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer

class TextClickService : Service() {
    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() { stopSelf() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
                if (resultCode == 0 || data == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundCompat("Ищу текст")
                begin(resultCode, data)
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun begin(resultCode: Int, data: Intent) {
        val config = TextClickStore.load(this)
        if (config.query.isBlank()) {
            stopSelf()
            return
        }
        val tapper = ClickFlowAccessibilityService.liveInstance ?: run {
            stopSelf()
            return
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)
        handlerThread = HandlerThread("text-click-capture").also { it.start() }
        handler = Handler(handlerThread!!.looper)
        projection?.registerCallback(projectionCallback, handler)

        val metrics: DisplayMetrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = projection?.createVirtualDisplay(
            "clickflow-text-click",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        )

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        running = true
        scope.launch {
            var attempts = 0
            while (running) {
                val bitmap = acquireBitmap(reader)
                if (bitmap != null) {
                    attempts++
                    val result = runCatching { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await() }.getOrNull()
                    val box = result?.let { findTextBox(it, config) }
                    if (box != null) {
                        val tapX = box.centerX()
                        val tapY = box.centerY()
                        startForegroundCompat("Текст найден · тап")
                        tapper.performSingleTap(tapX, tapY, 70L)
                        if (!config.continuous) {
                            running = false
                            bitmap.recycle()
                            break
                        }
                        delay(700)
                    } else if (attempts % 4 == 0) {
                        startForegroundCompat("Ищу текст · попытка $attempts")
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

    private fun acquireBitmap(reader: ImageReader): Bitmap? {
        val image = try { reader.acquireLatestImage() } catch (_: Throwable) { null } ?: return null
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also { bitmap.recycle() }
        } catch (_: Throwable) {
            null
        } finally {
            image.close()
        }
    }

    private fun startForegroundCompat(text: String) {
        val channelId = ensureChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClickFlow")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Text click", NotificationManager.IMPORTANCE_LOW))
            }
        }
        return CHANNEL_ID
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { projection?.unregisterCallback(projectionCallback) }
        runCatching { projection?.stop() }
        projection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
        handler = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.textclick.START"
        const val ACTION_STOP = "com.clickflow.android.textclick.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL_ID = "text_click"
        private const val NOTIFICATION_ID = 4271
    }
}
