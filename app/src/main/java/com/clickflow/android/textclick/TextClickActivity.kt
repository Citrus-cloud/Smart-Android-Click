package com.clickflow.android.textclick

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.ui.ClickFlowTheme

class TextClickActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                TextClickScreen(
                    context = this,
                    onRun = {
                        startService(Intent(this, TextClickService::class.java).apply { action = TextClickService.ACTION_START })
                        finish()
                    },
                    onStop = { startService(Intent(this, TextClickService::class.java).apply { action = TextClickService.ACTION_STOP }) },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun TextClickScreen(context: Context, onRun: () -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    var config by remember { mutableStateOf(TextClickStore.load(context)) }
    fun save(updated: TextClickConfig) {
        config = updated
        TextClickStore.save(context, updated)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("\u0422\u0435\u043a\u0441\u0442", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = config.query, onValueChange = { save(config.copy(query = it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ToggleRow("\u0427\u0430\u0441\u0442\u044c \u0441\u0442\u0440\u043e\u043a\u0438", config.contains) { save(config.copy(contains = it)) }
                    ToggleRow("\u0420\u0435\u0433\u0438\u0441\u0442\u0440", config.ignoreCase) { save(config.copy(ignoreCase = it)) }

                    Text("\u041f\u043e\u0432\u0442\u043e\u0440\u044b: " + if (config.infinite) "\u221e" else "${config.repeatCount}", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { save(config.copy(repeatCount = (config.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("\u22125") }
                        OutlinedButton(onClick = { save(config.copy(repeatCount = (config.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("+5") }
                        OutlinedButton(onClick = { save(config.copy(infinite = !config.infinite)) }, modifier = Modifier.weight(1f)) { Text(if (config.infinite) "\u221e \u0412\u041a\u041b" else "\u221e") }
                    }

                    Text("\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b: ${config.intervalMs} \u043c\u0441")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { save(config.copy(intervalMs = (config.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f)) { Text("\u2212200") }
                        OutlinedButton(onClick = { save(config.copy(intervalMs = (config.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f)) { Text("+200") }
                    }
                    Text("\u041c\u0438\u043d\u0438\u043c\u0443\u043c ~1 \u0441\u0435\u043a: \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0438\u0432\u0430\u0435\u0442 \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), enabled = config.query.isNotBlank()) { Text("\u0421\u0442\u0430\u0440\u0442") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("\u0421\u0442\u043e\u043f") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("\u041d\u0430\u0437\u0430\u0434") }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
