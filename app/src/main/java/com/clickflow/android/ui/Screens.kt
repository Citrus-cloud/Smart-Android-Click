package com.clickflow.android.ui

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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
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
import com.clickflow.android.overlay.FloatingTapperOverlayService
import com.clickflow.android.permissions.ClickFlowAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class UiTapMarker(val id: Int, val x: Float, val y: Float)

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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun HomeScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionStatus by vm.permissionStatus.collectAsState()

    var intervalMs by remember { mutableStateOf(500L) }
    var repeatCount by remember { mutableStateOf(30) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Готово к запуску") }
    var tapJob by remember { mutableStateOf<Job?>(null) }

    val markers = remember { mutableStateListOf(UiTapMarker(1, 0.5f, 0.5f)) }
    var nextMarkerId by remember { mutableStateOf(2) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    var areaWindowPos by remember { mutableStateOf(Offset.Zero) }

    fun markerScreenX(marker: UiTapMarker): Int = (areaWindowPos.x + marker.x * areaSize.width).roundToInt()
    fun markerScreenY(marker: UiTapMarker): Int = (areaWindowPos.y + marker.y * areaSize.height).roundToInt()

    fun stop() {
        tapJob?.cancel()
        tapJob = null
        running = false
        status = "Остановлено"
    }

    fun start() {
        vm.refreshPermissions()
        val service = ClickFlowAccessibilityService.liveInstance
        if (service == null) {
            status = "Включи Accessibility один раз"
            vm.openAccessibilitySettings()
            return
        }
        if (markers.isEmpty()) {
            status = "Добавь хотя бы одну метку"
            return
        }
        running = true
        status = "Работает: ${markers.size} меток"
        tapJob = scope.launch {
            repeat(repeatCount) { cycle ->
                if (!isActive) return@launch
                markers.toList().forEachIndexed { index, marker ->
                    if (!isActive) return@launch
                    val ok = service.performSingleTap(markerScreenX(marker), markerScreenY(marker), 70L)
                    status = if (ok) "Цикл ${cycle + 1}/$repeatCount · метка ${index + 1}" else "Ошибка тапа"
                    delay(intervalMs)
                }
            }
            running = false
            status = "Готово"
        }
    }

    DisposableEffect(Unit) { onDispose { tapJob?.cancel() } }

    Page {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF55504A)),
                    ),
                )
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ClickFlow", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("Минималистичная тапалка", color = Color.White.copy(alpha = 0.78f))
                Text("${markers.size} меток · ${intervalMs}мс · $repeatCount повторов", color = Color.White.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold)
            }
        }

        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Метки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Перетащи кружки туда, куда нужно тапать", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${markers.size}/5", fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
                    .onGloballyPositioned {
                        areaSize = it.size
                        areaWindowPos = it.positionInWindow()
                    },
            ) {
                markers.forEachIndexed { index, marker ->
                    val x = if (areaSize.width == 0) 0 else (marker.x * areaSize.width).roundToInt()
                    val y = if (areaSize.height == 0) 0 else (marker.y * areaSize.height).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x - 31, y - 31) }
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .pointerInput(areaSize, marker.id) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val w = areaSize.width.toFloat().coerceAtLeast(1f)
                                    val h = areaSize.height.toFloat().coerceAtLeast(1f)
                                    val currentIndex = markers.indexOfFirst { it.id == marker.id }
                                    if (currentIndex >= 0) {
                                        val current = markers[currentIndex]
                                        markers[currentIndex] = current.copy(
                                            x = (current.x + drag.x / w).coerceIn(0.03f, 0.97f),
                                            y = (current.y + drag.y / h).coerceIn(0.03f, 0.97f),
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (markers.size < 5) {
                            val offset = 0.12f * markers.size
                            markers.add(UiTapMarker(nextMarkerId++, (0.5f + offset).coerceAtMost(0.88f), (0.5f + offset).coerceAtMost(0.88f)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("+ Метка") }
                OutlinedButton(
                    onClick = { if (markers.size > 1) markers.removeAt(markers.lastIndex) },
                    modifier = Modifier.weight(1f),
                ) { Text("− Метка") }
            }
        }

        GlassCard {
            CounterRow("Таймер", "${intervalMs}мс", "-100", "+100", { intervalMs = (intervalMs - 100).coerceAtLeast(100) }, { intervalMs += 100 })
            CounterRow("Повторы", "$repeatCount", "-5", "+5", { repeatCount = (repeatCount - 5).coerceAtLeast(1) }, { repeatCount += 5 })
        }

        Button(
            onClick = { if (running) stop() else start() },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
        ) { Text(if (running) "Стоп" else "Запустить", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        Text(status, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    vm.refreshPermissions()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        context.startService(Intent(context, FloatingTapperOverlayService::class.java))
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Метки поверх") }
            OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.weight(1f)) { Text("Расширенные") }
        }

        if (!permissionStatus.accessibilityEnabled) {
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Включить Accessibility") }
        }
    }
}

@Composable
private fun CounterRow(
    title: String,
    value: String,
    minusLabel: String,
    plusLabel: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMinus) { Text(minusLabel) }
            Button(onClick = onPlus, shape = RoundedCornerShape(16.dp)) { Text(plusLabel) }
        }
    }
}

@Composable
private fun AdvancedScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    Page {
        Text("Расширенные", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        GlassCard {
            Text("Клик по картинке", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Следующий большой шаг: выбрать фото/иконку, область поиска и точку тапа внутри найденного изображения.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { context.startActivity(Intent(context, com.clickflow.android.capture.ScreenCaptureActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Захват экрана") }
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Разрешения") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun PermissionsScreen(vm: ClickFlowViewModel) {
    val status by vm.permissionStatus.collectAsState()
    Page {
        Text("Разрешения", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        GlassCard {
            Text("Accessibility: ${if (status.accessibilityEnabled) "включено" else "не включено"}")
            Text("Overlay: ${if (status.overlayGranted) "включено" else "не включено"}")
        }
        Button(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть Accessibility") }
        Button(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть Overlay") }
        OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) { Text("Обновить") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
