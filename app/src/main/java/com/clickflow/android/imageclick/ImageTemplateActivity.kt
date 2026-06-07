package com.clickflow.android.imageclick

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.core.Premium
import com.clickflow.android.ui.ClickFlowTheme
import java.io.File
import kotlin.math.roundToInt

class ImageTemplateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ImageTemplateScreen(
                    context = this,
                    onRunMulti = { ids ->
                        if (ids.isNotEmpty()) {
                            startService(Intent(this, ImageClickService::class.java).apply {
                                action = ImageClickService.ACTION_START
                                putExtra(ImageClickService.EXTRA_TEMPLATE_IDS, ids.joinToString(","))
                            })
                            finish()
                        }
                    },
                    onStop = { startService(Intent(this, ImageClickService::class.java).apply { action = ImageClickService.ACTION_STOP }) },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ImageTemplateScreen(context: Context, onRunMulti: (List<String>) -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    val templates = remember { mutableStateListOf<ImageClickTemplate>().also { it.addAll(ImageClickTemplateStore.loadTemplates(context)) } }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var premium by remember { mutableStateOf(Premium.isPremiumUnlocked(context)) }
    val limit = if (premium) Premium.PREMIUM_TARGET_LIMIT else Premium.FREE_TARGET_LIMIT
    val enabledCount = templates.count { it.enabled }
    val runCount = minOf(enabledCount, limit)
    fun persist() = ImageClickTemplateStore.saveTemplates(context, templates)
    fun setEnabled(template: ImageClickTemplate, value: Boolean) {
        val idx = templates.indexOfFirst { it.id == template.id }
        if (idx < 0) return
        templates[idx] = templates[idx].copy(enabled = value)
        persist()
    }
    fun updateTemplate(updated: ImageClickTemplate) {
        val idx = templates.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            templates[idx] = updated.normalized()
            persist()
        }
    }
    fun deleteTemplate(template: ImageClickTemplate) {
        File(template.filePath).delete()
        templates.removeAll { it.id == template.id }
        if (expandedId == template.id) expandedId = null
        persist()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val template = ImageClickTemplateStore.copyUriAsTemplate(context, uri, templates.size + 1)
        if (template != null) {
            templates.add(template)
            persist()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Фото", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Мультитап: $enabledCount / $limit", fontWeight = FontWeight.Bold)
                    Text(
                        if (premium) "Premium активен — до ${Premium.PREMIUM_TARGET_LIMIT} целей одновременно"
                        else "Бесплатно — ${Premium.FREE_TARGET_LIMIT} цели. Premium — до ${Premium.PREMIUM_TARGET_LIMIT}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Premium (тест)")
                        Switch(checked = premium, onCheckedChange = { premium = it; Premium.setPremiumUnlocked(context, it) })
                    }
                    Text(
                        "Мультитап нажимает все выбранные фото на ОДНОМ экране за один проход. Для цепочки «нажать иконку → зайти → вкладка» используй Сценарий.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("+ Фото") }
            Button(onClick = { onRunMulti(templates.filter { it.enabled }.map { it.id }.take(limit)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), enabled = runCount > 0) { Text("\u25b6 Старт мультитап ($runCount)") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Стоп") }

            templates.forEachIndexed { index, template ->
                val expanded = expandedId == template.id
                val canEnable = template.enabled || enabledCount < limit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                TemplateThumbnail(template.filePath)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Фото №${index + 1}", fontWeight = FontWeight.Bold)
                                    Text("${template.width}\u00d7${template.height}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(onClick = { deleteTemplate(template) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("\u2715") }
                        }
                        Text("${(template.threshold * 100).roundToInt()}% \u00b7 ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}% \u00b7 " + if (template.infinite) "\u221e" else "${template.repeatCount}х", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("В мультитап")
                            Switch(checked = template.enabled, onCheckedChange = { setEnabled(template, it) }, enabled = canEnable)
                        }
                        if (!template.enabled && !canEnable) {
                            Text("Лимит $limit — включи Premium (тест) для ${Premium.PREMIUM_TARGET_LIMIT}", color = MaterialTheme.colorScheme.error)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { expandedId = if (expanded) null else template.id }, modifier = Modifier.weight(1f)) { Text(if (expanded) "Скрыть" else "Настройки") }
                            Button(onClick = { onRunMulti(listOf(template.id)) }, modifier = Modifier.weight(1f)) { Text("Старт") }
                        }
                        if (expanded) {
                            TemplateEditor(template = template, onUpdate = { updateTemplate(it) })
                        }
                    }
                }
            }

            if (templates.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) { Text("Добавь фото кнопки или иконки") }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}

@Composable
private fun TemplateThumbnail(filePath: String) {
    val image = remember(filePath) {
        runCatching { BitmapFactory.decodeFile(filePath)?.asImageBitmap() }.getOrNull()
    }
    val shape = RoundedCornerShape(12.dp)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.size(54.dp).clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(54.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun TemplateEditor(template: ImageClickTemplate, onUpdate: (ImageClickTemplate) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Похожесть: ${(template.threshold * 100).roundToInt()}%")
            Slider(value = template.threshold, onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) }, valueRange = 0.5f..0.99f)
            Text("Тап X: ${(template.tapX * 100).roundToInt()}%")
            Slider(value = template.tapX, onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("Тап Y: ${(template.tapY * 100).roundToInt()}%")
            Slider(value = template.tapY, onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("Масштаб: ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}%")
            Slider(value = template.scaleMin, onValueChange = { onUpdate(template.copy(scaleMin = it.coerceIn(0.5f, template.scaleMax))) }, valueRange = 0.5f..2f)
            Slider(value = template.scaleMax, onValueChange = { onUpdate(template.copy(scaleMax = it.coerceIn(template.scaleMin, 2f))) }, valueRange = 0.5f..2f)
            OutlinedButton(onClick = { onUpdate(template.copy(scaleMin = 0.65f, scaleMax = 1.45f)) }, modifier = Modifier.fillMaxWidth()) { Text("65\u2013145%") }

            Text("Повторы: " + if (template.infinite) "\u221e" else "${template.repeatCount}", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("\u22125") }
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("+5") }
                OutlinedButton(onClick = { onUpdate(template.copy(infinite = !template.infinite)) }, modifier = Modifier.weight(1f)) { Text(if (template.infinite) "\u221e ВКЛ" else "\u221e") }
            }

            Text("Интервал: ${template.intervalMs} мс")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f)) { Text("\u2212200") }
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f)) { Text("+200") }
            }
            Text("Минимум ~1 сек: система ограничивает скриншоты", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
