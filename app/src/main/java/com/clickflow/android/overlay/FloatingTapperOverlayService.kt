package com.clickflow.android.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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

private const val PREFS_NAME = "clickflow_tapper"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"
private const val KEY_OVERLAY_MARKERS = "overlay_markers"

class FloatingTapperOverlayService : Service() {
    private data class FloatingMarker(val id: Int, val view: TextView, val params: WindowManager.LayoutParams)

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var startButton: Button? = null
    private val markers = mutableListOf<FloatingMarker>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapJob: Job? = null
    private var running = false
    private var nextId = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
        loadMarkers()
        if (markers.isEmpty()) addMarker(260, 420)
    }

    override fun onDestroy() {
        stopLoop()
        saveMarkers()
        markers.toList().forEach { runCatching { windowManager.removeView(it.view) } }
        panel?.let { runCatching { windowManager.removeView(it) } }
        markers.clear()
        panel = null
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun rectBg(color: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 30f
        setColor(color)
        if (stroke != 0) setStroke(2, stroke)
    }

    private fun circleBg(fill: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        setStroke(7, stroke)
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
            background = rectBg(0xE6181818.toInt(), 0x66333333)
        }
        val start = Button(this).apply {
            text = "▶"
            textSize = 18f
            setTextColor(Color.WHITE)
            setOnClickListener { if (running) stopLoop() else startLoop() }
        }
        val add = Button(this).apply {
            text = "+"
            textSize = 18f
            setOnClickListener {
                if (markers.size < 5) {
                    addMarker(260 + markers.size * 60, 420 + markers.size * 60)
                    saveMarkers()
                }
            }
        }
        val remove = Button(this).apply {
            text = "−"
            textSize = 18f
            setOnClickListener { removeLastMarker(); saveMarkers() }
        }
        val close = Button(this).apply {
            text = "×"
            textSize = 18f
            setOnClickListener { stopSelf() }
        }
        root.addView(start, LinearLayout.LayoutParams(72, 64))
        root.addView(add, LinearLayout.LayoutParams(72, 64))
        root.addView(remove, LinearLayout.LayoutParams(72, 64))
        root.addView(close, LinearLayout.LayoutParams(72, 64))
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 110
        }
        makeDraggable(root, lp, saveOnEnd = false)
        panel = root
        startButton = start
        windowManager.addView(root, lp)
    }

    private fun addMarker(x: Int, y: Int, forcedId: Int? = null) {
        val id = forcedId ?: nextId++
        nextId = maxOf(nextId, id + 1)
        val marker = TextView(this).apply {
            text = "◎"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(0xFF111111.toInt())
            background = circleBg(0xFFFFC542.toInt(), 0xFF111111.toInt())
            elevation = 20f
        }
        val lp = WindowManager.LayoutParams(92, 92, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        makeDraggable(marker, lp, saveOnEnd = true)
        windowManager.addView(marker, lp)
        markers.add(FloatingMarker(id, marker, lp))
    }

    private fun removeLastMarker() {
        if (markers.size <= 1) return
        val last = markers.removeAt(markers.lastIndex)
        runCatching { windowManager.removeView(last.view) }
    }

    private fun makeDraggable(view: View, lp: WindowManager.LayoutParams, saveOnEnd: Boolean) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downRawX = event.rawX; downRawY = event.rawY; startX = lp.x; startY = lp.y; true }
                MotionEvent.ACTION_MOVE -> { lp.x = startX + (event.rawX - downRawX).roundToInt(); lp.y = startY + (event.rawY - downRawY).roundToInt(); windowManager.updateViewLayout(view, lp); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { if (saveOnEnd) saveMarkers(); true }
                else -> true
            }
        }
    }

    private fun startLoop() {
        val service = ClickFlowAccessibilityService.liveInstance ?: return
        val p = prefs()
        val intervalMs = p.getLong(KEY_INTERVAL_MS, 500L)
        val repeatCount = p.getInt(KEY_REPEAT_COUNT, 100)
        val infinite = p.getBoolean(KEY_INFINITE, false)
        running = true
        startButton?.text = "■"
        tapJob = scope.launch {
            var cycle = 0
            while (isActive && running && (infinite || cycle < repeatCount)) {
                markers.toList().forEach { marker ->
                    if (!isActive || !running) return@launch
                    setAllOverlaysVisible(false)
                    delay(90)
                    val centerX = marker.params.x + marker.view.width / 2
                    val centerY = marker.params.y + marker.view.height / 2
                    service.performSingleTap(centerX, centerY, 80L)
                    delay(90)
                    setAllOverlaysVisible(true)
                    delay(intervalMs)
                }
                cycle++
            }
            stopLoop()
        }
    }

    private fun setAllOverlaysVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.INVISIBLE
        panel?.visibility = v
        markers.forEach { it.view.visibility = v }
    }

    private fun stopLoop() {
        running = false
        tapJob?.cancel()
        tapJob = null
        startButton?.text = "▶"
        setAllOverlaysVisible(true)
    }

    private fun saveMarkers() {
        val raw = markers.joinToString(";") { "${it.id},${it.params.x},${it.params.y}" }
        prefs().edit().putString(KEY_OVERLAY_MARKERS, raw).apply()
    }

    private fun loadMarkers() {
        val raw = prefs().getString(KEY_OVERLAY_MARKERS, null).orEmpty()
        raw.split(";").filter { it.isNotBlank() }.take(5).forEach { part ->
            val pieces = part.split(",")
            if (pieces.size == 3) {
                val id = pieces[0].toIntOrNull()
                val x = pieces[1].toIntOrNull()
                val y = pieces[2].toIntOrNull()
                if (id != null && x != null && y != null) addMarker(x, y, id)
            }
        }
    }
}
