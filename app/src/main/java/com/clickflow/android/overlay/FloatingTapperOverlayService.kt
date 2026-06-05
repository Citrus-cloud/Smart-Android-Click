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
    private var panelStatus: TextView? = null
    private var panelStartButton: Button? = null
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
        updateStatus("${markers.size} меток")
    }

    override fun onDestroy() {
        stopLoop()
        saveMarkers()
        markers.toList().forEach { runCatching { windowManager.removeView(it.view) } }
        markers.clear()
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun roundedBg(color: Int, strokeColor: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 34f
        setColor(color)
        strokeColor?.let { setStroke(3, it) }
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = roundedBg(0xF20D0E10.toInt(), 0x55FFFFFF)
        }
        val title = TextView(this).apply {
            text = "ClickFlow"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply {
            text = "Готово"
            textSize = 12f
            setTextColor(0xFFD9D9D9.toInt())
            gravity = Gravity.CENTER
        }
        val startStop = Button(this).apply {
            text = "Старт"
            textSize = 12f
            setOnClickListener { if (running) stopLoop() else startLoop() }
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val add = Button(this).apply {
            text = "+"
            textSize = 14f
            setOnClickListener {
                if (markers.size < 5) {
                    addMarker(260 + markers.size * 55, 420 + markers.size * 55)
                    saveMarkers()
                    updateStatus("${markers.size} меток")
                }
            }
        }
        val remove = Button(this).apply {
            text = "−"
            textSize = 14f
            setOnClickListener {
                removeLastMarker()
                saveMarkers()
                updateStatus("${markers.size} меток")
            }
        }
        val close = Button(this).apply {
            text = "×"
            textSize = 14f
            setOnClickListener { stopSelf() }
        }
        row.addView(add, LinearLayout.LayoutParams(80, 70))
        row.addView(remove, LinearLayout.LayoutParams(80, 70))
        row.addView(close, LinearLayout.LayoutParams(80, 70))
        root.addView(title, LinearLayout.LayoutParams(240, 44))
        root.addView(status, LinearLayout.LayoutParams(240, 40))
        root.addView(startStop, LinearLayout.LayoutParams(240, 76))
        root.addView(row)

        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 28
            y = 120
        }
        makeDraggable(root, lp, saveOnEnd = false)
        panel = root
        panelStatus = status
        panelStartButton = startStop
        windowManager.addView(root, lp)
    }

    private fun addMarker(x: Int, y: Int, forcedId: Int? = null) {
        val id = forcedId ?: nextId++
        nextId = maxOf(nextId, id + 1)
        val marker = TextView(this).apply {
            text = "$id"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xF2111111.toInt())
                setStroke(6, 0xFFFFFFFF.toInt())
            }
            elevation = 16f
        }
        val lp = WindowManager.LayoutParams(86, 86, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
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
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = lp.x
                    startY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - downRawX).roundToInt()
                    lp.y = startY + (event.rawY - downRawY).roundToInt()
                    windowManager.updateViewLayout(view, lp)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (saveOnEnd) saveMarkers()
                    true
                }
                else -> true
            }
        }
    }

    private fun startLoop() {
        val service = ClickFlowAccessibilityService.liveInstance
        if (service == null) {
            updateStatus("Включи Accessibility")
            return
        }
        val p = prefs()
        val intervalMs = p.getLong(KEY_INTERVAL_MS, 500L)
        val repeatCount = p.getInt(KEY_REPEAT_COUNT, 100)
        val infinite = p.getBoolean(KEY_INFINITE, false)

        running = true
        panelStartButton?.text = "Стоп"
        updateStatus("Работает · ${markers.size} меток")

        tapJob = scope.launch {
            var cycle = 0
            while (isActive && running && (infinite || cycle < repeatCount)) {
                markers.toList().forEachIndexed { index, marker ->
                    if (!isActive || !running) return@launch
                    setAllOverlaysVisible(false)
                    delay(60)
                    val centerX = marker.params.x + marker.view.width / 2
                    val centerY = marker.params.y + marker.view.height / 2
                    val ok = service.performSingleTap(centerX, centerY, 70L)
                    delay(60)
                    setAllOverlaysVisible(true)
                    val total = if (infinite) "∞" else repeatCount.toString()
                    updateStatus(if (ok) "${cycle + 1}/$total · метка ${index + 1}" else "Ошибка")
                    delay(intervalMs)
                }
                cycle++
            }
            running = false
            panelStartButton?.text = "Старт"
            updateStatus("Готово")
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
        panelStartButton?.text = "Старт"
        updateStatus("Стоп")
        setAllOverlaysVisible(true)
    }

    private fun updateStatus(text: String) { panelStatus?.text = text }

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
