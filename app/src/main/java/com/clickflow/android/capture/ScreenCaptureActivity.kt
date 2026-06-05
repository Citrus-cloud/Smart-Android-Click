package com.clickflow.android.capture

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.ui.ClickFlowTheme

/**
 * Dedicated activity that owns the MediaProjection consent flow (Step 66, Part 2).
 *
 * The system consent dialog can only be launched from an Activity result contract, so the
 * capture screen lives here rather than inside the main Compose navigation. On consent it starts
 * [ScreenCaptureService] as a foreground service; the service captures a single in-memory frame
 * and mirrors its state into [ScreenCaptureRepository], which this screen observes.
 */
class ScreenCaptureActivity : ComponentActivity() {

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                ScreenCaptureRepository.onPermissionResult(true)
                val svc = Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_DATA, data)
                }
                startForegroundService(svc)
            } else {
                ScreenCaptureRepository.onPermissionResult(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ScreenCaptureScreen(
                    onRequestCapture = {
                        ScreenCaptureRepository.requestPermission()
                        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                            as MediaProjectionManager
                        projectionLauncher.launch(mpm.createScreenCaptureIntent())
                    },
                    onStop = {
                        val svc = Intent(this, ScreenCaptureService::class.java).apply {
                            action = ScreenCaptureService.ACTION_STOP
                        }
                        startService(svc)
                        ScreenCaptureRepository.stop()
                    },
                    onReset = { ScreenCaptureRepository.reset() },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ScreenCaptureScreen(
    onRequestCapture: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val state by ScreenCaptureRepository.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Захват экрана (превью)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Один кадр захватывается только в память. На диск не сохраняется, не отправляется и не анализируется (Шаг 66).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Статус: ${state.status}", fontWeight = FontWeight.SemiBold)
                    Text("Разрешение: ${state.permission}")
                    val f = state.frame
                    if (f != null) {
                        Text("Кадр: ${f.width} × ${f.height} px")
                        Text("Время захвата (ms): ${f.capturedAtMs}")
                    } else {
                        Text("Кадр: —")
                    }
                    state.error?.let {
                        Text("Ошибка: $it", color = MaterialTheme.colorScheme.error)
                    }
                    Text("in-memory only: ${state.inMemoryOnly}", style = MaterialTheme.typography.bodySmall)
                    Text("persisted to disk: ${state.persistedToDisk}", style = MaterialTheme.typography.bodySmall)
                    Text("analysis performed: ${state.analysisPerformed}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onRequestCapture, modifier = Modifier.fillMaxWidth()) {
                Text("Запросить разрешение и захватить кадр")
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Остановить захват")
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Сбросить")
            }
            Text(
                "Реальные нажатия по-прежнему заблокированы. Этот экран только демонстрирует захват.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Назад")
            }
        }
    }
}
