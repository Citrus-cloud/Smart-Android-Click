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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.sp
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

private val SmallPad = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Фото", fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("выбрано $runCount / $limit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+ Фото") }
                Button(onClick = { onRunMulti(templates.filter { it.enabled }.map { it.id }.take(limit)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = runCount > 0, contentPadding = SmallPad) { Text("\u25b6 Старт ($runCount)") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onStop, contentPadding = SmallPad) { Text("\u25a0 Стоп") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Premium", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = premium, onCheckedChange = { premium = it; Premium.setPremiumUnlocked(context, it) })
                }
            }

            Text(
                "Нажимает все включённые фото на одном экране. Бесплатно ${Premium.FREE_TARGET_LIMIT}, Premium до ${Premium.PREMIUM_TARGET_LIMIT}. Для цепочки «иконка \u2192 зайти \u2192 вкладка» используй Сценарий.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
            )

            templates.forEachIndexed { index, template ->
                val expanded = expandedId == template.id
                val canEnable = template.enabled || enabledCount < limit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TemplateThumbnail(template.filePath)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("Фото №${index + 1}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "${template.width}\u00d7${template.height} \u00b7 ${(template.threshold * 100).roundToInt()}% \u00b7 " + if (template.infinite) "\u221e" else "${template.repeatCount}\u0445",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                                )
                            }
                            Switch(checked = template.enabled, onCheckedChange = { setEnabled(template, it) }, enabled = canEnable)
                        }
                        if (!template.enabled && !canEnable) {
                            Text("Лимит $limit \u2014 включи Premium", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { expandedId = if (expanded) null else template.id }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text(if (expanded) "Скрыть" else "Настройки") }
                            Button(onClick = { onRunMulti(listOf(template.id)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("Старт") }
                            OutlinedButton(onClick = { deleteTemplate(template) }, contentPadding = SmallPad, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("\u2715") }
                        }
                        if (expanded) {
                            TemplateEditor(template = template, onUpdate = { updateTemplate(it) })
                        }
                    }
                }
            }

            if (templates.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) { Text("Добавь фото кнопки или иконки кнопкой «+ Фото».", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), contentPadding = SmallPad) { Text("Назад") }
        }
    }
}

@Composable
private fun TemplateThumbnail(filePath: String) {
    val image = remember(filePath) {
        runCatching { BitmapFactory.decodeFile(filePath)?.asImageBitmap() }.getOrNull()
    }
    val shape = RoundedCornerShape(10.dp)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.size(46.dp).clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(46.dp).clip(shape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) { Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun TemplateEditor(template: ImageClickTemplate, onUpdate: (ImageClickTemplate) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Похожесть: ${(template.threshold * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.threshold, onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) }, valueRange = 0.5f..0.99f)
            Text("Совет: ~80% обычно лучше всего. 99% часто не ловит.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text("Тап X: ${(template.tapX * 100).roundToInt()}%  \u00b7  Тап Y: ${(template.tapY * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.tapX, onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Slider(value = template.tapY, onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("Масштаб: ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.scaleMin, onValueChange = { onUpdate(template.copy(scaleMin = it.coerceIn(0.5f, template.scaleMax))) }, valueRange = 0.5f..2f)
            Slider(value = template.scaleMax, onValueChange = { onUpdate(template.copy(scaleMax = it.coerceIn(template.scaleMin, 2f))) }, valueRange = 0.5f..2f)
            OutlinedButton(onClick = { onUpdate(template.copy(scaleMin = 0.65f, scaleMax = 1.45f)) }, modifier = Modifier.fillMaxWidth(), contentPadding = SmallPad) { Text("Сброс масштаба 65\u2013145%") }

            Text("Повторы: " + if (template.infinite) "\u221e" else "${template.repeatCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u22125") }
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+5") }
                OutlinedButton(onClick = { onUpdate(template.copy(infinite = !template.infinite)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text(if (template.infinite) "\u221e ВКЛ" else "\u221e") }
            }

            Text("Интервал: ${template.intervalMs} мс", fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u2212200") }
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+200") }
            }
            Text("Минимум ~1 сек: система ограничивает скриншоты", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
