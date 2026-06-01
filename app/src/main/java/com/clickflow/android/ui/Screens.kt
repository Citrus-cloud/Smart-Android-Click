package com.clickflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clickflow.android.R
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.Screen
import com.clickflow.android.scenarios.Scenario

@Composable
fun ClickFlowApp(vm: ClickFlowViewModel) {
    val screen by vm.screen.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.HOME -> HomeScreen(vm)
            Screen.SCENARIOS -> ScenariosScreen(vm)
            Screen.SCENARIO_FORM -> ScenarioFormScreen(vm)
            Screen.SAFETY -> SafetyCenterScreen(vm)
            Screen.DIAGNOSTICS -> DiagnosticsScreen(vm)
        }
    }
}

@Composable
private fun ScreenScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun BackButton(vm: ClickFlowViewModel) {
    OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }) {
        Text(stringResource(R.string.btn_back))
    }
}

// ---- Home -----------------------------------------------------------------

@Composable
private fun HomeScreen(vm: ClickFlowViewModel) {
    val status by vm.status.collectAsState()
    val progress by vm.progress.collectAsState()
    val scenarios by vm.scenarios.collectAsState()
    val active = scenarios.firstOrNull { it.isActive } ?: scenarios.firstOrNull()

    ScreenScaffold {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.status_badge), style = MaterialTheme.typography.labelLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.active_scenario), fontWeight = FontWeight.SemiBold)
                if (active != null) {
                    Text(active.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.scenario_summary,
                            active.settings.x, active.settings.y,
                            active.settings.repeatCount, active.settings.intervalMs,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(stringResource(R.string.active_scenario_none))
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.current_status, status.name.lowercase()))
                if (progress.totalSteps > 0) {
                    LinearProgressIndicator(
                        progress = { progress.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${progress.currentStep}/${progress.totalSteps} (${progress.percent}%)")
                    progress.lastLog?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Button(onClick = { vm.runActiveScenarioSimulation() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_start_simulation))
        }
        OutlinedButton(onClick = { vm.stopSimulation() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_stop))
        }
        Button(onClick = { vm.emergencyStop() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_emergency_stop))
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.navigateTo(Screen.SCENARIOS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.scenarios))
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.SAFETY) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_safety_center))
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.DIAGNOSTICS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_diagnostics))
        }

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.real_taps_disclaimer), fontWeight = FontWeight.SemiBold)
    }
}

// ---- Scenarios list -------------------------------------------------------

@Composable
private fun ScenariosScreen(vm: ClickFlowViewModel) {
    val scenarios by vm.scenarios.collectAsState()
    ScreenScaffold {
        Text(stringResource(R.string.scenarios), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        if (scenarios.isEmpty()) {
            Text(stringResource(R.string.no_scenarios))
            Button(onClick = { vm.resetScenarios() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.create_default_scenario))
            }
        } else {
            scenarios.forEach { scenario -> ScenarioCard(vm, scenario) }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.openCreateScenario() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.create_scenario))
        }
        OutlinedButton(onClick = { vm.resetScenarios() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_scenarios))
        }
        Text(stringResource(R.string.real_taps_disclaimer), fontWeight = FontWeight.SemiBold)
        BackButton(vm)
    }
}

@Composable
private fun ScenarioCard(vm: ClickFlowViewModel, scenario: Scenario) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(scenario.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                if (scenario.isActive) {
                    Text(stringResource(R.string.active_badge), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(scenario.type.name.lowercase(), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(
                    R.string.scenario_summary,
                    scenario.settings.x, scenario.settings.y,
                    scenario.settings.repeatCount, scenario.settings.intervalMs,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Divider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.selectScenario(scenario.id) }) {
                    Text(stringResource(R.string.select_scenario))
                }
                TextButton(onClick = { vm.openEditScenario(scenario.id) }) {
                    Text(stringResource(R.string.edit_scenario))
                }
                TextButton(onClick = { vm.deleteScenario(scenario.id) }) {
                    Text(stringResource(R.string.delete_scenario))
                }
            }
            Button(onClick = { vm.runScenarioSimulation(scenario.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.run_simulation))
            }
        }
    }
}

