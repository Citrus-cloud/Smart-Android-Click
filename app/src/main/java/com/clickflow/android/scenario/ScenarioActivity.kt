package com.clickflow.android.scenario

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clickflow.android.imageclick.ImageClickTemplate
import com.clickflow.android.imageclick.ImageClickTemplateStore
import com.clickflow.android.ui.ClickFlowTheme

class ScenarioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ScenarioRoot(
                    context = this,
                    onRun = { id ->
                        startService(Intent(this, ScenarioEngineService::class.java).apply {
                            action = ScenarioEngineService.ACTION_START
                            putExtra(ScenarioEngineService.EXTRA_SCENARIO_ID, id)
                        })
                        finish()
                    },
                    onStop = {
                        startService(Intent(this, ScenarioEngineService::class.java).apply { action = ScenarioEngineService.ACTION_STOP })
                    },
                    onBack = { finish() },
                )
            }
        }
    }
}

private val Compact = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
private val Tiny = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

@Composable
private fun ScenarioRoot(context: Context, onRun: (String) -> Unit, onStop: () -> Unit, onBack: () -> Unit) {
    var unlocked by remember { mutableStateOf(ScenarioPremium.isUnlocked(context)) }
    if (!unlocked) {
        PremiumLockScreen(onUnlock = { ScenarioPremium.startPurchaseFlow(context) { ok -> if (ok) unlocked = true } }, onBack = onBack)
        return
    }
    var editingId by remember { mutableStateOf<String?>(null) }
    val id = editingId
    if (id == null) {
        ScenarioListScreen(context, onRun = onRun, onStop = onStop, onEdit = { editingId = it }, onBack = onBack)
    } else {
        ScenarioEditorScreen(context, scenarioId = id, onRun = onRun, onStop = onStop, onBack = { editingId = null })
    }
}

@Composable
private fun PremiumLockScreen(onUnlock: () -> Unit, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumHeader()
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("\uD83D\uDD12 Премиум-функция", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Сценарии — цепочка действий в один тап.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    FeatureRow("\uD83C\uDFAC", "Шаги: метка, фото, текст, паузы")
                    FeatureRow("\uD83D\uDD01", "Повтор всего сценария: циклы или \u221e")
                    FeatureRow("\uD83E\uDDE9", "Гибкое «если не найдено»")
                }
            }
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("\u2726 Разблокировать Премиум") }
            Text("Сейчас доступ открывается для тестирования.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("Назад") }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Text(text, modifier = Modifier.weight(1f), fontSize = 13.sp)
    }
}

@Composable
private fun ScenarioListScreen(
    context: Context,
    onRun: (String) -> Unit,
    onStop: () -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var refresh by remember { mutableStateOf(0) }
    val scenarios = remember(refresh) { ScenarioLibraryStore.loadAll(context) }
    val activeId = remember(refresh) { ScenarioLibraryStore.getActiveId(context) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PremiumHeader()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val created = ScenarioLibraryStore.create(context, "Новый сценарий")
                    refresh++
                    onEdit(created.id)
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), contentPadding = Compact) { Text("+ Сценарий") }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\u25a0 Стоп") }
            }

            if (scenarios.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Создай свой первый сценарий и добавь шаги.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            scenarios.forEach { s ->
                val active = s.id == activeId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${s.icon}  ${s.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${s.steps.size} шагов \u00b7 " + if (s.loopInfinite) "\u221e" else "${s.loopCount}\u00d7", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            if (active) Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF2D7DF6)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text("Активный", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { ScenarioLibraryStore.setActive(context, s.id); refresh++; onRun(s.id) }, modifier = Modifier.weight(1f), enabled = s.steps.isNotEmpty(), contentPadding = Compact) { Text("\u25b6 Старт") }
                            OutlinedButton(onClick = { onEdit(s.id) }, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("Изменить") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { ScenarioLibraryStore.setActive(context, s.id); refresh++ }, modifier = Modifier.weight(1f), enabled = !active, contentPadding = Tiny) { Text(if (active) "\u2713 Активный" else "Активный", fontSize = 12.sp) }
                            OutlinedButton(onClick = { ScenarioLibraryStore.duplicate(context, s.id); refresh++ }, modifier = Modifier.weight(1f), contentPadding = Tiny) { Text("Дубль", fontSize = 12.sp) }
                            OutlinedButton(onClick = { ScenarioLibraryStore.delete(context, s.id); refresh++ }, modifier = Modifier.weight(1f), contentPadding = Tiny, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить", fontSize = 12.sp) }
                        }
                    }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("Назад") }
        }
    }
}

