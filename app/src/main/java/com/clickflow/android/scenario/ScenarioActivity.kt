package com.clickflow.android.scenario

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.ui.ClickFlowTheme

class ScenarioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ScenarioScreen(
                    context = this,
                    onStart = {
                        startService(Intent(this, ScenarioRunnerService::class.java).apply { action = ScenarioRunnerService.ACTION_START })
                    },
                    onStop = {
                        startService(Intent(this, ScenarioRunnerService::class.java).apply { action = ScenarioRunnerService.ACTION_STOP })
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ScenarioScreen(context: Context, onStart: () -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    var config by remember { mutableStateOf(ScenarioStore.load(context)) }
    var status by remember { mutableStateOf("Готово") }
    fun refresh() { config = ScenarioStore.load(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Сценарий", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Простой сценарий запускает сохранённые overlay-метки по очереди. Настройки берутся из главного экрана: таймер, повторы, бесконечный режим.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Текущий сценарий", fontWeight = FontWeight.Bold)
                    Text("Метки: ${config.markers.size}")
                    Text("Таймер: ${config.intervalMs}мс")
                    Text("Повторы: ${if (config.infinite) "∞" else config.repeatCount}")
                    Text("Статус: $status")
                    if (config.markers.isEmpty()) {
                        Text("Сначала открой «Метки поверх», добавь и расставь метки. После этого сценарий сможет их запускать.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            config.markers.forEachIndexed { index, marker ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Действие ${index + 1}: тап по метке ${marker.id}", fontWeight = FontWeight.Bold)
                        Text("Координаты: ${marker.x}, ${marker.y}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Button(
                onClick = {
                    onStart()
                    status = "Сценарий запущен"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = config.markers.isNotEmpty(),
                shape = RoundedCornerShape(20.dp),
            ) { Text("Запустить сценарий") }
            OutlinedButton(
                onClick = {
                    onStop()
                    status = "Сценарий остановлен"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Остановить сценарий") }
            OutlinedButton(onClick = { refresh(); status = "Обновлено" }, modifier = Modifier.fillMaxWidth()) { Text("Обновить из меток") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}
