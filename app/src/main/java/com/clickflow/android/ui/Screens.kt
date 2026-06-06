package com.clickflow.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.Screen
import com.clickflow.android.imageclick.ImageTemplateActivity
import com.clickflow.android.overlay.FloatingTapperOverlayService
import com.clickflow.android.scenario.ScenarioActivity
import com.clickflow.android.textclick.TextClickActivity

private const val PREFS_NAME = "clickflow_tapper"
private const val KEY_INTERVAL_MS = "interval_ms"
private const val KEY_REPEAT_COUNT = "repeat_count"
private const val KEY_INFINITE = "infinite"

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
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } }
}

private fun launchOverlay(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } else {
        context.startService(Intent(context, FloatingTapperOverlayService::class.java))
    }
}

@Composable
private fun HomeScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val permissionStatus by vm.permissionStatus.collectAsState()

    var intervalMs by remember { mutableStateOf(prefs.getLong(KEY_INTERVAL_MS, 500L)) }
    var repeatCount by remember { mutableStateOf(prefs.getInt(KEY_REPEAT_COUNT, 30)) }
    var infiniteMode by remember { mutableStateOf(prefs.getBoolean(KEY_INFINITE, false)) }

    fun saveAll() {
        prefs.edit()
            .putLong(KEY_INTERVAL_MS, intervalMs)
            .putInt(KEY_REPEAT_COUNT, repeatCount)
            .putBoolean(KEY_INFINITE, infiniteMode)
            .apply()
    }

    Page {
        Text("ClickFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        Button(
            onClick = { saveAll(); vm.refreshPermissions(); launchOverlay(context) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
        ) { Text("◎  Запустить метку", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        PlainCard {
            SettingRow("Интервал", "$intervalMs мс", { intervalMs = (intervalMs - 100).coerceAtLeast(100); saveAll() }, { intervalMs += 100; saveAll() })
            SettingRow("Повторы", if (infiniteMode) "∞" else "$repeatCount", { repeatCount = (repeatCount - 5).coerceAtLeast(1); infiniteMode = false; saveAll() }, { repeatCount += 5; infiniteMode = false; saveAll() })
            OutlinedButton(onClick = { infiniteMode = !infiniteMode; saveAll() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (infiniteMode) "Бесконечно: ВКЛ" else "Бесконечно: ВЫКЛ")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { context.startActivity(Intent(context, ImageTemplateActivity::class.java)) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Фото") }
            Button(onClick = { context.startActivity(Intent(context, TextClickActivity::class.java)) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Текст") }
        }

        OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.fillMaxWidth()) { Text("Расширенные") }

        if (!permissionStatus.accessibilityEnabled) {
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Включить Accessibility") }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMinus) { Text("−") }
            Button(onClick = onPlus, shape = RoundedCornerShape(14.dp)) { Text("+") }
        }
    }
}

@Composable
private fun AdvancedScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    Page {
        Text("Расширенные", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Button(onClick = { context.startActivity(Intent(context, ImageTemplateActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Клик по фото") }
        Button(onClick = { context.startActivity(Intent(context, TextClickActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Клик по тексту") }
        Button(onClick = { context.startActivity(Intent(context, ScenarioActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Сценарий") }
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
            Text("Accessibility: ${if (status.accessibilityEnabled) "включено" else "не включено"}")
            Text("Метка поверх: ${if (status.overlayGranted) "включено" else "не включено"}")
        }
        Button(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть Accessibility") }
        Button(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) { Text("Открыть метку поверх") }
        OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) { Text("Обновить") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
    }
}
