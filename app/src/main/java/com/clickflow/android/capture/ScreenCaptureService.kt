package com.clickflow.android.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import android.os.Looper
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat

/**
 * Foreground service that performs a SINGLE in-memory screen capture via MediaProjection
 * (Step 66, Part 2).
 *
 * INVARIANTS (mirrored from [ScreenCaptureController] / [ScreenCaptureState]):
 *   - Captures exactly one frame, then tears the whole pipeline down.
 *   - The frame is read for its dimensions only and is NEVER written to disk, exported, or
 *     analyzed (no OCR / template matching in this step).
 *   - Requires the user's explicit MediaProjection consent, obtained by [ScreenCaptureActivity].
 *
 * All lifecycle transitions are mirrored into [ScreenCaptureRepository] so the UI can observe them.
 */
class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            tearDown()
            ScreenCaptureRepository.stop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCaptureAndService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                if (resultCode == 0 || data == null) {
                    ScreenCaptureRepository.onError("missing_projection_consent")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundCompat()
                beginCapture(resultCode, data)
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        try {
            ScreenCaptureRepository.startCapture()
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = mpm.getMediaProjection(resultCode, data)
            if (mp == null) {
                ScreenCaptureRepository.onError("projection_unavailable")
                stopCaptureAndService()
                return
            }
            projection = mp

            handlerThread = HandlerThread("screen-capture").also { it.start() }
            handler = Handler(handlerThread!!.looper)

            // Android 14+ requires a registered callback before creating a virtual display.
            mp.registerCallback(projectionCallback, handler)

            val metrics: DisplayMetrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ r ->
                val image = try {
                    r.acquireLatestImage()
                } catch (t: Throwable) {
                    null
                }
                if (image != null) {
                    val w = image.width
                    val h = image.height
                    // Read dimensions only. We never copy, persist, export, or analyze the pixels.
                    image.close()
                    ScreenCaptureRepository.onFrameCaptured(w, h, System.currentTimeMillis())
                    // Single frame only — tear the pipeline down immediately.
                    tearDown()
                    Handler(Looper.getMainLooper()).post { stopForegroundOnly() }
                }
            }, handler)

            virtualDisplay = mp.createVirtualDisplay(
                "clickflow-capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )
        } catch (t: Throwable) {
            ScreenCaptureRepository.onError(t.message ?: "capture_failed")
            stopCaptureAndService()
        }
    }

    private fun startForegroundCompat() {
        val channelId = ensureChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClickFlow")
            .setContentText("Screen capture (preview, in-memory only)")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Screen capture",
                    NotificationManager.IMPORTANCE_LOW,
                )
                channel.description = "Shown only while ClickFlow captures a single screen frame"
                nm.createNotificationChannel(channel)
            }
        }
        return CHANNEL_ID
    }

    private fun tearDown() {
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {
        }
        virtualDisplay = null
        try {
            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
        } catch (_: Throwable) {
        }
        imageReader = null
        try {
            projection?.unregisterCallback(projectionCallback)
            projection?.stop()
        } catch (_: Throwable) {
        }
        projection = null
        try {
            handlerThread?.quitSafely()
        } catch (_: Throwable) {
        }
        handlerThread = null
        handler = null
    }

    private fun stopForegroundOnly() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        stopSelf()
    }

    private fun stopCaptureAndService() {
        tearDown()
        stopForegroundOnly()
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.capture.START"
        const val ACTION_STOP = "com.clickflow.android.capture.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 4266
    }
}
