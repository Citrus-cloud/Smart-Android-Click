package com.clickflow.android.imageclick

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.service.ForegroundNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Looks at the screen via the Accessibility screenshot API and taps where the saved
 * picture(s) are found. Two run modes:
 *
 *  - SEQUENTIAL (default): photos are tapped strictly in order — it waits for photo 1 to
 *    appear and taps it once, then waits for photo 2, then photo 3. By default it STOPS after
 *    the last photo. With looping on it instead restarts from photo 1 (infinitely or a set
 *    number of passes), and a missing photo never auto-stops it — it just keeps waiting, so the
 *    run only ends when the user presses Stop. This matches cross-screen chains like
 *    "open game icon → open map list → pick a map" and farming the same chain on repeat.
 *  - SIMULTANEOUS ("multitap"): every active photo is searched in the SAME screenshot and
 *    tapped within the same scan cycle, each with its own repeat counter — good for hitting
 *    several buttons on one screen.
 *
 * No screen-recording prompt. Android limits screenshots to about one per second, so the scan
 * interval is kept above that to stay reliable.
 *
 * Every fresh start cancels any previous run and clears the debug log + control chip, so a
 * restart always begins cleanly at photo 1 and never carries over stale state.
 *
 * The control panel is NOT hidden during each screenshot: toggling its visibility every cycle
 * made it (and the system's floating-window indicator) blink on and off. The panel sits in the
 * top-right corner; keep tap targets out of that corner so it never covers them in the capture.
 */
class ImageClickService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var running = false
    private var panelView: View? = null
    private var debugText: TextView? = null
    private var wm: WindowManager? = null
    private val debugLines = mutableListOf<String>()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                val ids = parseTemplateIds(intent)
                if (ids.isEmpty()) { stopSelf(); return START_NOT_STICKY }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(this, "\u041d\u0443\u0436\u0435\u043d Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                val sequential = intent.getBooleanExtra(EXTRA_SEQUENTIAL, true)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, false)
                val loopCount = intent.getIntExtra(EXTRA_LOOP_COUNT, 0).coerceAtLeast(0)
                // Fresh start: cancel any previous run and wipe leftover state so the log and the
                // step index never carry over from a finished/old run.
                job?.cancel()
                running = false
                debugLines.clear()
                removeStopChip()
                ForegroundNotifications.start(this, ForegroundNotifications.ID_IMAGE, "ClickFlow: \u0430\u0432\u0442\u043e\u0442\u0430\u043f \u043f\u043e \u0444\u043e\u0442\u043e")
                job = scope.launch {
                    val service = ClickFlowAccessibilityService.awaitInstance()
                    if (service == null) {
                        val msg = if (ClickFlowAccessibilityService.isEnabledInSettings(this@ImageClickService))
                            "ClickFlow \u0432\u043a\u043b\u044e\u0447\u0451\u043d, \u043d\u043e \u0441\u043b\u0443\u0436\u0431\u0430 \u043d\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u0430. \u0412\u044b\u043a\u043b\u044e\u0447\u0438 \u0438 \u0441\u043d\u043e\u0432\u0430 \u0432\u043a\u043b\u044e\u0447\u0438 ClickFlow \u0432 \u0441\u043f\u0435\u0446. \u0432\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u044f\u0445."
                        else "\u0412\u043a\u043b\u044e\u0447\u0438 Accessibility \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430\u0445"
                        Toast.makeText(this@ImageClickService, msg, Toast.LENGTH_LONG).show()
                        stopSelf(); return@launch
                    }
                    begin(ids, sequential, loop, loopCount)
                }
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun parseTemplateIds(intent: Intent): List<String> {
        intent.getStringExtra(EXTRA_TEMPLATE_IDS)?.let { joined ->
            return joined.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        }
        val single = intent.getStringExtra(EXTRA_TEMPLATE_ID)
        return if (single.isNullOrBlank()) emptyList() else listOf(single)
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showStopChip() {
        try {
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }
            val chip = TextView(this).apply {
                text = "\u25a0 \u0421\u0442\u043e\u043f \u0444\u043e\u0442\u043e"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(30, 18, 30, 18)
                background = GradientDrawable().apply { cornerRadius = 32f; setColor(0xF2D32F2F.toInt()) }
                setOnClickListener { stopSelf() }
            }
            val debug = TextView(this).apply {
                text = "\u0436\u0443\u0440\u043d\u0430\u043b \u043a\u043b\u0438\u043a\u043e\u0432\u2026"
                setTextColor(0xFFEAEAEA.toInt())
                textSize = 10f
                maxWidth = 560
                setPadding(26, 16, 26, 16)
                background = GradientDrawable().apply { cornerRadius = 22f; setColor(0xE61C1C1C.toInt()) }
            }
            val debugLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 14; gravity = Gravity.END }
            root.addView(chip)
            root.addView(debug, debugLp)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 120 }
            manager.addView(root, lp)
            panelView = root
            debugText = debug
            wm = manager
        } catch (_: Throwable) {}
    }

    private fun removeStopChip() {
        panelView?.let { v -> runCatching { wm?.removeView(v) } }
        panelView = null
        debugText = null
    }

    private fun pushDebug(lines: List<String>) {
        if (lines.isEmpty()) return
        debugLines.add(timeFmt.format(Date()))
        lines.forEach { debugLines.add("  $it") }
        while (debugLines.size > 10) debugLines.removeAt(0)
        val text = debugLines.joinToString("\n")
        debugText?.let { tv -> tv.post { tv.text = text } }
    }

    private fun pct(confidence: Float): String = "${(confidence * 100).roundToInt()}%"

    private fun matchTarget(bitmap: Bitmap, t: ImageTarget): BitmapTemplateMatcher.Match? {
        val regionLeftPx = (bitmap.width * t.meta.regionLeft).toInt()
        val regionTopPx = (bitmap.height * t.meta.regionTop).toInt()
        val regionRightPx = (bitmap.width * t.meta.regionRight).toInt()
        val regionBottomPx = (bitmap.height * t.meta.regionBottom).toInt()
        return BitmapTemplateMatcher.findBestMatch(
            screen = bitmap,
            template = t.bitmap,
            regionLeftPx = regionLeftPx,
            regionTopPx = regionTopPx,
            regionRightPx = regionRightPx,
            regionBottomPx = regionBottomPx,
            scaleMin = t.meta.scaleMin,
            scaleMax = t.meta.scaleMax,
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun begin(templateIds: List<String>, sequential: Boolean, loop: Boolean, loopCount: Int) {
        val all = ImageClickTemplateStore.loadTemplates(this)
        // Keep the order the user arranged the photos in — that defines the sequence.
        val targets = templateIds
            .mapNotNull { id -> all.firstOrNull { it.id == id }?.normalized() }
            .mapNotNull { meta ->
                val bmp = BitmapFactory.decodeFile(meta.filePath) ?: return@mapNotNull null
                ImageTarget(meta, bmp)
            }
            .toMutableList()
        if (targets.isEmpty()) { stopSelf(); return }
        if (ClickFlowAccessibilityService.liveInstance == null) {
            targets.forEach { runCatching { it.bitmap.recycle() } }
            stopSelf(); return
        }
        val service = ClickFlowAccessibilityService.liveInstance!!

        running = true
        showStopChip()
        // Scan as fast as the fastest target wants, but never faster than the screenshot limit.
        val scanInterval = maxOf(targets.minOf { it.meta.intervalMs }, 1100L)
        try {
            delay(800) // let the setup screen disappear before the first screenshot
            if (sequential) sequentialLoop(targets, service, scanInterval, loop, loopCount)
            else simultaneousLoop(targets, service, scanInterval)
        } finally {
            targets.forEach { runCatching { it.bitmap.recycle() } }
        }
        stopSelf()
    }

    /**
     * Tap photos strictly in order. By default runs a single pass and stops. With [loopEnabled]
     * it repeats the whole chain — [loopCount] passes, or forever when loopCount <= 0 — and a
     * missing photo no longer ends the run; it simply keeps waiting until the photo shows up or
     * the user presses Stop.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun sequentialLoop(
        targets: List<ImageTarget>,
        service: ClickFlowAccessibilityService,
        scanInterval: Long,
        loopEnabled: Boolean,
        loopCount: Int,
    ) {
        val loopLabel = when {
            !loopEnabled -> "1"
            loopCount <= 0 -> "\u221e"
            else -> "$loopCount"
        }
        pushDebug(listOf("\u25b6 \u043f\u043e \u043e\u0447\u0435\u0440\u0435\u0434\u0438: ${targets.size} \u0448\u0430\u0433(\u043e\u0432) \u00d7$loopLabel"))
        var pass = 0
        while (running) {
            val completed = runSequentialPass(targets, service, scanInterval, loopEnabled)
            if (!completed) break
            pass++
            if (!loopEnabled) {
                pushDebug(listOf("\u2713 \u0433\u043e\u0442\u043e\u0432\u043e \u2014 \u0432\u0441\u0435 \u0448\u0430\u0433\u0438 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u044b"))
                break
            }
            if (loopCount > 0 && pass >= loopCount) {
                pushDebug(listOf("\u2713 \u0433\u043e\u0442\u043e\u0432\u043e \u2014 $pass \u0446\u0438\u043a\u043b(\u043e\u0432)"))
                break
            }
            pushDebug(listOf("\u21bb \u0446\u0438\u043a\u043b ${pass + 1}\u2026"))
            delay(SETTLE_MS)
        }
        running = false
    }

    /**
     * One full ordered pass over [targets]. Returns true if every photo was tapped in order,
     * false if the pass was aborted (capture repeatedly failed, the user stopped, or — only when
     * not looping — a photo never showed up within [MAX_MISSES] scans).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun runSequentialPass(
        targets: List<ImageTarget>,
        service: ClickFlowAccessibilityService,
        scanInterval: Long,
        loopEnabled: Boolean,
    ): Boolean {
        var idx = 0
        var misses = 0
        var captureFails = 0
        while (running && idx < targets.size) {
            val bitmap = capture(service)
            if (bitmap == null) {
                captureFails++
                pushDebug(listOf("\u2717 \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442 \u043d\u0435 \u043f\u043e\u043b\u0443\u0447\u0435\u043d ($captureFails/5)"))
                if (captureFails >= 5) {
                    Toast.makeText(this@ImageClickService, "\u041d\u0435 \u0443\u0434\u0430\u0451\u0442\u0441\u044f \u0441\u0434\u0435\u043b\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442 \u044d\u043a\u0440\u0430\u043d\u0430. \u041f\u0440\u043e\u0432\u0435\u0440\u044c Accessibility \u0438 \u043f\u043e\u043f\u0440\u043e\u0431\u0443\u0439 \u0441\u043d\u043e\u0432\u0430.", Toast.LENGTH_LONG).show()
                    running = false
                    return false
                }
                delay(scanInterval)
                continue
            }
            captureFails = 0
            val t = targets[idx]
            val match = withContext(Dispatchers.Default) { matchTarget(bitmap, t) }
            val label = "#${idx + 1}"
            if (match != null && match.confidence >= t.meta.threshold) {
                val tapX = match.x + (match.width * t.meta.tapX).toInt()
                val tapY = match.y + (match.height * t.meta.tapY).toInt()
                pushDebug(listOf("$label \u2713 ${pct(match.confidence)} \u2192 $tapX,$tapY"))
                bitmap.recycle()
                service.performSingleTapAwait(tapX, tapY, 70L)
                idx++
                misses = 0
                delay(SETTLE_MS) // let the screen change before looking for the next photo
            } else {
                misses++
                val c = if (match == null) "\u2014" else pct(match.confidence)
                if (loopEnabled) {
                    pushDebug(listOf("$label \u2717 $c (\u043f\u043e\u0440\u043e\u0433 ${pct(t.meta.threshold)}) \u0436\u0434\u0443\u2026 $misses"))
                } else {
                    pushDebug(listOf("$label \u2717 $c (\u043f\u043e\u0440\u043e\u0433 ${pct(t.meta.threshold)}) \u0436\u0434\u0443\u2026 $misses/$MAX_MISSES"))
                }
                bitmap.recycle()
                if (!loopEnabled && misses >= MAX_MISSES) {
                    Toast.makeText(this@ImageClickService, "\u041d\u0435 \u043d\u0430\u0448\u0451\u043b \u0444\u043e\u0442\u043e \u2116${idx + 1}. \u041e\u0441\u0442\u0430\u043d\u0430\u0432\u043b\u0438\u0432\u0430\u044e.", Toast.LENGTH_LONG).show()
                    running = false
                    return false
                }
                delay(scanInterval)
            }
        }
        return running && idx >= targets.size
    }

    /** The original "multitap": search every active photo in one screenshot and tap each that clears its threshold. */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun simultaneousLoop(targets: MutableList<ImageTarget>, service: ClickFlowAccessibilityService, scanInterval: Long) {
        var captureFails = 0
        while (running) {
            val bitmap = capture(service)
            if (bitmap == null) {
                captureFails++
                pushDebug(listOf("\u2717 \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442 \u043d\u0435 \u043f\u043e\u043b\u0443\u0447\u0435\u043d ($captureFails/5)"))
                if (captureFails >= 5) {
                    Toast.makeText(this@ImageClickService, "\u041d\u0435 \u0443\u0434\u0430\u0451\u0442\u0441\u044f \u0441\u0434\u0435\u043b\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442 \u044d\u043a\u0440\u0430\u043d\u0430. \u041f\u0440\u043e\u0432\u0435\u0440\u044c Accessibility \u0438 \u043f\u043e\u043f\u0440\u043e\u0431\u0443\u0439 \u0441\u043d\u043e\u0432\u0430.", Toast.LENGTH_LONG).show()
                    running = false
                    break
                }
                delay(scanInterval)
                continue
            }
            captureFails = 0
            val scan = withContext(Dispatchers.Default) { targets.map { t -> t to matchTarget(bitmap, t) } }
            val hits = mutableListOf<Triple<ImageTarget, Int, Int>>()
            val lines = mutableListOf<String>()
            scan.forEachIndexed { i, (t, match) ->
                val label = "#${i + 1}"
                if (match != null && match.confidence >= t.meta.threshold) {
                    val tapX = match.x + (match.width * t.meta.tapX).toInt()
                    val tapY = match.y + (match.height * t.meta.tapY).toInt()
                    hits.add(Triple(t, tapX, tapY))
                    lines.add("$label \u2713 ${pct(match.confidence)} \u2192 $tapX,$tapY")
                } else {
                    val c = if (match == null) "\u2014" else pct(match.confidence)
                    lines.add("$label \u2717 $c (\u043f\u043e\u0440\u043e\u0433 ${pct(t.meta.threshold)})")
                }
            }
            pushDebug(lines)
            for ((t, tapX, tapY) in hits) {
                service.performSingleTapAwait(tapX, tapY, 70L)
                t.taps++
                if (hits.size > 1) delay(120)
            }
            val finished = targets.filter { !it.infinite && it.taps >= it.maxTaps }
            finished.forEach { it.bitmap.recycle() }
            targets.removeAll(finished.toSet())
            bitmap.recycle()
            if (targets.isEmpty()) { running = false; break }
            delay(scanInterval)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(service: ClickFlowAccessibilityService): Bitmap? = suspendCancellableCoroutine { cont ->
        // The panel is intentionally left visible: hiding/showing it every screenshot made it
        // blink. It lives in the top-right corner — keep targets away from there.
        service.captureScreenBitmap { bitmap ->
            if (cont.isActive) cont.resume(bitmap)
        }
    }

    override fun onDestroy() {
        running = false
        job?.cancel()
        removeStopChip()
        ForegroundNotifications.stop(this)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.imageclick.START"
        const val ACTION_STOP = "com.clickflow.android.imageclick.STOP"
        const val EXTRA_TEMPLATE_ID = "template_id"
        const val EXTRA_TEMPLATE_IDS = "template_ids"
        const val EXTRA_SEQUENTIAL = "sequential"
        const val EXTRA_LOOP = "loop"
        const val EXTRA_LOOP_COUNT = "loop_count"
        private const val SETTLE_MS = 700L
        private const val MAX_MISSES = 60
    }
}

/** Runtime state for one template inside a multi-target run. */
private class ImageTarget(
    val meta: ImageClickTemplate,
    val bitmap: Bitmap,
) {
    val infinite: Boolean = meta.infinite || meta.continuous
    val maxTaps: Int = meta.repeatCount.coerceAtLeast(1)
    var taps: Int = 0
}
