package com.clickflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clickflow.android.R
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.core.RealTapSessionState
import com.clickflow.android.core.Screen

/**
* Step 62: Real Tap Prototype screen.
*
* UI skeleton ONLY. SafetyGate.canRunRealTap() still returns false,
* so the actual tap dispatch is blocked. This screen exists to:
*  1. Walk the user through a 10-item safety review checklist
*  2. Start / end an explicit "real tap session" (audited)
*  3. Request a single-tap consent (10-second window, audited)
*  4. Attempt dispatch (always returns dispatch_failed in this build)
*
* Bulk real taps remain forbidden. Emergency stop terminates the session.
*/
@Composable
internal fun RealTapPrototypeScreen(vm: ClickFlowViewModel) {
   val review by vm.safetyReview.collectAsState()
   val session by vm.realTapSession.collectAsState()
   val consent by vm.realTapConsent.collectAsState()

   ScreenScaffoldPublic {
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
       MessageLinePublic(vm)

       // --- Safety review checklist ---
       Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
           androidx.compose.foundation.layout.Column(
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
                   androidx.compose.foundation.layout.Column(
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
