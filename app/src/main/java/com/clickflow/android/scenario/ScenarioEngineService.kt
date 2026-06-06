package com.clickflow.android.scenario

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
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
import com.clickflow.android.imageclick.BitmapTemplateMatcher
import com.clickflow.android.imageclick.ImageClickTemplate
import com.clickflow.android.imageclick.ImageClickTemplateStore
import com.clickflow.android.imageclick.normalized
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.service.ForegroundNotifications
import com.clickflow.android.textclick.TextClickConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Runs a saved [Scenario] step by step: taps a marker point, finds-and-taps a saved/inline
 * photo template, finds-and-taps on-screen text via OCR, or waits. Each step has its own
 * repeat count and interval; the whole scenario can loop. Per-step not-found policy decides
 * what happens when a photo/text target is not on screen (skip the step, wait and retry, or
 * stop the scenario). A floating control panel shows the scenario name, the current loop and
 * step, and a Stop button. Reuses the same Accessibility screenshot + matcher + OCR pipeline
 * as the standalone photo/text clickers, so there is no screen-recording prompt.
 */
class ScenarioEngineService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    @Volatile private var running = false
    private var panelView: View? = null
    private var progressText: TextView? = null
    private var wm: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_START -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    Toast.makeText(this, "Нужен Android 11+", Toast.LENGTH_LONG).show()
                    stopSelf(); return START_NOT_STICKY
                }
                ForegroundNotifications.start(this, ForegroundNotifications.ID_SCENARIO, "ClickFlow: сценарий")
                val scenarioId = intent.getStringExtra(EXTRA_SCENARIO_ID)
                scope.launch {
                    val service = ClickFlowAccessibilityService.awaitInstance()
                    if (service == null) {
                        val msg = if (ClickFlowAccessibilityService.isEnabledInSettings(this@ScenarioEngineService))
                            "Accessibility ещё запускается, попробуй ещё раз"
                        else "Включи Accessibility в настройках"
                        Toast.makeText(this@ScenarioEngineService, msg, Toast.LENGTH_LONG).show()
                        stopSelf(); return@launch
                    }
                    val scenario = if (!scenarioId.isNullOrBlank())
                        ScenarioLibraryStore.get(this@ScenarioEngineService, scenarioId)
                    else ScenarioLibraryStore.getActive(this@ScenarioEngineService)
                    if (scenario == null || scenario.steps.isEmpty()) {
                        Toast.makeText(this@ScenarioEngineService, "Сценарий пуст", Toast.LENGTH_LONG).show()
                        stopSelf(); return@launch
                    }
                    runScenario(scenario, service)
                }
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun runScenario(scenario: Scenario, service: ClickFlowAccessibilityService) {
        running = true
        showControlPanel()
        val recognizer: TextRecognizer? =
            if (scenario.steps.any { it.type == StepType.TEXT })
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            else null
        val loopLabel = if (scenario.loopInfinite) "∞" else "${scenario.loopCount.coerceAtLeast(1)}"
        job = scope.launch {
            delay(700L) // let the launching UI disappear before the first screenshot
            var loop = 0
            outer@ while (running && (scenario.loopInfinite || loop < scenario.loopCount.coerceAtLeast(1))) {
                for ((index, step) in scenario.steps.withIndex()) {
                    if (!running) break@outer
                    updateProgress("▶ ${scenario.name}\nЦикл ${loop + 1}/$loopLabel · шаг ${index + 1}/${scenario.steps.size}\n${step.summary()}")
                    val keepGoing = executeStep(step, service, recognizer)
                    if (!keepGoing) { running = false; break@outer }
                }
                loop++
            }
            recognizer?.close()
            running = false
            stopSelf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun executeStep(
        step: ScenarioStep,
        service: ClickFlowAccessibilityService,
        recognizer: TextRecognizer?,
    ): Boolean {
        when (step.type) {
            StepType.WAIT -> {
                delay(step.waitMs.coerceIn(0L, 600000L))
                return true
            }
            StepType.MARKER -> {
                var taps = 0
                val interval = step.intervalMs.coerceIn(50L, 600000L)
                val target = step.repeat.coerceAtLeast(1)
                while (running && taps < target) {
                    service.performSingleTap(step.x, step.y, 70L)
                    taps++
                    delay(interval)
                }
                return true
            }
            StepType.PHOTO -> {
                val resolved = resolvePhotoTemplate(step) ?: return true
                val (template, bitmap) = resolved
                try {
                    return runFindTapStep(step, captureInterval(step.intervalMs)) {
                        photoFindOnce(template, bitmap, service)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            StepType.TEXT -> {
                if (step.text.isBlank() || recognizer == null) return true
                val config = TextClickConfig(
                    query = step.text,
                    contains = step.textContains,
                    ignoreCase = step.textIgnoreCase,
                )
                return runFindTapStep(step, captureInterval(step.intervalMs)) {
                    textFindOnce(config, recognizer, service)
                }
            }
        }
    }

    /**
     * Repeats a find-and-tap action [ScenarioStep.repeat] times. When the target is missing it
     * applies the step's not-found policy: SKIP moves to the next step, WAIT_RETRY waits and
     * retries up to [ScenarioStep.notFoundRetries] times then skips, STOP ends the scenario
     * (returns false). Returns true to keep running the scenario.
     */
    private suspend fun runFindTapStep(
        step: ScenarioStep,
        intervalMs: Long,
        findOnce: suspend () -> Boolean,
    ): Boolean {
        var taps = 0
        val target = step.repeat.coerceAtLeast(1)
        while (running && taps < target) {
            var found = findOnce()
            if (!found && step.notFound == NotFoundPolicy.WAIT_RETRY) {
                var retries = 0
                val maxRetries = step.notFoundRetries.coerceAtLeast(0)
                val waitMs = step.notFoundWaitMs.coerceIn(100L, 600000L)
                while (running && !found && retries < maxRetries) {
                    delay(waitMs)
                    found = findOnce()
                    retries++
                }
            }
            if (!found) {
                return when (step.notFound) {
                    NotFoundPolicy.STOP -> false // stop the whole scenario
                    else -> true                 // SKIP or exhausted WAIT_RETRY -> go to next step
                }
            }
            taps++
            delay(intervalMs)
        }
        return true
    }

    private fun captureInterval(intervalMs: Long): Long = maxOf(intervalMs, 1100L)

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun photoFindOnce(
        template: ImageClickTemplate,
        templateBitmap: Bitmap,
        service: ClickFlowAccessibilityService,
    ): Boolean {
        val screen = capture(service) ?: return false
        try {
            // Heavy pixel matching runs off the main thread so the UI never blocks.
            val match = withContext(Dispatchers.Default) {
                val regionLeftPx = (screen.width * template.regionLeft).toInt()
                val regionTopPx = (screen.height * template.regionTop).toInt()
                val regionRightPx = (screen.width * template.regionRight).toInt()
                val regionBottomPx = (screen.height * template.regionBottom).toInt()
                BitmapTemplateMatcher.findBest(
                    screen = screen,
                    template = templateBitmap,
                    threshold = template.threshold,
                    regionLeftPx = regionLeftPx,
                    regionTopPx = regionTopPx,
                    regionRightPx = regionRightPx,
                    regionBottomPx = regionBottomPx,
                    scaleMin = template.scaleMin,
                    scaleMax = template.scaleMax,
                )
            } ?: return false
            val tapX = match.x + (match.width * template.tapX).toInt()
            val tapY = match.y + (match.height * template.tapY).toInt()
            service.performSingleTap(tapX, tapY, 70L)
            return true
        } finally {
            screen.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun textFindOnce(
        config: TextClickConfig,
        recognizer: TextRecognizer,
        service: ClickFlowAccessibilityService,
    ): Boolean {
        val screen = capture(service) ?: return false
        try {
            val result = runCatching { recognizer.process(InputImage.fromBitmap(screen, 0)).await() }.getOrNull()
            val box = result?.let { findTextBox(it, config) } ?: return false
            service.performSingleTap(box.centerX(), box.centerY(), 70L)
            return true
        } finally {
            screen.recycle()
        }
    }

    private fun findTextBox(text: Text, config: TextClickConfig): Rect? {
        val query = if (config.ignoreCase) config.query.lowercase() else config.query
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val value = if (config.ignoreCase) line.text.lowercase() else line.text
                val matched = if (config.contains) value.contains(query) else value == query
                if (matched) return line.boundingBox
                for (element in line.elements) {
                    val ev = if (config.ignoreCase) element.text.lowercase() else element.text
                    val em = if (config.contains) ev.contains(query) else ev == query
                    if (em) return element.boundingBox
                }
            }
        }
        return null
    }

    private fun resolvePhotoTemplate(step: ScenarioStep): Pair<ImageClickTemplate, Bitmap>? {
        val fromLibrary = if (step.photoTemplateId.isNotBlank())
            ImageClickTemplateStore.loadTemplates(this).firstOrNull { it.id == step.photoTemplateId }?.normalized()
        else null
        val template = fromLibrary ?: if (step.photoPath.isNotBlank()) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(step.photoPath, opts)
            ImageClickTemplate(
                id = "inline-" + step.id,
                name = step.label,
                filePath = step.photoPath,
                width = opts.outWidth.coerceAtLeast(1),
                height = opts.outHeight.coerceAtLeast(1),
            ).normalized()
        } else null
        if (template == null) return null
        val bitmap = BitmapFactory.decodeFile(template.filePath) ?: return null
        return template to bitmap
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(service: ClickFlowAccessibilityService): Bitmap? =
        suspendCancellableCoroutine { cont ->
            panelView?.visibility = View.INVISIBLE
            service.captureScreenBitmap { bitmap ->
                panelView?.let { v -> v.post { v.visibility = View.VISIBLE } }
                if (cont.isActive) cont.resume(bitmap)
            }
        }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun updateProgress(text: String) {
        progressText?.let { tv -> tv.post { tv.text = text } }
    }

    private fun showControlPanel() {
        try {
            val manager = getSystemService(WINDOW_SERVICE) as WindowManager
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 22, 30, 22)
                background = GradientDrawable().apply { cornerRadius = 34f; setColor(0xF21C1C1C.toInt()) }
            }
            val progress = TextView(this).apply {
                text = "▶ Сценарий запускается…"
                setTextColor(Color.WHITE)
                textSize = 13f
            }
            val stop = TextView(this).apply {
                text = "■ Стоп"
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(30, 14, 30, 14)
                background = GradientDrawable().apply { cornerRadius = 26f; setColor(0xF2D32F2F.toInt()) }
                setOnClickListener { stopSelf() }
            }
            val stopLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 18 }
            root.addView(progress)
            root.addView(stop, stopLp)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 120 }
            manager.addView(root, lp)
            panelView = root
            progressText = progress
            wm = manager
        } catch (_: Throwable) {}
    }

    private fun removeControlPanel() {
        panelView?.let { v -> runCatching { wm?.removeView(v) } }
        panelView = null
        progressText = null
    }

    override fun onDestroy() {
        running = false
        job?.cancel()
        removeControlPanel()
        ForegroundNotifications.stop(this)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.clickflow.android.scenario.engine.START"
        const val ACTION_STOP = "com.clickflow.android.scenario.engine.STOP"
        const val EXTRA_SCENARIO_ID = "scenario_id"
    }
}
