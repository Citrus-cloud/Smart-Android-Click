package com.clickflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.R
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.Screen

@Composable
fun ClickFlowApp(vm: ClickFlowViewModel) {
    val screen by vm.screen.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            Screen.HOME -> HomeScreen(vm)
            Screen.SCENARIOS -> ScenariosScreen(vm)
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

// Re-export ColumnScope for the helper above.
private typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun HomeScreen(vm: ClickFlowViewModel) {
    val status by vm.status.collectAsState()
    ScreenScaffold {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.status_badge),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.current_status, status.name.lowercase()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(onClick = { vm.startSimulation() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_start_simulation))
        }
        OutlinedButton(onClick = { vm.stop() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_stop))
        }
        Button(onClick = { vm.emergencyStop() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_emergency_stop))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = { vm.navigateTo(Screen.SCENARIOS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_scenarios))
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.SAFETY) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_safety_center))
        }
        OutlinedButton(onClick = { vm.navigateTo(Screen.DIAGNOSTICS) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.btn_diagnostics))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.real_taps_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BackButton(vm: ClickFlowViewModel) {
    OutlinedButton(onClick = { vm.navigateTo(Screen.HOME) }) {
        Text(stringResource(R.string.btn_back))
    }
}

@Composable
private fun ScenariosScreen(vm: ClickFlowViewModel) {
    ScreenScaffold {
        Text(stringResource(R.string.btn_scenarios), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        vm.scenarios().forEach { scenario ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(scenario.name, fontWeight = FontWeight.SemiBold)
                        Text(scenario.type.name.lowercase(), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { vm.startSimulation(scenario) }) {
                        Text(stringResource(R.string.btn_run))
                    }
                }
            }
        }
        Text(stringResource(R.string.real_taps_disclaimer), fontWeight = FontWeight.SemiBold)
        BackButton(vm)
    }
}

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
        Text(stringResource(R.string.blocked_reasons_title), fontWeight = FontWeight.Bold)
        vm.blockedReasons().forEach { reason -> Text("• $reason") }
        BackButton(vm)
    }
}

@Composable
private fun DiagnosticsScreen(vm: ClickFlowViewModel) {
    val d = vm.diagnostics()
    ScreenScaffold {
        Text(stringResource(R.string.btn_diagnostics), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("app version: ${d.appVersion}")
                Text("simulationOnly: ${d.simulationOnly}")
                Text("realTapsEnabled: ${d.realTapsEnabled}")
                Text("accessibilityServicePlanned: ${d.accessibilityServicePlanned}")
                Text("mediaProjectionPlanned: ${d.mediaProjectionPlanned}")
                Text("emergencyStopReady: ${d.emergencyStopReady}")
                Text("activeScenario: ${d.activeScenario ?: "—"}")
                Text("lastRunStatus: ${d.lastRunStatus}")
            }
        }
        BackButton(vm)
    }
}
