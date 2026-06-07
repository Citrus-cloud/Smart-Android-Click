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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/** Visible marker so a user can confirm which APK is actually installed. Bump on each fix. */
private const val BUILD_TAG = "fix-acc-2 \u00b7 07.06"

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
    val permissionStatus by vm.permissionStatus.collectAsState()

    Page {
        Text("ClickFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

        Button(
            onClick = { vm.refreshPermissions(); launchOverlay(context) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
        ) { Text("\u25ce  \u0417\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043c\u0435\u0442\u043a\u0443", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

        Text(
            "\u0412\u0441\u0435 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0442\u0430\u043f\u0430 \u2014 \u043f\u0440\u044f\u043c\u043e \u043d\u0430 \u043c\u0435\u0442\u043a\u0435: \u043f\u043e\u0432\u0442\u043e\u0440\u044b, \u221e \u0438 \u0438\u043d\u0442\u0435\u0440\u0432\u0430\u043b.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { context.startActivity(Intent(context, ImageTemplateActivity::class.java)) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("\u0424\u043e\u0442\u043e") }
            Button(onClick = { context.startActivity(Intent(context, TextClickActivity::class.java)) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("\u0422\u0435\u043a\u0441\u0442") }
        }

        OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.fillMaxWidth()) { Text("\u0420\u0430\u0441\u0448\u0438\u0440\u0435\u043d\u043d\u044b\u0435") }

        if (!permissionStatus.accessibilityReady) {
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (permissionStatus.accessibilityNeedsRestart) "Перезапустить Accessibility" else "Включить Accessibility")
            }
        }
    }
}

@Composable
private fun AdvancedScreen(vm: ClickFlowViewModel) {
    val context = LocalContext.current
    Page {
        Text("\u0420\u0430\u0441\u0448\u0438\u0440\u0435\u043d\u043d\u044b\u0435", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Button(onClick = { context.startActivity(Intent(context, ImageTemplateActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("\u041a\u043b\u0438\u043a \u043f\u043e \u0444\u043e\u0442\u043e") }
        Button(onClick = { context.startActivity(Intent(context, TextClickActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("\u041a\u043b\u0438\u043a \u043f\u043e \u0442\u0435\u043a\u0441\u0442\u0443") }
        Button(onClick = { context.startActivity(Intent(context, ScenarioActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("\u0421\u0446\u0435\u043d\u0430\u0440\u0438\u0439") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) { Text("\u0420\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u044f") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("\u041d\u0430\u0437\u0430\u0434") }
    }
}

@Composable
private fun PermissionsScreen(vm: ClickFlowViewModel) {
    val status by vm.permissionStatus.collectAsState()
    Page {
        Text("\u0420\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u044f", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        PlainCard {
            val accessibilityLine = when {
                status.accessibilityReady -> "Accessibility: включено и работает"
                status.accessibilityNeedsRestart -> "Accessibility: включено в настройках, но служба не запущена — выключите и снова включите ClickFlow в спец. возможностях"
                else -> "Accessibility: не включено"
            }
            Text(accessibilityLine)
            Text("\u041c\u0435\u0442\u043a\u0430 \u043f\u043e\u0432\u0435\u0440\u0445: ${if (status.overlayGranted) "\u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e" else "\u043d\u0435 \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e"}")
            Text("\u0421\u0431\u043e\u0440\u043a\u0430: $BUILD_TAG", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) { Text("\u041e\u0442\u043a\u0440\u044b\u0442\u044c Accessibility") }
        Button(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) { Text("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043c\u0435\u0442\u043a\u0443 \u043f\u043e\u0432\u0435\u0440\u0445") }
        OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) { Text("\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c") }
        OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("\u041d\u0430\u0437\u0430\u0434") }
    }
}
