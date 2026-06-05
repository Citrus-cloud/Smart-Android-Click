package com.clickflow.android.imageclick

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import java.util.UUID
import kotlin.math.roundToInt

private const val PREFS_NAME = "clickflow_image_templates"
private const val KEY_TEMPLATES = "templates"

data class ImageClickTemplate(
    val id: String,
    val name: String,
    val filePath: String,
    val width: Int,
    val height: Int,
    val threshold: Float = 0.82f,
    val tapX: Float = 0.5f,
    val tapY: Float = 0.5f,
)

class ImageTemplateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ImageTemplateScreen(
                    context = this,
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
private fun ImageTemplateScreen(context: Context, onBack: () -> Unit) {
    val templates = remember { mutableStateListOf<ImageClickTemplate>().also { it.addAll(loadTemplates(context)) } }
    val selectedId = remember { mutableStateOf<String?>(templates.firstOrNull()?.id) }
    val selected = templates.firstOrNull { it.id == selectedId.value }

    fun persist() = saveTemplates(context, templates)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val template = copyUriAsTemplate(context, uri, templates.size + 1)
        if (template != null) {
            templates.add(template)
            selectedId.value = template.id
            persist()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Клик по картинке", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Добавь фото/иконку. Потом ClickFlow будет искать похожую картинку на экране и тапать в выбранную точку.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Text("+ Добавить фото / иконку")
            }

            if (templates.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Шаблонов пока нет", fontWeight = FontWeight.Bold)
                        Text("Нажми кнопку выше и выбери иконку или скриншот кнопки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            templates.forEachIndexed { index, template ->
                val active = selectedId.value == template.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. ${template.name}", fontWeight = FontWeight.Bold)
                            Text("${template.width}×${template.height}")
                        }
                        Text("Порог: ${(template.threshold * 100).roundToInt()}% · Точка тапа: ${(template.tapX * 100).roundToInt()}%, ${(template.tapY * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedId.value = template.id }, modifier = Modifier.weight(1f)) { Text("Настроить") }
                            OutlinedButton(
                                onClick = {
                                    File(template.filePath).delete()
                                    templates.removeAll { it.id == template.id }
                                    selectedId.value = templates.firstOrNull()?.id
                                    persist()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Удалить") }
                        }
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
                    Text("Что уже готово", fontWeight = FontWeight.Bold)
                    Text("• выбор фото/иконки из галереи")
                    Text("• сохранение шаблонов внутри приложения")
                    Text("• настройка порога похожести")
                    Text("• настройка точки тапа внутри шаблона")
                    Text("Следующий шаг: подключить захват экрана и поиск шаблона на скриншоте.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Slider(
                value = template.threshold,
                onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) },
                valueRange = 0.5f..0.99f,
            )

            Text("Точка тапа по X: ${(template.tapX * 100).roundToInt()}%")
            Slider(
                value = template.tapX,
                onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) },
                valueRange = 0f..1f,
            )

            Text("Точка тапа по Y: ${(template.tapY * 100).roundToInt()}%")
            Slider(
                value = template.tapY,
                onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) },
                valueRange = 0f..1f,
            )
        }
    }
}

private fun copyUriAsTemplate(context: Context, uri: Uri, number: Int): ImageClickTemplate? {
    return runCatching {
        val dir = File(context.filesDir, "image_templates").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val out = File(dir, "$id.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.absolutePath, opts)
        ImageClickTemplate(
            id = id,
            name = "Шаблон $number",
            filePath = out.absolutePath,
            width = opts.outWidth.coerceAtLeast(1),
            height = opts.outHeight.coerceAtLeast(1),
        )
    }.getOrNull()
}

private fun loadTemplates(context: Context): List<ImageClickTemplate> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TEMPLATES, null).orEmpty()
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { encoded ->
        val line = String(Base64.decode(encoded, Base64.NO_WRAP))
        val p = line.split("|")
        if (p.size != 8) return@mapNotNull null
        ImageClickTemplate(
            id = p[0],
            name = p[1],
            filePath = p[2],
            width = p[3].toIntOrNull() ?: 1,
            height = p[4].toIntOrNull() ?: 1,
            threshold = p[5].toFloatOrNull() ?: 0.82f,
            tapX = p[6].toFloatOrNull() ?: 0.5f,
            tapY = p[7].toFloatOrNull() ?: 0.5f,
        )
    }.filter { File(it.filePath).exists() }
}

private fun saveTemplates(context: Context, templates: List<ImageClickTemplate>) {
    val raw = templates.joinToString(";") { t ->
        val line = listOf(t.id, t.name, t.filePath, t.width, t.height, t.threshold, t.tapX, t.tapY).joinToString("|")
        Base64.encodeToString(line.toByteArray(), Base64.NO_WRAP)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TEMPLATES, raw).apply()
}
