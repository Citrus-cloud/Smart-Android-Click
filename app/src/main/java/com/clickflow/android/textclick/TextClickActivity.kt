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
import com.clickflow.android.core.Premium
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
    var premium by remember { mutableStateOf(Premium.isPremiumUnlocked(context)) }
    val limit = if (premium) Premium.PREMIUM_TARGET_LIMIT else Premium.FREE_TARGET_LIMIT
    val canAdd = config.queries.size < limit
    val activeCount = config.queries.count { it.isNotBlank() }
    val runCount = minOf(activeCount, limit)
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
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Мультитап: ${config.queries.size} / $limit", fontWeight = FontWeight.Bold)
                    Text(
                        if (premium) "Premium активен — до ${Premium.PREMIUM_TARGET_LIMIT} текстов одновременно"
                        else "Бесплатно — ${Premium.FREE_TARGET_LIMIT} текста. Premium — до ${Premium.PREMIUM_TARGET_LIMIT}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Premium (тест)")
                        Switch(checked = premium, onCheckedChange = { premium = it; Premium.setPremiumUnlocked(context, it) })
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    config.queries.forEachIndexed { i, q ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = q,
                                onValueChange = { v ->
                                    val list = config.queries.toMutableList()
                                    list[i] = v
                                    save(config.copy(queries = list))
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            if (config.queries.size > 1) {
                                OutlinedButton(onClick = {
                                    val list = config.queries.toMutableList()
                                    list.removeAt(i)
                                    save(config.copy(queries = list))
                                }) { Text("\u2715") }
                            }
                        }
                    }
                    OutlinedButton(onClick = { if (canAdd) save(config.copy(queries = config.queries + "")) }, modifier = Modifier.fillMaxWidth(), enabled = canAdd) { Text("+ Текст") }
                    if (!canAdd && !premium) {
                        Text("Лимит $limit — включи Premium (тест) для ${Premium.PREMIUM_TARGET_LIMIT}", color = MaterialTheme.colorScheme.error)
                    }

                    ToggleRow("Часть строки", config.contains) { save(config.copy(contains = it)) }
                    ToggleRow("Регистр", config.ignoreCase) { save(config.copy(ignoreCase = it)) }

                    Text("Повторы: " + if (config.infinite) "\u221e" else "${config.repeatCount}", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { save(config.copy(repeatCount = (config.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("\u22125") }
                        OutlinedButton(onClick = { save(config.copy(repeatCount = (config.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("+5") }
                        OutlinedButton(onClick = { save(config.copy(infinite = !config.infinite)) }, modifier = Modifier.weight(1f)) { Text(if (config.infinite) "\u221e ВКЛ" else "\u221e") }
                    }

                    Text("Интервал: ${config.intervalMs} мс")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { save(config.copy(intervalMs = (config.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f)) { Text("\u2212200") }
                        OutlinedButton(onClick = { save(config.copy(intervalMs = (config.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f)) { Text("+200") }
                    }
                    Text("Минимум ~1 сек: система ограничивает скриншоты", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = onRun, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), enabled = runCount > 0) { Text("\u25b6 Старт мультитап ($runCount)") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Стоп") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
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