// ---- Scenario form --------------------------------------------------------

@Composable
private fun ScenarioFormScreen(vm: ClickFlowViewModel) {
    val form by vm.formState.collectAsState()
    val errors by vm.validationErrors.collectAsState()

    ScreenScaffold {
        Text(
            text = stringResource(if (form.isEditing) R.string.edit_scenario else R.string.create_scenario),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> vm.updateScenarioForm { it.copy(name = v) } },
            label = { Text(stringResource(R.string.scenario_name)) },
            isError = errors.name != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        errors.name?.let { Text(stringResource(R.string.validation_name_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.x,
                onValueChange = { v -> vm.updateScenarioForm { it.copy(x = v) } },
                label = { Text("X") },
                isError = errors.coordinates != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.y,
                onValueChange = { v -> vm.updateScenarioForm { it.copy(y = v) } },
                label = { Text("Y") },
                isError = errors.coordinates != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        errors.coordinates?.let { Text(stringResource(R.string.validation_coordinate_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = form.repeatCount,
            onValueChange = { v -> vm.updateScenarioForm { it.copy(repeatCount = v) } },
            label = { Text(stringResource(R.string.repeat_count)) },
            isError = errors.repeatCount != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        errors.repeatCount?.let { Text(stringResource(R.string.validation_repeat_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        OutlinedTextField(
            value = form.intervalMs,
            onValueChange = { v -> vm.updateScenarioForm { it.copy(intervalMs = v) } },
            label = { Text(stringResource(R.string.interval_ms)) },
            isError = errors.intervalMs != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        errors.intervalMs?.let { Text(stringResource(R.string.validation_interval_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.saveScenarioForm() }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save))
            }
            OutlinedButton(onClick = { vm.cancelScenarioForm() }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
        }
        Text(stringResource(R.string.simulation_only_scenario), style = MaterialTheme.typography.bodySmall)
    }
}

// ---- Safety Center --------------------------------------------------------

@Composable
private fun SafetyCenterScreen(vm: ClickFlowViewModel) {
    ScreenScaffold {
        Text(stringResource(R.string.btn_safety_center), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        vm.safetyCenter.items().forEach { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.label, fontWeight = FontWeight.SemiBold)
                    Text(item.status)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.real_taps_disabled), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Text(stringResource(R.string.blocked_reasons_title), fontWeight = FontWeight.Bold)
        vm.blockedReasons().forEach { reason -> Text("• $reason") }
        BackButton(vm)
    }
}

// ---- Diagnostics ----------------------------------------------------------

@Composable
private fun DiagnosticsScreen(vm: ClickFlowViewModel) {
    val d = vm.diagnostics()
    ScreenScaffold {
        Text(stringResource(R.string.btn_diagnostics), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("app version: ${d.appVersion}")
                Text("scenariosCount: ${d.scenariosCount}")
                Text("activeScenarioName: ${d.activeScenarioName ?: "—"}")
                Text("activeScenarioType: ${d.activeScenarioType ?: "—"}")
                Text("lastRunStatus: ${d.lastRunStatus}")
                Text("simulationOnly: ${d.simulationOnly}")
                Text("realTapsEnabled: ${d.realTapsEnabled}")
                Text("${stringResource(R.string.storage_ready)}: ${d.storageReady}")
                Text("${stringResource(R.string.corrupted_storage_recovered)}: ${d.corruptedStorageRecovered}")
                Text("accessibilityServicePlanned: ${d.accessibilityServicePlanned}")
                Text("mediaProjectionPlanned: ${d.mediaProjectionPlanned}")
                Text("emergencyStopReady: ${d.emergencyStopReady}")
            }
        }
        BackButton(vm)
    }
}
