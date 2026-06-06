package com.clickflow.android.imageclick

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
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

class ImageClickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                val templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID)
                if (templateId.isNullOrBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                begin(templateId)
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun begin(templateId: String) {
        val templateMeta = ImageClickTemplateStore.loadTemplates(this).firstOrNull { it.id == templateId }?.normalized() ?: run {
            stopSelf()
            return
        }
        val templateBitmap = BitmapFactory.decodeFile(templateMeta.filePath) ?: run {
            stopSelf()
            return
        }
        val service = ClickFlowAccessibilityService.liveInstance ?: run {
            stopSelf()
            return
        }

        running = true
        scope.launch {
            var attempts = 0
            while (running) {
                val bitmap = capture(service)
                if (bitmap != null) {
                    attempts++
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
                        service.performSingleTap(tapX, tapY, 80L)
                        if (!templateMeta.continuous) {
                            running = false
                            bitmap.recycle()
                            break
                        }
                        delay(700)
                    }
                    bitmap.recycle()
                }
                delay(if (attempts == 0) 250 else 450)
            }
            stopSelf()
        }
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
        const val ACTION_START = "com.clickflow.android.imageclick.START"
        const val ACTION_STOP = "com.clickflow.android.imageclick.STOP"
        const val EXTRA_TEMPLATE_ID = "template_id"
    }
}
