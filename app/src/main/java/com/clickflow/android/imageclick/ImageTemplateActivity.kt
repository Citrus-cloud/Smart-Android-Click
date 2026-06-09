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
                    onRunMulti = { ids, sequential ->
                        if (ids.isNotEmpty()) {
                            startService(Intent(this, ImageClickService::class.java).apply {
                                action = ImageClickService.ACTION_START
                                putExtra(ImageClickService.EXTRA_TEMPLATE_IDS, ids.joinToString(","))
                                putExtra(ImageClickService.EXTRA_SEQUENTIAL, sequential)
                                // Looping only applies to the ordered (sequential) chain.
                                if (sequential) {
                                    putExtra(ImageClickService.EXTRA_LOOP, ImageClickTemplateStore.loadLoop(this@ImageTemplateActivity))
                                    putExtra(ImageClickService.EXTRA_LOOP_COUNT, ImageClickTemplateStore.loadLoopCount(this@ImageTemplateActivity))
                                }
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
private fun ImageTemplateScreen(context: Context, onRunMulti: (List<String>, Boolean) -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    val templates = remember { mutableStateListOf<ImageClickTemplate>().also { it.addAll(ImageClickTemplateStore.loadTemplates(context)) } }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var premium by remember { mutableStateOf(Premium.isPremiumUnlocked(context)) }
    var sequential by remember { mutableStateOf(ImageClickTemplateStore.loadSequential(context)) }
    var loop by remember { mutableStateOf(ImageClickTemplateStore.loadLoop(context)) }
    var loopCount by remember { mutableStateOf(ImageClickTemplateStore.loadLoopCount(context)) }
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
                Text("\u0424\u043e\u0442\u043e", fontWeight = FontWeight.Black, fontSize = 24.sp)
                Text("\u0432\u044b\u0431\u0440\u0430\u043d\u043e $runCount / $limit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+ \u0424\u043e\u0442\u043e") }
                Button(onClick = { onRunMulti(templates.filter { it.enabled }.map { it.id }.take(limit), sequential) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = runCount > 0, contentPadding = SmallPad) { Text("\u25b6 \u0421\u0442\u0430\u0440\u0442 ($runCount)") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onStop, contentPadding = SmallPad) { Text("\u25a0 \u0421\u0442\u043e\u043f") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Premium", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = premium, onCheckedChange = { premium = it; Premium.setPremiumUnlocked(context, it) })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (sequential) "\u0420\u0435\u0436\u0438\u043c: \u043f\u043e \u043e\u0447\u0435\u0440\u0435\u0434\u0438" else "\u0420\u0435\u0436\u0438\u043c: \u043e\u0434\u043d\u043e\u0432\u0440\u0435\u043c\u0435\u043d\u043d\u043e", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Switch(checked = sequential, onCheckedChange = { sequential = it; ImageClickTemplateStore.saveSequential(context, it) })
                    }
                    Text(
                        if (sequential)
                            "\u0422\u0430\u043f\u0430\u0435\u0442 \u0444\u043e\u0442\u043e \u043f\u043e \u043f\u043e\u0440\u044f\u0434\u043a\u0443: \u21161 \u2192 \u21162 \u2192 \u21163, \u043a\u0430\u0436\u0434\u043e\u0435 \u043f\u043e \u043e\u0434\u043d\u043e\u043c\u0443 \u0440\u0430\u0437\u0443. \u0414\u043b\u044f \u0446\u0435\u043f\u043e\u0447\u043a\u0438 \u00ab\u0438\u043a\u043e\u043d\u043a\u0430 \u2192 \u0437\u0430\u0439\u0442\u0438 \u2192 \u0432\u044b\u0431\u043e\u0440\u00bb."
                        else
                            "\u041d\u0430\u0436\u0438\u043c\u0430\u0435\u0442 \u0432\u0441\u0435 \u0432\u043a\u043b\u044e\u0447\u0451\u043d\u043d\u044b\u0435 \u0444\u043e\u0442\u043e \u043d\u0430 \u043e\u0434\u043d\u043e\u043c \u044d\u043a\u0440\u0430\u043d\u0435, \u043a\u0430\u0436\u0434\u043e\u0435 \u043f\u043e \u0441\u0432\u043e\u0438\u043c \u043f\u043e\u0432\u0442\u043e\u0440\u0430\u043c.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                    )
                    if (sequential) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("\u0417\u0430\u0446\u0438\u043a\u043b\u0438\u0442\u044c (\u043d\u0435 \u0432\u044b\u043a\u043b\u044e\u0447\u0430\u0442\u044c)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Switch(checked = loop, onCheckedChange = { loop = it; ImageClickTemplateStore.saveLoop(context, it) })
                        }
                        if (loop) {
                            Text(
                                "\u0426\u0438\u043a\u043b\u043e\u0432: " + if (loopCount <= 0) "\u221e (\u0431\u0435\u0437 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0438)" else "$loopCount",
                                fontSize = 12.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { val v = (loopCount - 1).coerceAtLeast(0); loopCount = v; ImageClickTemplateStore.saveLoopCount(context, v) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u22121") }
                                OutlinedButton(onClick = { val v = (loopCount + 1).coerceAtMost(100000); loopCount = v; ImageClickTemplateStore.saveLoopCount(context, v) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+1") }
                                OutlinedButton(onClick = { loopCount = 0; ImageClickTemplateStore.saveLoopCount(context, 0) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u221e") }
                            }
                            Text(
                                "\u041f\u043e\u0441\u043b\u0435 \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u0435\u0433\u043e \u0444\u043e\u0442\u043e \u043d\u0430\u0447\u043d\u0451\u0442 \u0441\u043d\u043e\u0432\u0430 \u0441 \u21161. \u041f\u0440\u0438 \u221e \u043d\u0435 \u0432\u044b\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u0438 \u0436\u0434\u0451\u0442 \u0444\u043e\u0442\u043e \u0431\u0435\u0441\u043a\u043e\u043d\u0435\u0447\u043d\u043e \u2014 \u043f\u043e\u043a\u0430 \u043d\u0435 \u043d\u0430\u0436\u043c\u0451\u0448\u044c \u00ab\u0421\u0442\u043e\u043f\u00bb.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                            )
                        }
                    }
                    Text(
                        "\u0411\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e ${Premium.FREE_TARGET_LIMIT}, Premium \u0434\u043e ${Premium.PREMIUM_TARGET_LIMIT}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    )
                }
            }

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
                                Text("\u0424\u043e\u0442\u043e \u2116${index + 1}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "${template.width}\u00d7${template.height} \u00b7 ${(template.threshold * 100).roundToInt()}% \u00b7 " + if (template.infinite) "\u221e" else "${template.repeatCount}\u0445",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                                )
                            }
                            Switch(checked = template.enabled, onCheckedChange = { setEnabled(template, it) }, enabled = canEnable)
                        }
                        if (!template.enabled && !canEnable) {
                            Text("\u041b\u0438\u043c\u0438\u0442 $limit \u2014 \u0432\u043a\u043b\u044e\u0447\u0438 Premium", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { expandedId = if (expanded) null else template.id }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text(if (expanded) "\u0421\u043a\u0440\u044b\u0442\u044c" else "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438") }
                            Button(onClick = { onRunMulti(listOf(template.id), false) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u0421\u0442\u0430\u0440\u0442") }
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
                    Column(Modifier.padding(14.dp)) { Text("\u0414\u043e\u0431\u0430\u0432\u044c \u0444\u043e\u0442\u043e \u043a\u043d\u043e\u043f\u043a\u0438 \u0438\u043b\u0438 \u0438\u043a\u043e\u043d\u043a\u0438 \u043a\u043d\u043e\u043f\u043a\u043e\u0439 \u00ab+ \u0424\u043e\u0442\u043e\u00bb.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), contentPadding = SmallPad) { Text("\u041d\u0430\u0437\u0430\u0434") }
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
            Text("\u041f\u043e\u0445\u043e\u0436\u0435\u0441\u0442\u044c: ${(template.threshold * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.threshold, onValueChange = { onUpdate(template.copy(threshold = it.coerceIn(0.5f, 0.99f))) }, valueRange = 0.5f..0.99f)
            Text("\u0421\u043e\u0432\u0435\u0442: ~80% \u043e\u0431\u044b\u0447\u043d\u043e \u043b\u0443\u0447\u0448\u0435 \u0432\u0441\u0435\u0433\u043e. 99% \u0447\u0430\u0441\u0442\u043e \u043d\u0435 \u043b\u043e\u0432\u0438\u0442.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text("\u0422\u0430\u043f X: ${(template.tapX * 100).roundToInt()}%  \u00b7  \u0422\u0430\u043f Y: ${(template.tapY * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.tapX, onValueChange = { onUpdate(template.copy(tapX = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Slider(value = template.tapY, onValueChange = { onUpdate(template.copy(tapY = it.coerceIn(0f, 1f))) }, valueRange = 0f..1f)
            Text("\u041c\u0430\u0441\u0448\u0442\u0430\u0431: ${(template.scaleMin * 100).roundToInt()}-${(template.scaleMax * 100).roundToInt()}%", fontSize = 13.sp)
            Slider(value = template.scaleMin, onValueChange = { onUpdate(template.copy(scaleMin = it.coerceIn(0.5f, template.scaleMax))) }, valueRange = 0.5f..2f)
            Slider(value = template.scaleMax, onValueChange = { onUpdate(template.copy(scaleMax = it.coerceIn(template.scaleMin, 2f))) }, valueRange = 0.5f..2f)
            OutlinedButton(onClick = { onUpdate(template.copy(scaleMin = 0.65f, scaleMax = 1.45f)) }, modifier = Modifier.fillMaxWidth(), contentPadding = SmallPad) { Text("\u0421\u0431\u0440\u043e\u0441 \u043c\u0430\u0441\u0448\u0442\u0430\u0431\u0430 65\u2013145%") }

            Text("\u041f\u043e\u0432\u0442\u043e\u0440\u044b: " + if (template.infinite) "\u221e" else "${template.repeatCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount - 5).coerceAtLeast(1), infinite = false)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u22125") }
                OutlinedButton(onClick = { onUpdate(template.copy(repeatCount = (template.repeatCount + 5).coerceAtMost(100000), infinite = false)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+5") }
                OutlinedButton(onClick = { onUpdate(template.copy(infinite = !template.infinite)) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text(if (template.infinite) "\u221e \u0412\u041a\u041b" else "\u221e") }
            }
            Text("\u0412 \u0440\u0435\u0436\u0438\u043c\u0435 \u00ab\u043f\u043e \u043e\u0447\u0435\u0440\u0435\u0434\u0438\u00bb \u043a\u0430\u0436\u0434\u043e\u0435 \u0444\u043e\u0442\u043e \u0442\u0430\u043f\u0430\u0435\u0442\u0441\u044f \u043e\u0434\u0438\u043d \u0440\u0430\u0437 (\u043f\u043e\u0432\u0442\u043e\u0440\u044b \u0438\u0433\u043d\u043e\u0440\u0438\u0440\u0443\u044e\u0442\u0441\u044f).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

            Text("\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b: ${template.intervalMs} \u043c\u0441", fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs - 200).coerceAtLeast(300))) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("\u2212200") }
                OutlinedButton(onClick = { onUpdate(template.copy(intervalMs = (template.intervalMs + 200).coerceAtMost(600000))) }, modifier = Modifier.weight(1f), contentPadding = SmallPad) { Text("+200") }
            }
            Text("\u041c\u0438\u043d\u0438\u043c\u0443\u043c ~1 \u0441\u0435\u043a: \u0441\u0438\u0441\u0442\u0435\u043c\u0430 \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0438\u0432\u0430\u0435\u0442 \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}
