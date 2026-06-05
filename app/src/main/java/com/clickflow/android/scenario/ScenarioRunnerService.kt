package com.clickflow.android.scenario

import android.app.Service
import android.content.Intent
import android.os.IBinder
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
            scope.launch {
                delay(900)
                stopSelf()
            }
            return
        }

        job = scope.launch {
            var cycle = 0
            while (isActive && (config.infinite || cycle < config.repeatCount)) {
                config.markers.forEach { marker ->
                    if (!isActive) return@launch
                    tapper.performSingleTap(marker.x, marker.y, 70L)
                    delay(config.intervalMs)
                }
                cycle++
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.scenario.START"
        const val ACTION_STOP = "com.clickflow.android.scenario.STOP"
    }
}
