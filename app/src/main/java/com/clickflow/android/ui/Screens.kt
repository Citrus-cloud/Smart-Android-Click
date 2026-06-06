package com.clickflow.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.Screen
import com.clickflow.android.imageclick.ImageTemplateActivity
import com.clickflow.android.overlay.FloatingTapperOverlayService
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import com.clickflow.android.scenario.ScenarioActivity
import com.clickflow.android.textclick.TextClickActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PREFS_NAME = "clickflow_tapper"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"
private const val KEY_START_DELAY_MS = "start_delay_ms"
private const val KEY_MARKERS = "markers"

private data class UiTapMarker(val id: Int, val x: Float, val y: Float)

private fun encodeMarkers(markers: List<UiTapMarker>): String = markers.joinToString(";") { "${it.id},${it.x},${it.y}" }

private fun decodeMarkers(raw: String?): List<UiTapMarker> {
    if (raw.isNullOrBlank()) return listOf(UiTapMarker(1, 0.5f, 0.5f))
    val parsed = raw.split(";").mapNotNull { part ->
        val pieces = part.split(",")
        if (pieces.size != 3) return@mapNotNull null
        val id = pieces[0].toIntOrNull() ?: return@mapNotNull null
        val x = pieces[1].toFloatOrNull()?.coerceIn(0.04f, 0.96f) ?: return@mapNotNull null
        val y = pieces[2].toFloatOrNull()?.coerceIn(0.04f, 0.96f) ?: return@mapNotNull null
        UiTapMarker(id, x, y)
    }
    return parsed.ifEmpty { listOf(UiTapMarker(1, 0.5f, 0.5f)) }.take(5)
}

@Composable
fun ClickFlowApp(vm: ClickFlowViewModel) {
    val screen by vm.screen.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.ADVANCED -> AdvancedScreen(vm)
            Screen.PERMISSIONS -> PermissionsScreen(vm)
            else -> HomeScreen(vm)
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun PlainCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } }
}