@Composable
private fun ScenarioEditorScreen(
    context: Context,
    scenarioId: String,
    onRun: (String) -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
) {
    var working by remember { mutableStateOf(ScenarioLibraryStore.get(context, scenarioId) ?: Scenario(id = scenarioId, name = "Сценарий")) }
    fun save(updated: Scenario) { working = updated; ScenarioLibraryStore.upsert(context, updated) }

    var templatesRefresh by remember { mutableStateOf(0) }
    val templateList = remember(templatesRefresh) { ImageClickTemplateStore.loadTemplates(context) }
    var pendingPhotoStepId by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val stepId = pendingPhotoStepId
        pendingPhotoStepId = null
        if (uri != null && stepId != null) {
            val tpl = ImageClickTemplateStore.copyUriAsTemplate(context, uri, templateList.size + 1)
            if (tpl != null) {
                val all = ImageClickTemplateStore.loadTemplates(context).toMutableList().also { it.add(tpl) }
                ImageClickTemplateStore.saveTemplates(context, all)
                templatesRefresh++
                save(working.copy(steps = working.steps.map { if (it.id == stepId) it.copy(photoTemplateId = tpl.id, photoPath = "") else it }))
            }
        }
    }

    fun addStep(type: StepType) {
        if (working.steps.size >= 50) return
        save(working.copy(steps = working.steps + ScenarioStep(id = ScenarioLibraryStore.newId(), type = type)))
    }
    fun updateStep(updated: ScenarioStep) { save(working.copy(steps = working.steps.map { if (it.id == updated.id) updated else it })) }
    fun deleteStep(id: String) { save(working.copy(steps = working.steps.filterNot { it.id == id })) }
    fun duplicateStep(id: String) {
        val idx = working.steps.indexOfFirst { it.id == id }
        if (idx < 0 || working.steps.size >= 50) return
        val copy = working.steps[idx].copy(id = ScenarioLibraryStore.newId())
        save(working.copy(steps = working.steps.toMutableList().also { it.add(idx + 1, copy) }))
    }
    fun move(id: String, delta: Int) {
        val idx = working.steps.indexOfFirst { it.id == id }
        val target = idx + delta
        if (idx < 0 || target < 0 || target >= working.steps.size) return
        val list = working.steps.toMutableList()
        val tmp = list[idx]; list[idx] = list[target]; list[target] = tmp
        save(working.copy(steps = list))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Сценарий", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("${working.steps.size}/50 шагов", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            TextField(value = working.name, onValueChange = { save(working.copy(name = it.take(60))) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название") })

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ScenarioLibraryStore.setActive(context, working.id); onRun(working.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = working.steps.isNotEmpty(), contentPadding = Compact) { Text("\u25b6 Старт") }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\u25a0 Стоп") }
            }

            LoopRow(working, ::save)

            if (working.steps.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) { Text("Пока нет шагов. Добавь ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                }
            }
            working.steps.forEachIndexed { i, step ->
                StepCard(
                    index = i,
                    total = working.steps.size,
                    step = step,
                    templates = templateList,
                    onUpdate = { updateStep(it) },
                    onPickPhoto = { id -> pendingPhotoStepId = id; picker.launch("image/*") },
                    onMoveUp = { move(step.id, -1) },
                    onMoveDown = { move(step.id, 1) },
                    onDuplicate = { duplicateStep(step.id) },
                    onDelete = { deleteStep(step.id) },
                )
            }

            Text("Добавить шаг", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addStep(StepType.MARKER) }, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\u25ce Метка") }
                OutlinedButton(onClick = { addStep(StepType.PHOTO) }, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\uD83D\uDDBC Фото") }
                OutlinedButton(onClick = { addStep(StepType.TEXT) }, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\uD83D\uDD24 Текст") }
                OutlinedButton(onClick = { addStep(StepType.WAIT) }, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("\u23f1 Пауза") }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("Назад") }
        }
    }
}

