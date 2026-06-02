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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.R
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.RealTapDispatchResult
import com.clickflow.android.core.RealTapSessionState
import com.clickflow.android.core.Screen

/**
* Step 62: Real Tap Prototype screen.
* Step 63: adds a result chip + a "why blocked" reasons list driven by the live SafetyGate.
*
* UI skeleton ONLY. SafetyGate.canRunRealTap() still returns false,
* so the actual tap dispatch is blocked. This screen exists to:
*  1. Walk the user through a 10-item safety review checklist
*  2. Start / end an explicit "real tap session" (audited)
*  3. Request a single-tap consent (10-second window, audited)
*  4. Attempt dispatch (always returns BLOCKED_BY_GATE in this build)
*  5. (Step 63) Show the most recent dispatch result + the live list of
*     missing prototype gate flags.
*
* Bulk real taps remain forbidden. Emergency stop terminates the session
* and resets every prototype flag.
*
* This file is intentionally self-contained: it does not depend on any
* private helper in Screens.kt. The small Scaffold/Message helpers below
* are local duplicates to keep this screen isolated and reviewable.
*/
@Composable
internal fun RealTapPrototypeScreen(vm: ClickFlowViewModel) {
   val review by vm.safetyReview.collectAsState()
   val session by vm.realTapSession.collectAsState()
   val consent by vm.realTapConsent.collectAsState()
   val gateReasons by vm.safetyGateReasons.collectAsState()
   val lastResult by vm.lastDispatchResult.collectAsState()

   RealTapScaffold {
       Text(
           stringResource(R.string.real_tap_prototype_title),
           style = MaterialTheme.typography.headlineSmall,
           fontWeight = FontWeight.Bold,
       )
       Text(
           stringResource(R.string.real_tap_prototype_intro),
           style = MaterialTheme.typography.bodyMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant,
       )
       RealTapMessageLine(vm)

       // --- Safety review checklist ---
       Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
           Column(
               Modifier.padding(16.dp),
               verticalArrangement = Arrangement.spacedBy(6.dp),
           ) {
               Text(stringResource(R.string.real_tap_safety_review), fontWeight = FontWeight.SemiBold)
               review.itemsLocalized().forEachIndexed { index, item ->
                   Row(
                       Modifier.fillMaxWidth(),
                       horizontalArrangement = Arrangement.SpaceBetween,
                       verticalAlignment = Alignment.CenterVertically,
                   ) {
                       Text(
                           (if (item.checked) "☑ " else "☐ ") + item.label,
                           style = MaterialTheme.typography.bodySmall,
                           modifier = Modifier.fillMaxWidth(0.8f),
                       )
                       TextButton(onClick = { vm.toggleSafetyReviewItem(index) }) {
                           Text(if (item.checked) "−" else "+")
                       }
                   }
               }
               HorizontalDivider()
               Text(
                   stringResource(
                       if (review.allPassed) R.string.real_tap_safety_review_passed
                       else R.string.real_tap_blocked_by_gate
                   ),
                   color = if (review.allPassed) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error,
                   fontWeight = FontWeight.Bold,
               )
           }
       }

       // --- Session controls ---
       when (session) {
           RealTapSessionState.INACTIVE -> {
               Button(
                   onClick = { vm.startRealTapSession() },
                   enabled = review.allPassed,
                   modifier = Modifier.fillMaxWidth(),
               ) { Text(stringResource(R.string.real_tap_session_start)) }
           }
           RealTapSessionState.ACTIVE -> {
               Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                   Column(
                       Modifier.padding(16.dp),
                       verticalArrangement = Arrangement.spacedBy(8.dp),
                   ) {
                       Text(
                           stringResource(R.string.real_tap_audit_session_started),
                           fontWeight = FontWeight.SemiBold,
                           color = MaterialTheme.colorScheme.primary,
                       )

                       // Consent flow
                       if (consent == null) {
                           Button(
                               onClick = { vm.requestRealTap() },
                               modifier = Modifier.fillMaxWidth(),
                           ) { Text(stringResource(R.string.real_tap_request_consent)) }
                       } else {
                           Text(
                               stringResource(R.string.real_tap_consent_pending),
                               fontWeight = FontWeight.SemiBold,
                           )
                           Row(
                               Modifier.fillMaxWidth(),
                               horizontalArrangement = Arrangement.spacedBy(8.dp),
                           ) {
                               Button(
                                   onClick = { vm.confirmRealTap() },
                                   modifier = Modifier.weight(1f),
                               ) { Text(stringResource(R.string.real_tap_confirm)) }
                               OutlinedButton(
                                   onClick = { vm.cancelRealTap() },
                                   modifier = Modifier.weight(1f),
                               ) { Text(stringResource(R.string.real_tap_cancel)) }
                           }
                       }

                       OutlinedButton(
                           onClick = { vm.endRealTapSession() },
                           modifier = Modifier.fillMaxWidth(),
                       ) { Text(stringResource(R.string.real_tap_session_end)) }
                   }
               }
           }
       }

       // --- Step 63: dispatch-result chip ---
       DispatchResultChip(result = lastResult, onClear = { vm.consumeLastDispatchResult() })

       // --- Step 63: why-blocked reasons list (collapses when empty) ---
       if (gateReasons.isNotEmpty()) {
           Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
               Column(
                   Modifier.padding(16.dp),
                   verticalArrangement = Arrangement.spacedBy(4.dp),
               ) {
                   Text(
                       stringResource(R.string.real_tap_why_blocked),
                       fontWeight = FontWeight.SemiBold,
                       color = MaterialTheme.colorScheme.error,
                   )
                   gateReasons.forEach { reason ->
                       Text(
                           "• $reason",
                           style = MaterialTheme.typography.bodySmall,
                           color = MaterialTheme.colorScheme.onSurfaceVariant,
                       )
                   }
               }
           }
       }

       Spacer(Modifier.height(8.dp))

       // --- Blocking notices ---
       Text(
           stringResource(R.string.real_tap_bulk_still_blocked),
           color = MaterialTheme.colorScheme.error,
           fontWeight = FontWeight.SemiBold,
           style = MaterialTheme.typography.bodySmall,
       )
       Text(
           stringResource(R.string.real_tap_emergency_stop_note),
           style = MaterialTheme.typography.bodySmall,
           color = MaterialTheme.colorScheme.onSurfaceVariant,
       )
       OutlinedButton(
           onClick = { vm.emergencyStop() },
           modifier = Modifier.fillMaxWidth(),
       ) { Text(stringResource(R.string.btn_emergency_stop)) }

       Spacer(Modifier.height(8.dp))
       OutlinedButton(
           onClick = { vm.navigateTo(Screen.ADVANCED) },
           modifier = Modifier.fillMaxWidth(),
       ) { Text(stringResource(R.string.btn_back)) }
   }
}