@Composable
private fun HomeScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val permissionStatus by vm.permissionStatus.collectAsState()

    var intervalMs by remember { mutableStateOf(prefs.getLong(KEY_INTERVAL_MS, 500L)) }
    var repeatCount by remember { mutableStateOf(prefs.getInt(KEY_REPEAT_COUNT, 30)) }
    var infiniteMode by remember { mutableStateOf(prefs.getBoolean(KEY_INFINITE, false)) }
    var startDelayMs by remember { mutableStateOf(prefs.getLong(KEY_START_DELAY_MS, 0L)) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("—") }
    var tapJob by remember { mutableStateOf<Job?>(null) }

    val initialMarkers = remember { decodeMarkers(prefs.getString(KEY_MARKERS, null)) }
    val markers = remember { mutableStateListOf<UiTapMarker>().also { it.addAll(initialMarkers) } }
    var nextMarkerId by remember { mutableStateOf((initialMarkers.maxOfOrNull { it.id } ?: 0) + 1) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    var areaWindowPos by remember { mutableStateOf(Offset.Zero) }

    fun saveAll() {
        prefs.edit()
            .putLong(KEY_INTERVAL_MS, intervalMs)
            .putInt(KEY_REPEAT_COUNT, repeatCount)
            .putBoolean(KEY_INFINITE, infiniteMode)
            .putLong(KEY_START_DELAY_MS, startDelayMs)
            .putString(KEY_MARKERS, encodeMarkers(markers))
            .apply()
    }

    fun markerScreenX(marker: UiTapMarker): Int = (areaWindowPos.x + marker.x * areaSize.width).roundToInt()
    fun markerScreenY(marker: UiTapMarker): Int = (areaWindowPos.y + marker.y * areaSize.height).roundToInt()

    fun stop(message: String = "—") {
        tapJob?.cancel()
        tapJob = null
        running = false
        status = message
    }

    fun start() {
        saveAll()
        vm.refreshPermissions()
        val service = ClickFlowAccessibilityService.liveInstance
        if (service == null) {
            status = "Accessibility"
            vm.openAccessibilitySettings()
            return
        }
        running = true
        status = "▶"
        tapJob = scope.launch {
            if (startDelayMs > 0) {
                var left = startDelayMs / 1000
                while (left > 0 && isActive) {
                    status = "${left}s"
                    delay(1000)
                    left--
                }
            }
            var cycle = 0
            while (isActive && running && (infiniteMode || cycle < repeatCount)) {
                markers.toList().forEachIndexed { index, marker ->
                    if (!isActive || !running) return@launch
                    val ok = service.performSingleTap(markerScreenX(marker), markerScreenY(marker), 80L)
                    status = if (ok) "${cycle + 1}.${index + 1}" else "!"
                    delay(intervalMs)
                }
                cycle++
            }
            running = false
            status = "—"
        }
    }

    DisposableEffect(Unit) { onDispose { tapJob?.cancel() } }

    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ClickFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(status, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        PlainCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFCCC3B5))
                    .border(1.dp, Color(0xFFB5AA9B), RoundedCornerShape(22.dp))
                    .onGloballyPositioned { areaSize = it.size; areaWindowPos = it.positionInWindow() },
            ) {
                markers.forEachIndexed { index, marker ->
                    val x = if (areaSize.width == 0) 0 else (marker.x * areaSize.width).roundToInt()
                    val y = if (areaSize.height == 0) 0 else (marker.y * areaSize.height).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x - 34, y - 34) }
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC542))
                            .border(5.dp, Color(0xFF151515), CircleShape)
                            .pointerInput(areaSize, marker.id) {
                                detectDragGestures(onDragEnd = { saveAll() }) { change, drag ->
                                    change.consume()
                                    val w = areaSize.width.toFloat().coerceAtLeast(1f)
                                    val h = areaSize.height.toFloat().coerceAtLeast(1f)
                                    val currentIndex = markers.indexOfFirst { it.id == marker.id }
                                    if (currentIndex >= 0) {
                                        val current = markers[currentIndex]
                                        markers[currentIndex] = current.copy(
                                            x = (current.x + drag.x / w).coerceIn(0.04f, 0.96f),
                                            y = (current.y + drag.y / h).coerceIn(0.04f, 0.96f),
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("◎", color = Color(0xFF151515), fontWeight = FontWeight.Black) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (markers.size < 5) {
                        val offset = 0.10f * markers.size
                        markers.add(UiTapMarker(nextMarkerId++, (0.5f + offset).coerceAtMost(0.90f), (0.5f + offset).coerceAtMost(0.90f)))
                        saveAll()
                    }
                }, modifier = Modifier.weight(1f)) { Text("+") }
                OutlinedButton(onClick = { if (markers.size > 1) { markers.removeAt(markers.lastIndex); saveAll() } }, modifier = Modifier.weight(1f)) { Text("−") }
            }
        }

        PlainCard {
            CounterRow("ms", "$intervalMs", "−", "+", { intervalMs = (intervalMs - 100).coerceAtLeast(100); saveAll() }, { intervalMs += 100; saveAll() })
            CounterRow("×", if (infiniteMode) "∞" else "$repeatCount", "−", "+", { repeatCount = (repeatCount - 5).coerceAtLeast(1); infiniteMode = false; saveAll() }, { repeatCount += 5; infiniteMode = false; saveAll() })
            CounterRow("s", "${startDelayMs / 1000}", "−", "+", { startDelayMs = (startDelayMs - 1000).coerceAtLeast(0); saveAll() }, { startDelayMs += 1000; saveAll() })
            OutlinedButton(onClick = { infiniteMode = !infiniteMode; saveAll() }, modifier = Modifier.fillMaxWidth()) { Text(if (infiniteMode) "∞" else "×") }
        }

        Button(
            onClick = { if (running) stop() else start() },
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
        ) { Text(if (running) "■" else "▶", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { stop("!") }, modifier = Modifier.weight(1f)) { Text("!") }
            OutlinedButton(onClick = {
                saveAll()
                vm.refreshPermissions()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    context.startService(Intent(context, FloatingTapperOverlayService::class.java))
                }
            }, modifier = Modifier.weight(1f)) { Text("◎") }
            OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.weight(1f)) { Text("⋯") }
        }

        if (!permissionStatus.accessibilityEnabled) {
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Accessibility") }
        }
    }
}

@Composable
private fun CounterRow(title: String, value: String, minusLabel: String, plusLabel: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onMinus) { Text(minusLabel) }
            Button(onClick = onPlus, shape = RoundedCornerShape(14.dp)) { Text(plusLabel) }
        }
    }
}

@Composable
private fun AdvancedScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    Page {
        Text("⋯", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        PlainCard { Button(onClick = { context.startActivity(Intent(context, ImageTemplateActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Фото") } }
        PlainCard { Button(onClick = { context.startActivity(Intent(context, TextClickActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Текст") } }
        PlainCard { Button(onClick = { context.startActivity(Intent(context, ScenarioActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Сценарий") } }
        OutlinedButton(onClick = { vm.navigateTo(Screen.PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Разрешения") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun PermissionsScreen(vm: ClickFlowViewModel) {
    val status by vm.permissionStatus.collectAsState()
    Page {
        Text("Разрешения", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        PlainCard {
            Text("Accessibility: ${if (status.accessibilityEnabled) "ON" else "OFF"}")
            Text("Overlay: ${if (status.overlayGranted) "ON" else "OFF"}")
        }
        Button(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Accessibility") }
        Button(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Overlay") }
        OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) { Text("↻") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
