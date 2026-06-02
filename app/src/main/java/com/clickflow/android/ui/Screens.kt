package com.clickflow.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.clickflow.android.R
import com.clickflow.android.core.AppInfo
import kotlin.math.roundToInt
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.Screen
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioAction
import com.clickflow.android.scenarios.ScenarioActionType

@Composable
fun ClickFlowApp(vm: ClickFlowViewModel) {
val screen by vm.screen.collectAsState()
Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    when (screen) {
        Screen.HOME -> SimpleClickerScreen(vm)
        Screen.ADVANCED -> AdvancedScreen(vm)
        Screen.ABOUT -> AboutScreen(vm)
        Screen.SCENARIOS -> ScenariosScreen(vm)
        Screen.SCENARIO_DETAIL -> ScenarioDetailScreen(vm)
        Screen.SCENARIO_FORM -> ScenarioFormScreen(vm)
        Screen.ACTION_FORM -> ActionFormScreen(vm)
        Screen.PROFILES -> ProfilesScreen(vm)
        Screen.PROFILE_FORM -> ProfileFormScreen(vm)
        Screen.BACKUP -> BackupScreen(vm)
        Screen.SAFETY -> SafetyCenterScreen(vm)
        Screen.DIAGNOSTICS -> DiagnosticsScreen(vm)
        Screen.AUDIT_LOG -> AuditLogScreen(vm)
        Screen.PERMISSIONS -> PermissionsScreen(vm)
    }
}
}

@Composable
private fun ScreenScaffold(content: @Composable ColumnScope.() -> Unit) {
Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content,
)
}

@Composable
private fun NavButton(text: String, onClick: () -> Unit) {
OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun ErrText(resId: Int) {
Text(stringResource(resId), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** Maps a one-shot UiMessage key to a localized line and renders it if present. */
@Composable
private fun MessageLine(vm: ClickFlowViewModel) {
val msg by vm.message.collectAsState()
val key = msg?.key ?: return
val resId = when (key) {
    "profile_saved" -> R.string.profile_saved
    "profile_deleted" -> R.string.profile_deleted
    "cannot_delete_active" -> R.string.profile_cannot_delete_active
    "cannot_delete_last" -> R.string.profile_cannot_delete_last
    "cannot_delete_with_scenarios" -> R.string.profile_cannot_delete_with_scenarios
    "scenario_saved" -> R.string.scenario_saved
    "scenario_deleted" -> R.string.scenario_deleted
    "no_active_scenario" -> R.string.active_scenario_none
    "audit_log_exported" -> R.string.audit_log_exported
    "audit_log_export_failed" -> R.string.audit_log_export_failed
    "active_profile" -> R.string.active_profile
    "active_scenario" -> R.string.active_scenario
    "backup_exported" -> R.string.backup_exported
    "backup_export_failed" -> R.string.backup_export_failed
    "backup_import_completed" -> R.string.backup_import_completed
    "backup_import_failed" -> R.string.backup_import_failed
    "replace_all_requires_confirmation" -> R.string.replace_all_requires_confirmation
    else -> null
} ?: return
Card(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(resId), fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { vm.consumeMessage() }) { Text("✕") }
    }
}
}

private fun actionSummary(a: ScenarioAction): String = when (a.type) {
ScenarioActionType.SIMULATED_TAP -> "tap (${a.x ?: 0}, ${a.y ?: 0})${a.label?.let { " — $it" } ?: ""}"
ScenarioActionType.WAIT -> "wait ${a.durationMs ?: 0} ms"
ScenarioActionType.NOTE -> "note: ${a.message.orEmpty()}"
}

// ---- Simple Clicker (home) ------------------------------------------------

@Composable
private fun SimpleClickerScreen(vm: ClickFlowViewModel) {
val status by vm.status.collectAsState()
val progress by vm.progress.collectAsState()
val interval by vm.quickIntervalMs.collectAsState()
val count by vm.quickRepeatCount.collectAsState()

ScreenScaffold {
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.status_badge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.drag_to_choose_click_point), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    MessageLine(vm)

    TargetArea(vm)

    // Quick settings
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Stepper(
                label = stringResource(R.string.interval),
                value = "$interval ms",
                onMinus = { vm.adjustQuickInterval(-100) },
                onPlus = { vm.adjustQuickInterval(100) },
            )
            Stepper(
                label = stringResource(R.string.count),
                value = "$count",
                onMinus = { vm.adjustQuickCount(-1) },
                onPlus = { vm.adjustQuickCount(1) },
            )
        }
    }

    // Primary action
    Button(
        onClick = { vm.startQuickSimulation() },
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp),
    ) { Text(stringResource(R.string.start_clicker), style = MaterialTheme.typography.titleMedium) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { vm.stopSimulation() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.stop_clicker)) }
        OutlinedButton(
            onClick = { vm.emergencyStop() },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.btn_emergency_stop)) }
    }

    // Status
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${stringResource(R.string.current_status, status.name.lowercase())}")
            if (progress.totalSteps > 0) {
                @Suppress("DEPRECATION")
                LinearProgressIndicator(progress = progress.percent / 100f, modifier = Modifier.fillMaxWidth())
                Text("${stringResource(R.string.current_tap)}: ${progress.currentStep}/${progress.totalSteps} (${progress.percent}%)", style = MaterialTheme.typography.bodySmall)
                progress.lastLog?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    OutlinedButton(onClick = { vm.navigateTo(Screen.ADVANCED) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.open_advanced))
    }
}
}

