package com.clickflow.android.imageclick

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    onRun = { templateId ->
                        startService(Intent(this, ImageClickService::class.java).apply {
                            action = ImageClickService.ACTION_START
                            putExtra(ImageClickService.EXTRA_TEMPLATE_ID, templateId)
                        })
                        finish()
                    },
                    onStop = { startService(Intent(this, ImageClickService::class.java).apply { action = ImageClickService.ACTION_STOP }) },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ImageTemplateScreen(context: Context, onRun: (String) -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    val templates = remember { mutableStateListOf<ImageClickTemplate>().also { it.addAll(ImageClickTemplateStore.loadTemplates(context)) } }
    val selectedId = remember { mutableStateOf<String?>(templates.firstOrNull()?.id) }
    val selected = templates.firstOrNull { it.id == selectedId.value }
    fun persist() = ImageClickTemplateStore.saveTemplates(context, templates)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val template = ImageClickTemplateStore.copyUriAsTemplate(context, uri, templates.size + 1)
        if (template != null) {
            templates.add(template)
            selectedId.value = template.id
            persist()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Фото", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("+ Фото") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("Стоп") }

            templates.forEachIndexed { index, template ->
                val active = selectedId.value == template.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold)
                            Text("${template.width}×${template.height}")
                        }
                        Text("${(template.threshold * 100).roundToInt()}% · ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedId.value = template.id }, modifier = Modifier.weight(1f)) { Text("Настройки") }
                            Button(onClick = { onRun(template.id) }, modifier = Modifier.weight(1f)) { Text("Старт") }
                        }
                        OutlinedButton(
                            onClick = {
                                File(template.filePath).delete()
                                templates.removeAll { it.id == template.id }
                                selectedId.value = templates.firstOrNull()?.id
                                persist()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Удалить") }
                    }
                }
            }

            if (templates.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) { Text("Добавь фото кнопки или иконки") }
                }
            }

            if (selected != null) {
                TemplateEditor(
                    template = selected,
                    onUpdate = { updated ->
                        val idx = templates.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) {
                            templates[idx] = updated.normalized()
                            persist()
                        }
                    },
                )
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
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
            OutlinedButton(onClick = { onUpdate(template.copy(scaleMin = 0.65f, scaleMax = 1.45f)) }, modifier = Modifier.fillMaxWidth()) { Text("65–145%") }
            OutlinedButton(onClick = { onUpdate(template.copy(continuous = !template.continuous)) }, modifier = Modifier.fillMaxWidth()) { Text(if (template.continuous) "Постоянно" else "Один раз") }
        }
    }
}
