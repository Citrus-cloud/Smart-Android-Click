package com.clickflow.android.textclick

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
            Text("Текст", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = config.query, onValueChange = { save(config.copy(query = it)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ToggleRow("Часть строки", config.contains) { save(config.copy(contains = it)) }
                    ToggleRow("Регистр", config.ignoreCase) { save(config.copy(ignoreCase = it)) }
                    ToggleRow("Постоянно", config.continuous) { save(config.copy(continuous = it)) }
                }
            }
            Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), enabled = config.query.isNotBlank()) { Text("Старт") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Стоп") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