/** Draggable in-app circular click marker. NOT a system overlay. */
@Composable
private fun TargetArea(vm: ClickFlowViewModel) {
val fx by vm.markerX.collectAsState()
val fy by vm.markerY.collectAsState()
var areaSize by remember { mutableStateOf(IntSize.Zero) }
val markerRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 28.dp.toPx() }

Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
    Column(Modifier.padding(12.dp)) {
        Text(stringResource(R.string.tap_target), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { areaSize = it },
        ) {
            if (areaSize != IntSize.Zero) {
                val cx = (fx * areaSize.width)
                val cy = (fy * areaSize.height)
                Box(
                    modifier = Modifier
                        .offset { IntOffset((cx - markerRadiusPx).roundToInt(), (cy - markerRadiusPx).roundToInt()) }
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .pointerInput(areaSize) {
                            detectDragGestures(onDragEnd = { vm.commitMarker() }) { change, drag ->
                                change.consume()
                                val w = areaSize.width.toFloat().coerceAtLeast(1f)
                                val h = areaSize.height.toFloat().coerceAtLeast(1f)
                                vm.setMarker(vm.markerX.value + drag.x / w, vm.markerY.value + drag.y / h)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${stringResource(R.string.marker_position)}: ${(fx * 1000).roundToInt()}, ${(fy * 1000).roundToInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { vm.centerMarker() }) { Text(stringResource(R.string.center_marker)) }
        }
    }
}
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column { Text(label, fontWeight = FontWeight.SemiBold); Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onMinus) { Text("−") }
        OutlinedButton(onClick = onPlus) { Text("+") }
    }
}
}

// ---- Advanced menu --------------------------------------------------------

@Composable
private fun AdvancedScreen(vm: ClickFlowViewModel) {
ScreenScaffold {
    Text(stringResource(R.string.advanced_menu), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    MessageLine(vm)
    NavButton(stringResource(R.string.scenarios)) { vm.navigateTo(Screen.SCENARIOS) }
    NavButton(stringResource(R.string.profiles)) { vm.navigateTo(Screen.PROFILES) }
    NavButton(stringResource(R.string.audit_log)) { vm.navigateTo(Screen.AUDIT_LOG) }
    NavButton(stringResource(R.string.backup)) { vm.openBackup() }
    NavButton(stringResource(R.string.btn_safety_center)) { vm.navigateTo(Screen.SAFETY) }
    NavButton(stringResource(R.string.btn_permissions)) { vm.navigateTo(Screen.PERMISSIONS) }
    NavButton(stringResource(R.string.btn_diagnostics)) { vm.navigateTo(Screen.DIAGNOSTICS) }
    NavButton(stringResource(R.string.about)) { vm.navigateTo(Screen.ABOUT) }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.real_taps_disclaimer), fontWeight = FontWeight.SemiBold)
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.HOME) }
}
}

