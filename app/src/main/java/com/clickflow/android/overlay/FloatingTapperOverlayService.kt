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

/**
 * The floating tap marker is the main feature: a draggable target you place over
 * any app, with all controls attached to it (start/stop, add/remove targets,
 * interval). No in-app dummy tapper anymore.
 */
class FloatingTapperOverlayService : Service() {

    private data class FloatingMarker(val id: Int, val view: View, val params: WindowManager.LayoutParams)

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var startButton: Button? = null
    private var intervalLabel: TextView? = null
    private val markers = mutableListOf<FloatingMarker>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapJob: Job? = null
    private var running = false
    private var nextId = 1
    private var intervalMs = 500L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        intervalMs = prefs().getLong(KEY_INTERVAL_MS, 500L)
        showPanel()
        loadMarkers()
        if (markers.isEmpty()) addMarker(280, 480)
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

    private fun panelBg(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 28f
        setColor(0xF21B1B1B.toInt())
        setStroke(2, 0x33FFFFFF)
    }

    private fun pill(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setPadding(8, 4, 8, 4)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f
            setColor(0xFF2E2E2E.toInt())
        }
        setOnClickListener { onClick() }
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            background = panelBg()
        }

        val title = TextView(this).apply {
            text = "Метка"
            textSize = 13f
            setTextColor(0xFFBDBDBD.toInt())
            gravity = Gravity.CENTER
        }

        val start = pill("▶ Старт") { if (running) stopLoop() else startLoop() }

        val markerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val add = pill("+ Точка") {
            if (markers.size < 5) { addMarker(280 + markers.size * 60, 480 + markers.size * 60); saveMarkers() }
        }
        val remove = pill("− Точка") { removeLastMarker(); saveMarkers() }
        markerRow.addView(add, lp(0, 78, 1f))
        markerRow.addView(space())
        markerRow.addView(remove, lp(0, 78, 1f))

        val intervalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val minus = pill("−") { setInterval(intervalMs - 100) }
        val label = TextView(this).apply {
            text = "$intervalMs мс"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val plus = pill("+") { setInterval(intervalMs + 100) }
        intervalRow.addView(minus, lp(70, 70, 0f))
        intervalRow.addView(label, lp(0, 70, 1f))
        intervalRow.addView(plus, lp(70, 70, 0f))

        val close = pill("× Закрыть") { stopSelf() }

        root.addView(title, lp(260, 40, 0f))
        root.addView(start, lp(260, 84, 0f))
        root.addView(spaceV())
        root.addView(markerRow, lp(260, 0, 0f))
        root.addView(spaceV())
        root.addView(intervalRow, lp(260, 0, 0f))
        root.addView(spaceV())
        root.addView(close, lp(260, 70, 0f))

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 90
        }
        makeDraggable(title, lp, root, saveOnEnd = false)
        panel = root
        startButton = start
        intervalLabel = label
        windowManager.addView(root, lp)
    }

    private fun lp(w: Int, h: Int, weight: Float) = LinearLayout.LayoutParams(
        if (w == 0) LinearLayout.LayoutParams.MATCH_PARENT else w,
        if (h == 0) LinearLayout.LayoutParams.WRAP_CONTENT else h,
        weight,
    )

    private fun space(): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(10, 1) }
    private fun spaceV(): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 10) }

    private fun setInterval(value: Long) {
        intervalMs = value.coerceIn(100L, 5000L)
        intervalLabel?.text = "$intervalMs мс"
        prefs().edit().putLong(KEY_INTERVAL_MS, intervalMs).apply()
    }

    private fun markerView(id: Int): View {
        // Target look: translucent ring with a crosshair center. Clearly shows the tap point.
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x552D7DF6)
            setStroke(8, 0xFF2D7DF6.toInt())
        }
        return TextView(this).apply {
            text = "✛"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = ring
            elevation = 22f
        }
    }

    private fun addMarker(x: Int, y: Int, forcedId: Int? = null) {
        val id = forcedId ?: nextId++
        nextId = maxOf(nextId, id + 1)
        val view = markerView(id)
        val lp = WindowManager.LayoutParams(120, 120, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        makeDraggable(view, lp, view, saveOnEnd = true)
        windowManager.addView(view, lp)
        markers.add(FloatingMarker(id, view, lp))
    }

    private fun removeLastMarker() {
        if (markers.size <= 1) return
        val last = markers.removeAt(markers.lastIndex)
        runCatching { windowManager.removeView(last.view) }
    }

    private fun makeDraggable(handle: View, lp: WindowManager.LayoutParams, moved: View, saveOnEnd: Boolean) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downRawX = event.rawX; downRawY = event.rawY; startX = lp.x; startY = lp.y; true }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - downRawX).roundToInt()
                    lp.y = startY + (event.rawY - downRawY).roundToInt()
                    runCatching { windowManager.updateViewLayout(moved, lp) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { if (saveOnEnd) saveMarkers(); true }
                else -> true
            }
        }
    }

    private fun startLoop() {
        val service = ClickFlowAccessibilityService.liveInstance ?: return
        val p = prefs()
        val repeatCount = p.getInt(KEY_REPEAT_COUNT, 100)
        val infinite = p.getBoolean(KEY_INFINITE, false)
        running = true
        startButton?.text = "■ Стоп"
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
        startButton?.text = "▶ Старт"
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
