package com.clickflow.android.imageclick

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
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
    private var pendingTemplateId: String? = null

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val templateId = pendingTemplateId
        if (result.resultCode == RESULT_OK && data != null && !templateId.isNullOrBlank()) {
            val svc = Intent(this, ImageClickService::class.java).apply {
                action = ImageClickService.ACTION_START
                putExtra(ImageClickService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ImageClickService.EXTRA_DATA, data)
                putExtra(ImageClickService.EXTRA_TEMPLATE_ID, templateId)
            }
            startForegroundService(svc)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ImageTemplateScreen(
                    context = this,
                    onRun = { templateId ->
                        pendingTemplateId = templateId
                        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        projectionLauncher.launch(mpm.createScreenCaptureIntent())
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ImageTemplateScreen(context: Context, onRun: (String) -> Unit, onBack: () -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Клик по картинке", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Добавь фото/иконку, настрой похожесть и нажми запуск. ClickFlow будет искать шаблон на экране и тапнет в выбранную точку.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("+ Добавить фото / иконку") }

            templates.forEachIndexed { index, template ->
                val active = selectedId.value == template.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${template.name}", fontWeight = FontWeight.Bold)
                            Text("${template.width}×${template.height}")
                        }
                        Text("Порог: ${(template.threshold * 100).roundToInt()}% · Тап: ${(template.tapX * 100).roundToInt()}%, ${(template.tapY * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedId.value = template.id }, modifier = Modifier.weight(1f)) { Text("Настроить") }
                            Button(onClick = { onRun(template.id) }, modifier = Modifier.weight(1f)) { Text("Запустить") }
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
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Шаблонов пока нет", fontWeight = FontWeight.Bold)
                        Text("Выбери иконку или фрагмент скриншота. Лучше использовать небольшую картинку кнопки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (selected != null) {
                TemplateEditor(
                    template = selected,
                    onUpdate = { updated ->
                        val idx = templates.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) {
                            templates[idx] = updated
                            persist()
                        }
                    },
                )
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Как тестировать", fontWeight = FontWeight.Bold)
                    Text("1. Добавь маленькую картинку кнопки/иконки.")
                    Text("2. Поставь порог 75–85%.")
                    Text("3. Нажми «Запустить» и разреши захват экрана.")
                    Text("4. Открой приложение, где должна появиться эта картинка. Когда ClickFlow найдёт её, он тапнет один раз.")
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}

@Composable
private fun TemplateEditor(template: ImageClickTemplate, onUpdate: (ImageClickTemplate) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Настройка: ${template.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Порог похожести: ${(template.threshold * 100).roundToInt()}%")
            Slider(value = template.threshold, onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) }, valueRange = 0.5f..0.99f)
            Text("Точка тапа по X: ${(template.tapX * 100).roundToInt()}%")
            Slider(value = template.tapX, onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("Точка тапа по Y: ${(template.tapY * 100).roundToInt()}%")
            Slider(value = template.tapY, onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
        }
    }
}