@Composable
private fun LoopRow(working: Scenario, save: (Scenario) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberRow("Повтор сценария", if (working.loopInfinite) "\u221e" else "${working.loopCount}\u00d7",
                onMinus = { save(working.copy(loopCount = (working.loopCount - 1).coerceAtLeast(1), loopInfinite = false)) },
                onPlus = { save(working.copy(loopCount = (working.loopCount + 1).coerceAtMost(100000), loopInfinite = false)) })
            ToggleRow("Бесконечно", working.loopInfinite) { save(working.copy(loopInfinite = it)) }
        }
    }
}

@Composable
private fun StepCard(
    index: Int,
    total: Int,
    step: ScenarioStep,
    templates: List<ImageClickTemplate>,
    onUpdate: (ScenarioStep) -> Unit,
    onPickPhoto: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(step.id) { mutableStateOf(false) }
    val typeLabel = when (step.type) {
        StepType.MARKER -> "\u25ce Метка"
        StepType.PHOTO -> "\uD83D\uDDBC Фото"
        StepType.TEXT -> "\uD83D\uDD24 Текст"
        StepType.WAIT -> "\u23f1 Пауза"
    }
    val summary = when (step.type) {
        StepType.MARKER -> "тап ${step.x},${step.y} \u00b7 ${step.repeat}\u00d7"
        StepType.PHOTO -> {
            val ti = templates.indexOfFirst { it.id == step.photoTemplateId }
            (if (ti >= 0) "фото №${ti + 1}" else "фото не выбрано") + " \u00b7 ${step.repeat}\u00d7"
        }
        StepType.TEXT -> (if (step.text.isBlank()) "текст не задан" else "\u00ab${step.text.take(18)}\u00bb") + " \u00b7 ${step.repeat}\u00d7"
        StepType.WAIT -> "${step.waitMs} мс"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. $typeLabel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (step.label.isNotBlank()) Text(step.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Text(summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onMoveUp, enabled = index > 0, contentPadding = Tiny) { Text("\u2191") }
                OutlinedButton(onClick = onMoveDown, enabled = index < total - 1, contentPadding = Tiny) { Text("\u2193") }
                OutlinedButton(onClick = { expanded = !expanded }, contentPadding = Tiny) { Text(if (expanded) "\u2715" else "\u270e") }
            }

            if (expanded) {
                TextField(value = step.label, onValueChange = { onUpdate(step.copy(label = it.take(60))) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название шага (необязательно)") })
                when (step.type) {
                    StepType.MARKER -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField(value = if (step.x == 0) "" else step.x.toString(), onValueChange = { onUpdate(step.copy(x = it.filter { c -> c.isDigit() }.take(6).toIntOrNull() ?: 0)) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("X") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            TextField(value = if (step.y == 0) "" else step.y.toString(), onValueChange = { onUpdate(step.copy(y = it.filter { c -> c.isDigit() }.take(6).toIntOrNull() ?: 0)) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Y") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        RepeatIntervalControls(step, onUpdate)
                    }
                    StepType.PHOTO -> {
                        if (templates.isNotEmpty()) {
                            templates.forEachIndexed { i, t ->
                                val active = t.id == step.photoTemplateId
                                if (active) Button(onClick = { onUpdate(step.copy(photoTemplateId = t.id, photoPath = "")) }, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("\u2713 Фото №${i + 1} \u00b7 ${t.width}\u00d7${t.height}") }
                                else OutlinedButton(onClick = { onUpdate(step.copy(photoTemplateId = t.id, photoPath = "")) }, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("Фото №${i + 1} \u00b7 ${t.width}\u00d7${t.height}") }
                            }
                        } else {
                            Text("Нет сохранённых фото.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { onPickPhoto(step.id) }, modifier = Modifier.fillMaxWidth(), contentPadding = Compact) { Text("+ Загрузить фото") }
                        RepeatIntervalControls(step, onUpdate)
                        NotFoundControls(step, onUpdate)
                    }
                    StepType.TEXT -> {
                        TextField(value = step.text, onValueChange = { onUpdate(step.copy(text = it.take(200))) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Текст для поиска") })
                        ToggleRow("Часть строки", step.textContains) { onUpdate(step.copy(textContains = it)) }
                        ToggleRow("Игнорировать регистр", step.textIgnoreCase) { onUpdate(step.copy(textIgnoreCase = it)) }
                        RepeatIntervalControls(step, onUpdate)
                        NotFoundControls(step, onUpdate)
                    }
                    StepType.WAIT -> {
                        NumberRow("Пауза", "${step.waitMs} мс",
                            onMinus = { onUpdate(step.copy(waitMs = (step.waitMs - 250).coerceAtLeast(50L))) },
                            onPlus = { onUpdate(step.copy(waitMs = (step.waitMs + 250).coerceAtMost(600000L))) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f), contentPadding = Compact) { Text("Дублировать") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), contentPadding = Compact, colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить") }
                }
            }
        }
    }
}

@Composable
private fun RepeatIntervalControls(step: ScenarioStep, onUpdate: (ScenarioStep) -> Unit) {
    NumberRow("Повторы", "${step.repeat}\u00d7",
        onMinus = { onUpdate(step.copy(repeat = (step.repeat - 1).coerceAtLeast(1))) },
        onPlus = { onUpdate(step.copy(repeat = (step.repeat + 1).coerceAtMost(100000))) })
    NumberRow("Интервал", "${step.intervalMs} мс",
        onMinus = { onUpdate(step.copy(intervalMs = (step.intervalMs - 100).coerceAtLeast(50L))) },
        onPlus = { onUpdate(step.copy(intervalMs = (step.intervalMs + 100).coerceAtMost(600000L))) })
}

@Composable
private fun NotFoundControls(step: ScenarioStep, onUpdate: (ScenarioStep) -> Unit) {
    Text("Если не найдено", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PolicyBtn("Пропуск", step.notFound == NotFoundPolicy.SKIP, Modifier.weight(1f)) { onUpdate(step.copy(notFound = NotFoundPolicy.SKIP)) }
        PolicyBtn("Ждать", step.notFound == NotFoundPolicy.WAIT_RETRY, Modifier.weight(1f)) { onUpdate(step.copy(notFound = NotFoundPolicy.WAIT_RETRY)) }
        PolicyBtn("Стоп", step.notFound == NotFoundPolicy.STOP, Modifier.weight(1f)) { onUpdate(step.copy(notFound = NotFoundPolicy.STOP)) }
    }
    if (step.notFound == NotFoundPolicy.WAIT_RETRY) {
        NumberRow("Ждать", "${step.notFoundWaitMs} мс",
            onMinus = { onUpdate(step.copy(notFoundWaitMs = (step.notFoundWaitMs - 500).coerceAtLeast(100L))) },
            onPlus = { onUpdate(step.copy(notFoundWaitMs = (step.notFoundWaitMs + 500).coerceAtMost(600000L))) })
        NumberRow("Попыток", "${step.notFoundRetries}",
            onMinus = { onUpdate(step.copy(notFoundRetries = (step.notFoundRetries - 1).coerceAtLeast(0))) },
            onPlus = { onUpdate(step.copy(notFoundRetries = (step.notFoundRetries + 1).coerceAtMost(1000))) })
    }
}

@Composable
private fun PolicyBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) { Text(label, fontSize = 12.sp) }
    else OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun NumberRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onMinus, contentPadding = Tiny) { Text("\u2212") }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedButton(onClick = onPlus, contentPadding = Tiny) { Text("+") }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PremiumHeader() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF8A63FF), Color(0xFF4F8DF7))))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x40FFFFFF)).padding(horizontal = 9.dp, vertical = 2.dp)) {
                Text("\u2726 PREMIUM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Text("Сценарии", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Цепочка шагов — метка, фото, текст, паузы — в один сценарий.", color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
    }
}
