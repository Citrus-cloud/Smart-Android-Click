package com.clickflow.android.imageclick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ImageClickService : Service() {
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
                val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
                if (resultCode == 0 || data == null || templateId.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundCompat("Ищу картинку")
                begin(resultCode, data, templateId)
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun begin(resultCode: Int, data: Intent, templateId: String) {
        val templateMeta = ImageClickTemplateStore.loadTemplates(this).firstOrNull { it.id == templateId }?.normalized() ?: run {
            stopSelf()
            return
        }
        val templateBitmap = BitmapFactory.decodeFile(templateMeta.filePath) ?: run {
            stopSelf()
            return
        }
        val tapper = ClickFlowAccessibilityService.liveInstance ?: run {
            stopSelf()
            return
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)
        handlerThread = HandlerThread("image-click-capture").also { it.start() }
        handler = Handler(handlerThread!!.looper)
        projection?.registerCallback(projectionCallback, handler)

        val metrics: DisplayMetrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = projection?.createVirtualDisplay(
            "clickflow-image-click",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        )

        val regionLeftPx = (width * templateMeta.regionLeft).toInt()
        val regionTopPx = (height * templateMeta.regionTop).toInt()
        val regionRightPx = (width * templateMeta.regionRight).toInt()
        val regionBottomPx = (height * templateMeta.regionBottom).toInt()

        running = true
        scope.launch {
            var attempts = 0
            while (running) {
                val bitmap = acquireBitmap(reader)
                if (bitmap != null) {
                    attempts++
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
                        startForegroundCompat("Найдено ${(match.confidence * 100).toInt()}% · ${(match.scale * 100).toInt()}% · тап")
                        tapper.performSingleTap(tapX, tapY, 70L)
                        if (!templateMeta.continuous) {
                            running = false
                            bitmap.recycle()
                            break
                        }
                        delay(700)
                    } else if (attempts % 5 == 0) {
                        startForegroundCompat("Ищу картинку · попытка $attempts")
                    }
                    bitmap.recycle()
                }
                delay(420)
            }
            stopSelf()
        }
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
                nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Image click", NotificationManager.IMPORTANCE_LOW))
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
        const val ACTION_START = "com.clickflow.android.imageclick.START"
        const val ACTION_STOP = "com.clickflow.android.imageclick.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        const val EXTRA_TEMPLATE_ID = "template_id"
        private const val CHANNEL_ID = "image_click"
        private const val NOTIFICATION_ID = 4270
    }
}