// --- Step 63: result chip composable ---

@Composable
private fun DispatchResultChip(result: RealTapDispatchResult?, onClear: () -> Unit) {
   if (result == null) return
   val labelRes = when (result) {
       RealTapDispatchResult.DISPATCHED -> R.string.real_tap_result_dispatched
       RealTapDispatchResult.BLOCKED_BY_GATE -> R.string.real_tap_result_blocked_by_gate
       RealTapDispatchResult.BLOCKED_NO_SERVICE -> R.string.real_tap_result_blocked_no_service
       RealTapDispatchResult.BLOCKED_INVALID_CONSENT -> R.string.real_tap_result_blocked_invalid_consent
       RealTapDispatchResult.DISPATCH_CANCELLED -> R.string.real_tap_result_dispatch_cancelled
       RealTapDispatchResult.DISPATCH_FAILED -> R.string.real_tap_result_dispatch_failed
   }
   val accent: Color = when (result) {
       RealTapDispatchResult.DISPATCHED -> MaterialTheme.colorScheme.primary
       RealTapDispatchResult.DISPATCH_CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
       else -> MaterialTheme.colorScheme.error
   }
   Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
       Row(
           Modifier.fillMaxWidth().padding(12.dp),
           horizontalArrangement = Arrangement.SpaceBetween,
           verticalAlignment = Alignment.CenterVertically,
       ) {
           Text(
               stringResource(labelRes),
               fontWeight = FontWeight.Bold,
               color = accent,
           )
           TextButton(onClick = onClear) { Text("✕") }
       }
   }
}

// --- Local helpers (intentional duplicates of private helpers in Screens.kt) ---

@Composable
private fun RealTapScaffold(content: @Composable ColumnScope.() -> Unit) {
   Column(
       modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
       verticalArrangement = Arrangement.spacedBy(12.dp),
       content = content,
   )
}

/** Mirrors MessageLine from Screens.kt — duplicated to keep this screen self-contained. */
@Composable
private fun RealTapMessageLine(vm: ClickFlowViewModel) {
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
       "real_tap_audit_session_started" -> R.string.real_tap_audit_session_started
       "real_tap_audit_session_ended" -> R.string.real_tap_audit_session_ended
       "real_tap_audit_consent_requested" -> R.string.real_tap_audit_consent_requested
       "real_tap_audit_consent_confirmed" -> R.string.real_tap_audit_consent_confirmed
       "real_tap_audit_consent_expired" -> R.string.real_tap_audit_consent_expired
       "real_tap_audit_dispatch_blocked" -> R.string.real_tap_audit_dispatch_blocked
       else -> null
   } ?: return
   Card(Modifier.fillMaxWidth()) {
       Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
           Text(stringResource(resId), fontWeight = FontWeight.SemiBold)
           TextButton(onClick = { vm.consumeMessage() }) { Text("✕") }
       }
   }
}
