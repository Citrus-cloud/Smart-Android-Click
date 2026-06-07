package com.clickflow.android.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.clickflow.android.core.Premium
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.service.ForegroundNotifications
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
private const val MARKER_SIZE = 160

/**
 * The floating tap marker is the main feature: a simple round target placed over any
 * app. Drag it by touching it directly. All controls live in the attached panel
 * (start/stop, add/remove targets, interval, repeat count, infinite); drag the panel
 * by its top grab bar.
 *
 * During tapping we do NOT hide the marker (that caused flicker). Each marker window
 * is made touch-transparent so the dispatched gesture passes through to the app
 * below while the marker stays visible. FLAG_NOT_TOUCHABLE is only honored at addView
 * time on many Android builds, so we REMOVE and RE-ADD the window to actually apply
 * it. Keeping the marker a single View (not a container) makes that re-add reliable.
 */
class FloatingTapperOverlayService : Service() {

    private data class FloatingMarker(val id: Int, val view: View, val params: WindowManager.LayoutParams)

    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var startButton: Button? = null
    private var intervalLabel: TextView? = null
    private var countLabel: TextView? = null
    private val markers = mutableListOf<FloatingMarker>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tapJob: Job? = null
    private var running = false
    private var nextId = 1
    private var intervalMs = 500L
    private var repeatCount = 30
    private var infinite = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ForegroundNotifications.start(this, ForegroundNotifications.ID_OVERLAY, "ClickFlow: \u0430\u0432\u0442\u043e\u0442\u0430\u043f \u043f\u043e \u043c\u0435\u0442\u043a\u0435")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val p = prefs()
        intervalMs = p.getLong(KEY_INTERVAL_MS, 500L)
        repeatCount = p.getInt(KEY_REPEAT_COUNT, 30)
        infinite = p.getBoolean(KEY_INFINITE, false)
        showPanel()
        loadMarkers()
        if (markers.isEmpty()) addMarker(280, 520)
    }

    override fun onDestroy() {
        stopLoop()
        saveMarkers()
        markers.toList().forEach { runCatching { windowManager.removeView(it.view) } }
        panel?.let { runCatching { windowManager.removeView(it) } }
        markers.clear()
        panel = null
        ForegroundNotifications.stop(this)
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun panelBg(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 28f
        setColor(0xF21C1C1C.toInt())
    }

    private fun grabber(): View = View(this).apply {
        background = GradientDrawable().apply { cornerRadius = 17f; setColor(0xFF7A7A7A.toInt()) }
    }

    private fun pill(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setPadding(8, 4, 8, 4)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 22f
            setColor(0xFF2E2E2E.toInt())
        }
        setOnClickListener { onClick() }
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 12)
            background = panelBg()
        }

        val grab = grabber()
        val grabLp = LinearLayout.LayoutParams(170, 32).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = 4; bottomMargin = 12 }

        val start = pill("\u25b6 \u0421\u0442\u0430\u0440\u0442") { if (running) stopLoop() else startLoop() }

        val markerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val add = pill("+ \u0422\u043e\u0447\u043a\u0430") {
            if (markers.size < Premium.MARKER_LIMIT) { addMarker(280 + markers.size * 70, 520 + markers.size * 70); saveMarkers() }
        }
        val remove = pill("\u2212 \u0422\u043e\u0447\u043a\u0430") { removeLastMarker(); saveMarkers() }
        markerRow.addView(add, lp(0, 70, 1f))
        markerRow.addView(space())
        markerRow.addView(remove, lp(0, 70, 1f))

        val intervalRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        intervalRow.addView(pill("\u2212") { setInterval(intervalMs - 100) }, lp(62, 58, 0f))
        val iLabel = TextView(this).apply { text = "$intervalMs \u043c\u0441"; textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        intervalRow.addView(iLabel, lp(0, 58, 1f))
        intervalRow.addView(pill("+") { setInterval(intervalMs + 100) }, lp(62, 58, 0f))

        val countRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        countRow.addView(pill("\u2212") { setRepeat(repeatCount - 5) }, lp(62, 58, 0f))
        val cLabel = TextView(this).apply { text = if (infinite) "\u221e" else "$repeatCount"; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        countRow.addView(cLabel, lp(0, 58, 1f))
        countRow.addView(pill("+") { setRepeat(repeatCount + 5) }, lp(62, 58, 0f))
        countRow.addView(space())
        countRow.addView(pill("\u221e") { toggleInfinite() }, lp(62, 58, 0f))

        val close = pill("\u00d7 \u0417\u0430\u043a\u0440\u044b\u0442\u044c") { stopSelf() }

        root.addView(grab, grabLp)
        root.addView(start, lp(252, 72, 0f))
        root.addView(spaceV())
        root.addView(markerRow, lp(252, 0, 0f))
        root.addView(spaceV())
        root.addView(labelText("\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b"))
        root.addView(intervalRow, lp(252, 0, 0f))
        root.addView(spaceV())
        root.addView(labelText("\u041f\u043e\u0432\u0442\u043e\u0440\u044b"))
        root.addView(countRow, lp(252, 0, 0f))
        root.addView(spaceV())
        root.addView(close, lp(252, 56, 0f))

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 80
        }
        makeDraggable(grab, lp, root, saveOnEnd = false)
        panel = root
        startButton = start
        intervalLabel = iLabel
        countLabel = cLabel
        windowManager.addView(root, lp)
    }

    private fun labelText(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(0xFF9E9E9E.toInt())
        setPadding(6, 0, 0, 0)
    }

    private fun lp(w: Int, h: Int, weight: Float) = LinearLayout.LayoutParams(
        if (w == 0) LinearLayout.LayoutParams.MATCH_PARENT else w,
        if (h == 0) LinearLayout.LayoutParams.WRAP_CONTENT else h,
        weight,
    )

    private fun space(): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(10, 1) }
    private fun spaceV(): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 9) }

    private fun setInterval(value: Long) {
        intervalMs = value.coerceIn(100L, 5000L)
        intervalLabel?.text = "$intervalMs \u043c\u0441"
        prefs().edit().putLong(KEY_INTERVAL_MS, intervalMs).apply()
    }

    private fun setRepeat(value: Int) {
        repeatCount = value.coerceIn(1, 100000)
        infinite = false
        countLabel?.text = "$repeatCount"
        prefs().edit().putInt(KEY_REPEAT_COUNT, repeatCount).putBoolean(KEY_INFINITE, false).apply()
    }

    private fun toggleInfinite() {
        infinite = !infinite
        countLabel?.text = if (infinite) "\u221e" else "$repeatCount"
        prefs().edit().putBoolean(KEY_INFINITE, infinite).apply()
    }

    /** Simple round target: crisp blue ring + translucent fill (see-through) + a small
     *  white center dot marking the exact tap point. No text glyph, so nothing renders
     *  as an unknown box. */
    private fun markerDrawable(): LayerDrawable {
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x202D7DF6)
            setStroke(7, 0xE62D7DF6.toInt())
        }
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xCCFFFFFF.toInt())
        }
        val inset = MARKER_SIZE / 2 - 8
        return LayerDrawable(arrayOf(ring, dot)).apply {
            setLayerInset(1, inset, inset, inset, inset)
        }
    }

    private fun addMarker(x: Int, y: Int, forcedId: Int? = null) {
        val id = forcedId ?: nextId++
        nextId = maxOf(nextId, id + 1)
        val view = View(this).apply { background = markerDrawable() }
        val dm = resources.displayMetrics
        val cx = x.coerceIn(0, (dm.widthPixels - MARKER_SIZE).coerceAtLeast(0))
        val cy = y.coerceIn(0, (dm.heightPixels - MARKER_SIZE).coerceAtLeast(0))
        val lp = WindowManager.LayoutParams(MARKER_SIZE, MARKER_SIZE, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = cx
            this.y = cy
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
                    val dm = resources.displayMetrics
                    val w = if (moved.width > 0) moved.width else MARKER_SIZE
                    val h = if (moved.height > 0) moved.height else MARKER_SIZE
                    lp.x = (startX + (event.rawX - downRawX).roundToInt()).coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
                    lp.y = (startY + (event.rawY - downRawY).roundToInt()).coerceIn(0, (dm.heightPixels - h).coerceAtLeast(0))
                    runCatching { windowManager.updateViewLayout(moved, lp) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { if (saveOnEnd) saveMarkers(); true }
                else -> true
            }
        }
    }

    /**
     * Apply/clear touch-transparency by re-adding each marker window, because the
     * FLAG_NOT_TOUCHABLE flag is only read when the window is added on most devices.
     */
    private fun setMarkersTouchable(touchable: Boolean) {
        markers.forEach { m ->
            m.params.flags = if (touchable) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            runCatching { windowManager.removeViewImmediate(m.view) }
            runCatching { windowManager.addView(m.view, m.params) }
        }
    }

    private fun startLoop() {
        if (markers.isEmpty()) return
        running = true
        startButton?.text = "\u25a0 \u0421\u0442\u043e\u043f"
        setMarkersTouchable(false)
        tapJob = scope.launch {
            val service = ClickFlowAccessibilityService.awaitInstance()
            if (service == null) {
                val enabled = ClickFlowAccessibilityService.isEnabledInSettings(this@FloatingTapperOverlayService)
                Toast.makeText(
                    this@FloatingTapperOverlayService,
                    if (enabled) "Accessibility \u0435\u0449\u0451 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f, \u043f\u043e\u043f\u0440\u043e\u0431\u0443\u0439 \u0435\u0449\u0451 \u0440\u0430\u0437" else "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0432\u043a\u043b\u044e\u0447\u0438 \u0441\u043f\u0435\u0446. \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438 (Accessibility)",
                    Toast.LENGTH_LONG,
                ).show()
                stopLoop()
                return@launch
            }
            delay(250) // let the not-touchable flag take effect so taps pass through
            var cycle = 0
            while (isActive && running && (infinite || cycle < repeatCount)) {
                for (marker in markers.toList()) {
                    if (!isActive || !running) break
                    // Tap the marker's REAL on-screen center. params.x/y are window coords that
                    // exclude the status-bar inset on TOP|START overlays, but dispatchGesture wants
                    // absolute screen coords, so the old (params + size/2) math tapped above the dot.
                    val loc = IntArray(2)
                    marker.view.getLocationOnScreen(loc)
                    val vw = if (marker.view.width > 0) marker.view.width else MARKER_SIZE
                    val vh = if (marker.view.height > 0) marker.view.height else MARKER_SIZE
                    val centerX = loc[0] + vw / 2
                    val centerY = loc[1] + vh / 2
                    service.performSingleTap(centerX, centerY, 60L)
                    delay(intervalMs)
                }
                cycle++
            }
            stopLoop()
        }
    }

    private fun stopLoop() {
        running = false
        tapJob?.cancel()
        tapJob = null
        startButton?.text = "\u25b6 \u0421\u0442\u0430\u0440\u0442"
        setMarkersTouchable(true)
    }

    private fun saveMarkers() {
        val raw = markers.joinToString(";") { "${it.id},${it.params.x},${it.params.y}" }
        prefs().edit().putString(KEY_OVERLAY_MARKERS, raw).apply()
    }

    private fun loadMarkers() {
        val raw = prefs().getString(KEY_OVERLAY_MARKERS, null).orEmpty()
        raw.split(";").filter { it.isNotBlank() }.take(Premium.MARKER_LIMIT).forEach { part ->
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