@Composable
private fun AboutScreen(vm: ClickFlowViewModel) {
ScreenScaffold {
    Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(AppInfo.APP_NAME, fontWeight = FontWeight.SemiBold)
            Text("version: ${AppInfo.VERSION_NAME}")
            Text(AppInfo.STEP, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.simulation_only_marker), style = MaterialTheme.typography.bodySmall)
        }
    }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Scenarios list (filtered by active profile) --------------------------

@Composable
private fun ScenariosScreen(vm: ClickFlowViewModel) {
val scenarios by vm.scenarios.collectAsState()
val profiles by vm.profiles.collectAsState()
val activeProfile = profiles.firstOrNull { it.isActive } ?: profiles.firstOrNull()
val inProfile = scenarios.filter { it.profileId == activeProfile?.id }

ScreenScaffold {
    Text(stringResource(R.string.scenarios), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("${stringResource(R.string.scenarios_for_profile)}: ${activeProfile?.name ?: "—"}", style = MaterialTheme.typography.bodySmall)
    MessageLine(vm)

    if (inProfile.isEmpty()) {
        Text(stringResource(R.string.no_scenarios))
        Button(onClick = { vm.openCreateScenario() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_scenario)) }
    } else {
        inProfile.forEach { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        if (s.isActive) Text(stringResource(R.string.active_badge), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.scenario_meta_summary, s.actions.size, s.settings.repeatCount, s.settings.intervalMs), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.openScenarioDetail(s.id) }) { Text(stringResource(R.string.open_scenario)) }
                        TextButton(onClick = { vm.selectScenario(s.id) }) { Text(stringResource(R.string.select_scenario)) }
                        TextButton(onClick = { vm.deleteScenario(s.id) }) { Text(stringResource(R.string.delete_scenario)) }
                    }
                    Button(onClick = { vm.runScenarioSimulation(s.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.run_simulation)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.openCreateScenario() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_scenario)) }
    }
    NavButton(stringResource(R.string.reset_scenarios)) { vm.resetScenarios() }
    Text(stringResource(R.string.real_taps_disclaimer), fontWeight = FontWeight.SemiBold)
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Scenario detail (multi-step editor) ----------------------------------

@Composable
private fun ScenarioDetailScreen(vm: ClickFlowViewModel) {
val scenarios by vm.scenarios.collectAsState()
val selectedId by vm.selectedScenarioId.collectAsState()
val s: Scenario? = scenarios.firstOrNull { it.id == selectedId }

ScreenScaffold {
    if (s == null) {
        Text(stringResource(R.string.no_scenarios))
        NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.SCENARIOS) }
        return@ScreenScaffold
    }
    Text(s.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.scenario_meta_summary, s.actions.size, s.settings.repeatCount, s.settings.intervalMs), style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { vm.openEditScenario(s.id) }) { Text(stringResource(R.string.edit_scenario)) }
        TextButton(onClick = { vm.selectScenario(s.id) }) { Text(stringResource(R.string.select_scenario)) }
    }

    Text(stringResource(R.string.actions), fontWeight = FontWeight.Bold)
    s.actions.forEachIndexed { index, a ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${index + 1}. ${a.type.name.lowercase()}", fontWeight = FontWeight.SemiBold)
                Text(actionSummary(a), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { vm.openEditAction(s.id, a.id) }) { Text(stringResource(R.string.edit_action)) }
                    TextButton(onClick = { vm.deleteAction(s.id, a.id) }) { Text(stringResource(R.string.delete_action)) }
                    TextButton(onClick = { vm.moveActionUp(s.id, a.id) }) { Text(stringResource(R.string.move_up)) }
                    TextButton(onClick = { vm.moveActionDown(s.id, a.id) }) { Text(stringResource(R.string.move_down)) }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    NavButton(stringResource(R.string.add_tap_action)) { vm.openAddAction(s.id, ScenarioActionType.SIMULATED_TAP) }
    NavButton(stringResource(R.string.add_wait_action)) { vm.openAddAction(s.id, ScenarioActionType.WAIT) }
    NavButton(stringResource(R.string.add_note_action)) { vm.openAddAction(s.id, ScenarioActionType.NOTE) }
    Button(onClick = { vm.runScenarioSimulation(s.id) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.run_simulation)) }
    Text(stringResource(R.string.simulation_only_actions), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.SCENARIOS) }
}
}

