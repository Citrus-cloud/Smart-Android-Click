package com.clickflow.android.scenario

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScenarioRunnerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> startScenario()
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun startScenario() {
        job?.cancel()
        val tapper = ClickFlowAccessibilityService.liveInstance ?: run {
            stopSelf()
            return
        }
        val config = ScenarioStore.load(this)
        if (config.markers.isEmpty()) {
            startForegroundCompat("Нет overlay-меток")
            scope.launch {
                delay(900)
                stopSelf()
            }
            return
        }

        startForegroundCompat("Сценарий запущен")
        job = scope.launch {
            var cycle = 0
            while (isActive && (config.infinite || cycle < config.repeatCount)) {
                config.markers.forEachIndexed { index, marker ->
                    if (!isActive) return@launch
                    startForegroundCompat("Цикл ${cycle + 1}/${if (config.infinite) "∞" else config.repeatCount} · метка ${index + 1}")
                    tapper.performSingleTap(marker.x, marker.y, 70L)
                    delay(config.intervalMs)
                }
                cycle++
            }
            stopSelf()
        }
    }

    private fun startForegroundCompat(text: String) {
        val channelId = ensureChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClickFlow")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Scenario runner", NotificationManager.IMPORTANCE_LOW))
            }
        }
        return CHANNEL_ID
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.scenario.START"
        const val ACTION_STOP = "com.clickflow.android.scenario.STOP"
        private const val CHANNEL_ID = "scenario_runner"
        private const val NOTIFICATION_ID = 4272
    }
}
