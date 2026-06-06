package com.clickflow.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.clickflow.android.MainActivity
import com.clickflow.android.R

/**
 * Shared helper that promotes ClickFlow's worker services to the foreground.
 *
 * Android — and MIUI in particular — kills plain background services quickly. When that
 * happened mid-run, the Accessibility pipeline died with the process and taps silently
 * stopped (the app kept showing "Accessibility ещё запускается"). Running as a foreground
 * service with a visible ongoing notification keeps the process alive for as long as a tap
 * task is active.
 *
 * minSdk is 26, so notification channels always exist. On Android 14+ a foreground service
 * type must be supplied at start time; we use SPECIAL_USE (declared in the manifest).
 */
object ForegroundNotifications {
    private const val CHANNEL_ID = "clickflow_running"

    const val ID_OVERLAY = 1001
    const val ID_IMAGE = 1002
    const val ID_TEXT = 1003
    const val ID_SCENARIO = 1004

    private fun ensureChannel(service: Service) {
        val mgr = service.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ClickFlow",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Активный автотап ClickFlow"
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    fun start(service: Service, id: Int, title: String) {
        ensureChannel(service)
        val tapIntent = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(service, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Нажми, чтобы открыть ClickFlow")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            service.startForeground(id, notification)
        }
    }

    fun stop(service: Service) {
        runCatching { service.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    }
}
