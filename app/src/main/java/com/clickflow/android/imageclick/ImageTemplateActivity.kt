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
            Text("\u0424\u043e\u0442\u043e", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("+ \u0424\u043e\u0442\u043e") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("\u0421\u0442\u043e\u043f") }

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
                            Text("${template.width}\u00d7${template.height}")
                        }
                        Text("${(template.threshold * 100).roundToInt()}% \u00b7 ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}% \u00b7 " + if (template.infinite) "\u221e" else "${template.repeatCount}\u0445", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedId.value = template.id }, modifier = Modifier.weight(1f)) { Text("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438") }
                            Button(onClick = { onRun(template.id) }, modifier = Modifier.weight(1f)) { Text("\u0421\u0442\u0430\u0440\u0442") }
                        }
                        OutlinedButton(
                            onClick = {
                                File(template.filePath).delete()
                                templates.removeAll { it.id == template.id }
                                selectedId.value = templates.firstOrNull()?.id
                                persist()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("\u0423\u0434\u0430\u043b\u0438\u0442\u044c") }
                    }
                }
            }

            if (templates.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) { Text("\u0414\u043e\u0431\u0430\u0432\u044c \u0444\u043e\u0442\u043e \u043a\u043d\u043e\u043f\u043a\u0438 \u0438\u043b\u0438 \u0438\u043a\u043e\u043d\u043a\u0438") }
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

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("\u041d\u0430\u0437\u0430\u0434") }
        }
    }
}

@Composable
private fun TemplateEditor(template: ImageClickTemplate, onUpdate: (ImageClickTemplate) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("\u041f\u043e\u0445\u043e\u0436\u0435\u0441\u0442\u044c: ${(template.threshold * 100).roundToInt()}%")
            Slider(value = template.threshold, onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) }, valueRange = 0.5f..0.99f)
            Text("\u0422\u0430\u043f X: ${(template.tapX * 100).roundToInt()}%")
            Slider(value = template.tapX, onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("\u0422\u0430\u043f Y: ${(template.tapY * 100).roundToInt()}%")
            Slider(value = template.tapY, onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("\u041c\u0430\u0441\u0448\u0442\u0430\u0431: ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}%")
            Slider(value = template.scaleMin, onValueChange = { onUpdate(template.copy(scaleMin = it.coerceIn(0.5f, template.scaleMax))) }, valueRange = 0.5f..2f)
            Slider(value = template.scaleMax, onValueChange = { onUpdate(template.copy(scaleMax = it.coerceIn(template.scaleMin, 2f))) }, valueRange = 0.5f..2f)
            OutlinedButton(onClick = { onUpdate(template.copy(scaleMin = 0.65f, scaleMax = 1.45f)) }, modifier = Modifier.fillMaxWidth()) { Text("65\u2013145%") }

            Text("\u041f\u043e\u0432\u0442\u043e\u0440\u044b: " + if (template.infinite) "\u221e" else "${template.repeatCount}", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("\u22125") }
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f)) { Text("+5") }
                OutlinedButton(onClick = { onUpdate(template.copy(infinite = !template.infinite)) }, modifier = Modifier.weight(1f)) { Text(if (template.infinite) "\u221e \u0412\u041a\u041b" else "\u221e") }
            }

            Text("\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b: ${template.intervalMs} \u043c\u0441")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f)) { Text("\u2212200") }
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f)) { Text("+200") }
            }
            Text("\u041c\u0438\u043d\u0438\u043c\u0443\u043c ~1 \u0441\u0435\u043a: \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0438\u0432\u0430\u0435\u0442 \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
