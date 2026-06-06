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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PremiumHeader()
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔒 Премиум-функция", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Сценарии — мощный режим автоматизации. Соберите цепочку действий и запускайте её в один тап.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FeatureRow("🎬", "Цепочки шагов: метка, фото, текст, паузы")
                    FeatureRow("🔁", "Повтор всего сценария: циклы или ∞")
                    FeatureRow("🧩", "Гибкое «если не найдено» для каждого шага")
                    FeatureRow("📚", "Своя библиотека сценариев: дублируйте и меняйте")
                }
            }
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("✦ Разблокировать Премиум") }
            Text("Подписка появится позже. Сейчас доступ открывается для тестирования.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}

@Composable
private fun FeatureRow(icon: String, text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Text(text, modifier = Modifier.weight(1f))
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumHeader()
            Button(onClick = {
                val created = ScenarioLibraryStore.create(context, "Новый сценарий")
                refresh++
                onEdit(created.id)
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text("+ Новый сценарий") }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("■ Остановить") }

            if (scenarios.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Создай свой первый сценарий и добавь в него шаги: метка, фото, текст, пауза.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            scenarios.forEach { s ->
                val active = s.id == activeId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${s.icon}  ${s.name}", fontWeight = FontWeight.Black, fontSize = 17.sp)
                            if (active) Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF2D7DF6)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text("Активный", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("${s.steps.size} шагов · " + if (s.loopInfinite) "∞" else "${s.loopCount}×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { ScenarioLibraryStore.setActive(context, s.id); refresh++; onRun(s.id) }, modifier = Modifier.weight(1f), enabled = s.steps.isNotEmpty()) { Text("▶ Старт") }
                            OutlinedButton(onClick = { onEdit(s.id) }, modifier = Modifier.weight(1f)) { Text("Изменить") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { ScenarioLibraryStore.setActive(context, s.id); refresh++ }, modifier = Modifier.weight(1f), enabled = !active) { Text(if (active) "✓ Активный" else "Активный") }
                            OutlinedButton(onClick = { ScenarioLibraryStore.duplicate(context, s.id); refresh++ }, modifier = Modifier.weight(1f)) { Text("Дублировать") }
                        }
                        OutlinedButton(onClick = { ScenarioLibraryStore.delete(context, s.id); refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Удалить") }
                    }
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumHeader()
            TextField(value = working.name, onValueChange = { save(working.copy(name = it.take(60))) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название сценария") })

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { ScenarioLibraryStore.setActive(context, working.id); onRun(working.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), enabled = working.steps.isNotEmpty()) { Text("▶ Старт") }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text("■ Стоп") }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Повтор всего сценария", fontWeight = FontWeight.Bold)
                    NumberRow("Циклов", if (working.loopInfinite) "∞" else "${working.loopCount}×",
                        onMinus = { save(working.copy(loopCount = (working.loopCount - 1).coerceAtLeast(1), loopInfinite = false)) },
                        onPlus = { save(working.copy(loopCount = (working.loopCount + 1).coerceAtMost(100000), loopInfinite = false)) })
                    ToggleRow("Бесконечно", working.loopInfinite) { save(working.copy(loopInfinite = it)) }
                }
            }

            Text("Шаги (${working.steps.size}/50)", fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (working.steps.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) { Text("Пока нет шагов. Добавьте первый шаг ниже.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

            Text("Добавить шаг", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addStep(StepType.MARKER) }, modifier = Modifier.weight(1f)) { Text("◎ Метка") }
                OutlinedButton(onClick = { addStep(StepType.PHOTO) }, modifier = Modifier.weight(1f)) { Text("🖼 Фото") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { addStep(StepType.TEXT) }, modifier = Modifier.weight(1f)) { Text("🔤 Текст") }
                OutlinedButton(onClick = { addStep(StepType.WAIT) }, modifier = Modifier.weight(1f)) { Text("⏱ Пауза") }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
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
    val typeLabel = when (step.type) {
        StepType.MARKER -> "◎ Метка"
        StepType.PHOTO -> "🖼 Фото"
        StepType.TEXT -> "🔤 Текст"
        StepType.WAIT -> "⏱ Пауза"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}. $typeLabel", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onMoveUp, enabled = index > 0, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("↑") }
                    OutlinedButton(onClick = onMoveDown, enabled = index < total - 1, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("↓") }
                }
            }
            TextField(value = step.label, onValueChange = { onUpdate(step.copy(label = it.take(60))) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Название шага (необязательно)") })

            when (step.type) {
                StepType.MARKER -> {
                    Text("Координаты тапа", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextField(value = if (step.x == 0) "" else step.x.toString(), onValueChange = { onUpdate(step.copy(x = it.filter { c -> c.isDigit() }.take(6).toIntOrNull() ?: 0)) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("X") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        TextField(value = if (step.y == 0) "" else step.y.toString(), onValueChange = { onUpdate(step.copy(y = it.filter { c -> c.isDigit() }.take(6).toIntOrNull() ?: 0)) }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Y") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Text("Координаты можно подобрать в режиме метки.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    RepeatIntervalControls(step, onUpdate)
                }
                StepType.PHOTO -> {
                    if (templates.isNotEmpty()) {
                        Text("Выбери фото", fontWeight = FontWeight.Bold)
                        templates.forEachIndexed { i, t ->
                            val active = t.id == step.photoTemplateId
                            if (active) Button(onClick = { onUpdate(step.copy(photoTemplateId = t.id, photoPath = "")) }, modifier = Modifier.fillMaxWidth()) { Text("✓ Фото №${i + 1} · ${t.width}×${t.height}") }
                            else OutlinedButton(onClick = { onUpdate(step.copy(photoTemplateId = t.id, photoPath = "")) }, modifier = Modifier.fillMaxWidth()) { Text("Фото №${i + 1} · ${t.width}×${t.height}") }
                        }
                    } else {
                        Text("Нет сохранённых фото — загрузи новое.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { onPickPhoto(step.id) }, modifier = Modifier.fillMaxWidth()) { Text("+ Загрузить новое фото") }
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
                OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Text("Дублировать") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Удалить") }
            }
        }
    }
}

@Composable
private fun RepeatIntervalControls(step: ScenarioStep, onUpdate: (ScenarioStep) -> Unit) {
    NumberRow("Повторы", "${step.repeat}×",
        onMinus = { onUpdate(step.copy(repeat = (step.repeat - 1).coerceAtLeast(1))) },
        onPlus = { onUpdate(step.copy(repeat = (step.repeat + 1).coerceAtMost(100000))) })
    NumberRow("Интервал", "${step.intervalMs} мс",
        onMinus = { onUpdate(step.copy(intervalMs = (step.intervalMs - 100).coerceAtLeast(50L))) },
        onPlus = { onUpdate(step.copy(intervalMs = (step.intervalMs + 100).coerceAtMost(600000L))) })
}

@Composable
private fun NotFoundControls(step: ScenarioStep, onUpdate: (ScenarioStep) -> Unit) {
    Text("Если не найдено", fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PolicyBtn("Пропустить", step.notFound == NotFoundPolicy.SKIP, Modifier.weight(1f)) { onUpdate(step.copy(notFound = NotFoundPolicy.SKIP)) }
        PolicyBtn("Подождать", step.notFound == NotFoundPolicy.WAIT_RETRY, Modifier.weight(1f)) { onUpdate(step.copy(notFound = NotFoundPolicy.WAIT_RETRY)) }
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
        Text(label, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("−") }
            Text(value, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("+") }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PremiumHeader() {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF2D7DF6))))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text("✦ PREMIUM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            Text("Сценарии", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Text("Собери цепочку шагов — метка, фото, текст и паузы — в один автоматический сценарий.", color = Color(0xCCFFFFFF), fontSize = 13.sp)
        }
    }
}