// ---- Scenario metadata form -----------------------------------------------

@Composable
private fun ScenarioFormScreen(vm: ClickFlowViewModel) {
val form by vm.scenarioForm.collectAsState()
val errors by vm.scenarioErrors.collectAsState()
ScreenScaffold {
    Text(stringResource(if (form.isEditing) R.string.edit_scenario else R.string.create_scenario), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

    OutlinedTextField(value = form.name, onValueChange = { v -> vm.updateScenarioForm { it.copy(name = v) } }, label = { Text(stringResource(R.string.scenario_name)) }, isError = errors.name != null, singleLine = true, modifier = Modifier.fillMaxWidth())
    errors.name?.let { ErrText(R.string.validation_name_required) }

    OutlinedTextField(value = form.repeatCount, onValueChange = { v -> vm.updateScenarioForm { it.copy(repeatCount = v) } }, label = { Text(stringResource(R.string.repeat_count)) }, isError = errors.repeatCount != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    errors.repeatCount?.let { ErrText(R.string.validation_repeat_invalid) }

    OutlinedTextField(value = form.intervalMs, onValueChange = { v -> vm.updateScenarioForm { it.copy(intervalMs = v) } }, label = { Text(stringResource(R.string.interval_ms)) }, isError = errors.intervalMs != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    errors.intervalMs?.let { ErrText(R.string.validation_interval_invalid) }
    errors.actions?.let { ErrText(R.string.validation_actions_required) }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.saveScenarioForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
        OutlinedButton(onClick = { vm.cancelScenarioForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
    }
    Text(stringResource(R.string.simulation_only_scenario), style = MaterialTheme.typography.bodySmall)
}
}

// ---- Action form ----------------------------------------------------------

@Composable
private fun ActionFormScreen(vm: ClickFlowViewModel) {
val form by vm.actionForm.collectAsState()
val errors by vm.actionErrors.collectAsState()
val title = when (form.type) {
    ScenarioActionType.SIMULATED_TAP -> R.string.simulated_tap_action
    ScenarioActionType.WAIT -> R.string.wait_action
    ScenarioActionType.NOTE -> R.string.note_action
}
ScreenScaffold {
    Text(stringResource(R.string.add_action), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("${stringResource(R.string.action_type)}: ${stringResource(title)}", fontWeight = FontWeight.SemiBold)

    when (form.type) {
        ScenarioActionType.SIMULATED_TAP -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = form.x, onValueChange = { v -> vm.updateActionForm { it.copy(x = v) } }, label = { Text("X") }, isError = errors.coordinates != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(value = form.y, onValueChange = { v -> vm.updateActionForm { it.copy(y = v) } }, label = { Text("Y") }, isError = errors.coordinates != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            errors.coordinates?.let { ErrText(R.string.validation_coordinate_invalid) }
            OutlinedTextField(value = form.label, onValueChange = { v -> vm.updateActionForm { it.copy(label = v) } }, label = { Text(stringResource(R.string.action_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        ScenarioActionType.WAIT -> {
            OutlinedTextField(value = form.durationMs, onValueChange = { v -> vm.updateActionForm { it.copy(durationMs = v) } }, label = { Text(stringResource(R.string.duration_ms)) }, isError = errors.duration != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            errors.duration?.let { ErrText(R.string.validation_duration_invalid) }
        }
        ScenarioActionType.NOTE -> {
            OutlinedTextField(value = form.message, onValueChange = { v -> vm.updateActionForm { it.copy(message = v) } }, label = { Text(stringResource(R.string.note_message)) }, isError = errors.message != null, modifier = Modifier.fillMaxWidth())
            errors.message?.let { ErrText(R.string.validation_message_required) }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.saveActionForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
        OutlinedButton(onClick = { vm.cancelActionForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
    }
    Text(stringResource(R.string.real_tap_still_disabled), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
}
}

// ---- Profiles -------------------------------------------------------------

@Composable
private fun ProfilesScreen(vm: ClickFlowViewModel) {
val profiles by vm.profiles.collectAsState()
val scenarios by vm.scenarios.collectAsState()
ScreenScaffold {
    Text(stringResource(R.string.profiles), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.profiles_are_local), style = MaterialTheme.typography.bodySmall)
    MessageLine(vm)

    if (profiles.isEmpty()) {
        Text(stringResource(R.string.no_profiles))
        Button(onClick = { vm.resetProfiles() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.default_profile)) }
    } else {
        profiles.forEach { p ->
            val count = scenarios.count { it.profileId == p.id }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        if (p.isActive) Text(stringResource(R.string.active_badge), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    if (p.description.isNotBlank()) Text(p.description, style = MaterialTheme.typography.bodySmall)
                    Text("${stringResource(R.string.scenarios)}: $count", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.selectProfile(p.id) }) { Text(stringResource(R.string.select_profile)) }
                        TextButton(onClick = { vm.openEditProfile(p.id) }) { Text(stringResource(R.string.edit_profile)) }
                        TextButton(onClick = { vm.deleteProfile(p.id) }) { Text(stringResource(R.string.delete_profile)) }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { vm.openCreateProfile() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_profile)) }
    NavButton(stringResource(R.string.reset_profiles)) { vm.resetProfiles() }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

@Composable
private fun ProfileFormScreen(vm: ClickFlowViewModel) {
val form by vm.profileForm.collectAsState()
val errors by vm.profileErrors.collectAsState()
ScreenScaffold {
    Text(stringResource(if (form.isEditing) R.string.edit_profile else R.string.create_profile), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

    OutlinedTextField(value = form.name, onValueChange = { v -> vm.updateProfileForm { it.copy(name = v) } }, label = { Text(stringResource(R.string.profile_name)) }, isError = errors.name != null, singleLine = true, modifier = Modifier.fillMaxWidth())
    errors.name?.let { ErrText(if (it == "name_too_long") R.string.profile_name_too_long else R.string.profile_name_required) }

    OutlinedTextField(value = form.description, onValueChange = { v -> vm.updateProfileForm { it.copy(description = v) } }, label = { Text(stringResource(R.string.profile_description)) }, isError = errors.description != null, modifier = Modifier.fillMaxWidth())
    errors.description?.let { ErrText(R.string.profile_description_too_long) }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.saveProfileForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
        OutlinedButton(onClick = { vm.cancelProfileForm() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
    }
}
}

// ---- Audit log ------------------------------------------------------------

@Composable
private fun AuditLogScreen(vm: ClickFlowViewModel) {
val events by vm.auditEvents.collectAsState()
val summary = vm.auditSummary()
ScreenScaffold {
    Text(stringResource(R.string.audit_log), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    MessageLine(vm)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.audit_summary), fontWeight = FontWeight.SemiBold)
            Text("total=${summary.totalEvents} info=${summary.infoCount} warning=${summary.warningCount} error=${summary.errorCount} safety=${summary.safetyCount}", style = MaterialTheme.typography.bodySmall)
            Text("${stringResource(R.string.audit_storage_ready)}: ${summary.storageReady}", style = MaterialTheme.typography.bodySmall)
            Text("${stringResource(R.string.corrupted_audit_recovered)}: ${summary.corruptedAuditRecovered}", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (events.isEmpty()) {
        Text("—")
    } else {
        events.forEach { e ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${e.severity} · ${e.type}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(e.message, style = MaterialTheme.typography.bodyMedium)
                    Text("ts=${e.timestamp}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = { vm.shareAuditLog() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.share_audit_log)) }
    OutlinedButton(onClick = { vm.clearAuditLog() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear_audit_log)) }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Backup (export / import) ---------------------------------------------

@Composable
private fun BackupScreen(vm: ClickFlowViewModel) {
val profiles by vm.profiles.collectAsState()
val scenarios by vm.scenarios.collectAsState()
val json by vm.backupJsonText.collectAsState()
val preview by vm.backupPreview.collectAsState()
val result by vm.backupImportResult.collectAsState()
val replaceConfirmed by vm.replaceAllConfirmed.collectAsState()

ScreenScaffold {
    Text(stringResource(R.string.backup), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    MessageLine(vm)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${stringResource(R.string.backup_profiles_count)}: ${profiles.size}")
            Text("${stringResource(R.string.backup_scenarios_count)}: ${scenarios.size}")
            Text(stringResource(R.string.backup_simulation_only), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.backup_does_not_include_audit_log), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.no_permissions_required), style = MaterialTheme.typography.bodySmall)
        }
    }

    Button(onClick = { vm.shareBackupJson() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_backup)) }

    HorizontalDivider()
    Text(stringResource(R.string.import_backup), fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = json,
        onValueChange = { vm.updateBackupJsonText(it) },
        label = { Text(stringResource(R.string.paste_backup_json)) },
        modifier = Modifier.fillMaxWidth().height(160.dp),
    )
    OutlinedButton(onClick = { vm.validateBackupJson() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.validate_backup)) }

    preview?.let { p ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(if (p.valid) R.string.backup_valid else R.string.backup_invalid), fontWeight = FontWeight.SemiBold, color = if (p.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text("${stringResource(R.string.backup_profiles_count)}: ${p.profilesCount}")
                Text("${stringResource(R.string.backup_scenarios_count)}: ${p.scenariosCount}")
                Text("${stringResource(R.string.invalid_items_count)}: ${p.invalidItemsCount}")
                if (p.warnings.isNotEmpty()) Text("${stringResource(R.string.backup_warnings)}: ${p.warnings.size}", style = MaterialTheme.typography.bodySmall)
                p.appVersion?.let { Text("appVersion: $it", style = MaterialTheme.typography.bodySmall) }
                p.createdAt?.let { Text("createdAt: $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    val canImport = preview?.valid == true
    Text(stringResource(R.string.import_strategy), fontWeight = FontWeight.SemiBold)
    Button(onClick = { vm.importBackup(com.clickflow.android.backup.ImportStrategy.MERGE_RENAME_CONFLICTS) }, enabled = canImport, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.merge_rename_conflicts))
    }
    OutlinedButton(onClick = { vm.setReplaceAllConfirmed(!replaceConfirmed) }, modifier = Modifier.fillMaxWidth()) {
        Text((if (replaceConfirmed) "☑ " else "☐ ") + stringResource(R.string.replace_all_requires_confirmation))
    }
    Button(onClick = { vm.importBackup(com.clickflow.android.backup.ImportStrategy.REPLACE_ALL_REQUIRE_CONFIRMATION) }, enabled = canImport && replaceConfirmed, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.replace_all_requires_confirmation))
    }

    result?.let { r ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.import_result), fontWeight = FontWeight.SemiBold)
                Text("${stringResource(R.string.imported_profiles)}: ${r.importedProfiles}")
                Text("${stringResource(R.string.imported_scenarios)}: ${r.importedScenarios}")
                Text("${stringResource(R.string.skipped_items)}: ${r.skippedItems}")
                if (r.warnings.isNotEmpty()) Text("${stringResource(R.string.backup_warnings)}: ${r.warnings.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    OutlinedButton(onClick = { vm.clearBackupImportState() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear_import)) }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Safety Center --------------------------------------------------------

@Composable
private fun SafetyCenterScreen(vm: ClickFlowViewModel) {
ScreenScaffold {
    Text(stringResource(R.string.btn_safety_center), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    vm.currentSafetyCenter().items().forEach { item ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label, fontWeight = FontWeight.SemiBold)
                Text(item.status)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.real_taps_disabled), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    Text(stringResource(R.string.blocked_reasons_title), fontWeight = FontWeight.Bold)
    vm.blockedReasons().forEach { reason -> Text("• $reason") }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Diagnostics ----------------------------------------------------------

@Composable
private fun DiagnosticsScreen(vm: ClickFlowViewModel) {
val d = vm.diagnostics()
ScreenScaffold {
    Text(stringResource(R.string.btn_diagnostics), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("app version: ${d.appVersion}")
            Text("profilesCount: ${d.profilesCount}")
            Text("activeProfileName: ${d.activeProfileName ?: "—"}")
            Text("scenariosCount: ${d.scenariosCount}")
            Text("activeScenarioName: ${d.activeScenarioName ?: "—"}")
            Text("activeScenarioType: ${d.activeScenarioType ?: "—"}")
            Text("actionsCount: ${d.actionsCount}")
            Text("currentActionIndex: ${d.currentActionIndex}")
            Text("currentRepeatIndex: ${d.currentRepeatIndex}")
            Text("auditEventsCount: ${d.auditEventsCount}")
            Text("lastAuditEventType: ${d.lastAuditEventType ?: "—"}")
            Text("lastRunStatus: ${d.lastRunStatus}")
            Text("simulationOnly: ${d.simulationOnly}")
            Text("realTapsEnabled: ${d.realTapsEnabled}")
            Text("scenarioStorageReady: ${d.scenarioStorageReady}")
            Text("corruptedScenarioRecovered: ${d.corruptedScenarioRecovered}")
            Text("storageMigrated: ${d.storageMigrated}")
            Text("${stringResource(R.string.profile_storage_ready)}: ${d.profileStorageReady}")
            Text("${stringResource(R.string.corrupted_profile_storage_recovered)}: ${d.corruptedProfileStorageRecovered}")
            Text("${stringResource(R.string.audit_storage_ready)}: ${d.auditStorageReady}")
            Text("${stringResource(R.string.corrupted_audit_recovered)}: ${d.corruptedAuditRecovered}")
            Text("backupAvailable: ${d.backupAvailable}")
            Text("lastBackupExportAt: ${d.lastBackupExportAt ?: "—"}")
            Text("lastBackupImportAt: ${d.lastBackupImportAt ?: "—"}")
            Text("invalidImportItemsLast: ${d.invalidImportItemsLast}")
            Text("backupContainsAuditLog: ${d.backupContainsAuditLog}")
            Text("externalStorageUsed: ${d.externalStorageUsed}")
            Text("permissionsRequired: ${d.permissionsRequired}")
            Text("overlayEnabled: ${d.overlayEnabled}")
            Text("accessibilityEnabled: ${d.accessibilityEnabled}")
        }
    }
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}

// ---- Permissions (Step 61) ------------------------------------------------

@Composable
private fun PermissionsScreen(vm: ClickFlowViewModel) {
val status by vm.permissionStatus.collectAsState()
ScreenScaffold {
    Text(stringResource(R.string.btn_permissions), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.permissions_explainer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    // Overlay (SYSTEM_ALERT_WINDOW)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.permission_overlay), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(if (status.overlayGranted) R.string.permission_status_granted else R.string.permission_status_not_granted),
                    color = if (status.overlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(R.string.permission_overlay_explainer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { vm.openOverlaySettings() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permission_open_settings))
            }
        }
    }

    // Accessibility service
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.permission_accessibility), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(if (status.accessibilityEnabled) R.string.permission_status_granted else R.string.permission_status_not_granted),
                    color = if (status.accessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(R.string.permission_accessibility_explainer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { vm.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.permission_open_settings))
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { vm.refreshPermissions() }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.permission_refresh))
    }
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.permissions_real_input_disclaimer), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    Text(stringResource(R.string.real_taps_disabled), fontWeight = FontWeight.SemiBold)
    NavButton(stringResource(R.string.btn_back)) { vm.navigateTo(Screen.ADVANCED) }
}
}
