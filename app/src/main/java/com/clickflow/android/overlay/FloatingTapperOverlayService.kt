package com.clickflow.android.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Minimal floating overlay for real-device testing.
 *
 * UX goal:
 * - One draggable marker shown above other apps.
 * - One Start/Stop button.
 * - No internal safety checklist, no scenario editor, no extra confirmations.
 *
 * Android still requires system permissions:
 * - Overlay permission to show this bubble.
 * - Accessibility Service to dispatch taps.
 */
class FloatingTapperOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapJob: Job? = null
    private var running = false

    private var intervalMs: Long = 500L
    private var repeatCount: Int = 100

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onDestroy() {
        stopLoop()
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        super.onDestroy()
    }

    private fun showOverlay() {
        if (root != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 8, 10, 8)
            setBackgroundColor(0xDD1E1E1E.toInt())
        }

        val marker = TextView(this).apply {
            text = "＋"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xAA3F51B5.toInt())
            layoutParams = LinearLayout.LayoutParams(120, 90)
        }

        val status = TextView(this).apply {
            text = "Готово"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val startStop = Button(this).apply {
            text = "Старт"
            textSize = 12f
            setOnClickListener {
                if (running) {
                    stopLoop()
                    text = "Старт"
                    status.text = "Стоп"
                } else {
                    startLoop(marker, status, this)
                }
            }
        }

        val close = Button(this).apply {
            text = "×"
            textSize = 12f
            setOnClickListener { stopSelf() }
        }

        container.addView(marker)
        container.addView(status)
        container.addView(startStop)
        container.addView(close)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 250
        }

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0

        marker.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = p.x
                    startY = p.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (event.rawX - downRawX).roundToInt()
                    p.y = startY + (event.rawY - downRawY).roundToInt()
                    windowManager.updateViewLayout(container, p)
                    true
                }
                else -> true
            }
        }

        root = container
        params = lp
        windowManager.addView(container, lp)
    }

    private fun startLoop(marker: View, status: TextView, button: Button) {
        val service = ClickFlowAccessibilityService.liveInstance
        if (service == null) {
            status.text = "Включи Accessibility"
            return
        }
        val overlayRoot = root ?: return
        val lp = params ?: return

        running = true
        button.text = "Стоп"
        status.text = "Работает"

        tapJob = scope.launch {
            repeat(repeatCount) { index ->
                if (!isActive || !running) return@launch

                val centerX = lp.x + overlayRoot.width / 2
                val centerY = lp.y + marker.height / 2

                // Hide overlay briefly so the tap hits the underlying app, not the overlay itself.
                overlayRoot.visibility = View.INVISIBLE
                delay(70)
                val ok = service.performSingleTap(centerX, centerY, 70L)
                delay(70)
                overlayRoot.visibility = View.VISIBLE

                status.text = if (ok) "Тап ${index + 1}" else "Ошибка тапа"
                delay(intervalMs)
            }
            running = false
            button.text = "Старт"
            status.text = "Готово"
        }
    }

    private fun stopLoop() {
        running = false
        tapJob?.cancel()
        tapJob = null
    }
}
