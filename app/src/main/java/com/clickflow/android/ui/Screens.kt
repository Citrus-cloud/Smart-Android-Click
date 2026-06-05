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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

@Composable
fun ClickFlowApp(vm: ClickFlowViewModel) {
    val screen by vm.screen.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.ADVANCED -> MinimalAdvancedScreen(vm)
            Screen.PERMISSIONS -> MinimalPermissionsScreen(vm)
            else -> MinimalTapperScreen(vm)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun MinimalTapperScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionStatus by vm.permissionStatus.collectAsState()

    var intervalMs by remember { mutableStateOf(500L) }
    var repeatCount by remember { mutableStateOf(30) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Готово") }
    var tapJob by remember { mutableStateOf<Job?>(null) }

    var marker by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    var areaWindowPos by remember { mutableStateOf(Offset.Zero) }

    fun markerScreenX(): Int = (areaWindowPos.x + marker.x * areaSize.width).roundToInt()
    fun markerScreenY(): Int = (areaWindowPos.y + marker.y * areaSize.height).roundToInt()

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
            status = "Включи Accessibility Service один раз в разрешениях"
            vm.openAccessibilitySettings()
            return
        }
        if (areaSize.width <= 0 || areaSize.height <= 0) {
            status = "Сначала поставь метку"
            return
        }
        running = true
        status = "Работает: $repeatCount тапов, интервал ${intervalMs}мс"
        tapJob = scope.launch {
            repeat(repeatCount) { index ->
                if (!isActive) return@launch
                val ok = service.performSingleTap(markerScreenX(), markerScreenY(), 70L)
                status = if (ok) "Тап ${index + 1}/$repeatCount" else "Не удалось выполнить тап"
                delay(intervalMs)
            }
            running = false
            status = "Готово"
        }
    }

    DisposableEffect(Unit) { onDispose { tapJob?.cancel() } }

    Page {
        Text("ClickFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Простая тапалка для теста", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Метка тапа", fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .onGloballyPositioned {
                            areaSize = it.size
                            areaWindowPos = it.positionInWindow()
                        },
                ) {
                    val x = if (areaSize.width == 0) 0 else (marker.x * areaSize.width).roundToInt()
                    val y = if (areaSize.height == 0) 0 else (marker.y * areaSize.height).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x - 28, y - 28) }
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .pointerInput(areaSize) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    val w = areaSize.width.toFloat().coerceAtLeast(1f)
                                    val h = areaSize.height.toFloat().coerceAtLeast(1f)
                                    marker = Offset(
                                        (marker.x + drag.x / w).coerceIn(0f, 1f),
                                        (marker.y + drag.y / h).coerceIn(0f, 1f),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("+") }
                }
                Text("Координаты: ${markerScreenX()}, ${markerScreenY()}")
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Таймер", fontWeight = FontWeight.SemiBold)
                    Text("${intervalMs}мс")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { intervalMs = (intervalMs - 100).coerceAtLeast(100) }, modifier = Modifier.weight(1f)) { Text("-100") }
                    OutlinedButton(onClick = { intervalMs += 100 }, modifier = Modifier.weight(1f)) { Text("+100") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Повторы", fontWeight = FontWeight.SemiBold)
                    Text("$repeatCount")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { repeatCount = (repeatCount - 5).coerceAtLeast(1) }, modifier = Modifier.weight(1f)) { Text("-5") }
                    OutlinedButton(onClick = { repeatCount += 5 }, modifier = Modifier.weight(1f)) { Text("+5") }
                }
            }
        }

        Button(
            onClick = { if (running) stop() else start() },
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text(if (running) "Стоп" else "Запустить", style = MaterialTheme.typography.titleMedium) }

        Text(status, fontWeight = FontWeight.SemiBold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    vm.refreshPermissions()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        context.startService(Intent(context, FloatingTapperOverlayService::class.java))
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Метка поверх") }
            OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.weight(1f)) { Text("Расширенные") }
        }

        if (!permissionStatus.accessibilityEnabled) {
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) {
                Text("Включить Accessibility")
            }
        }
    }
}

@Composable
private fun MinimalAdvancedScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    Page {
        Text("Расширенные функции", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Здесь оставляем только полезное: метка поверх экрана и подготовка клика по картинке/иконке.")
        Button(
            onClick = { context.startActivity(Intent(context, com.clickflow.android.capture.ScreenCaptureActivity::class.java)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Открыть захват экрана") }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Клик по картинке", fontWeight = FontWeight.SemiBold)
                Text("Следующий большой шаг: добавить фото/иконку, область поиска и точку тапа внутри найденной картинки.")
            }
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) { Text("Разрешения") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}

@Composable
private fun MinimalPermissionsScreen(vm: ClickFlowViewModel) {
    val status by vm.permissionStatus.collectAsState()
    Page {
        Text("Разрешения", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Accessibility: ${if (status.accessibilityEnabled) "включено" else "не включено"}")
        Text("Overlay: ${if (status.overlayGranted) "включено" else "не включено"}")
        Button(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть Accessibility") }
        Button(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть Overlay") }
        OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) { Text("Обновить") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
