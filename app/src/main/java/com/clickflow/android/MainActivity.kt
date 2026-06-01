package com.clickflow.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.ui.ClickFlowApp
import com.clickflow.android.ui.ClickFlowTheme

/**
 * Single-activity entry point for ClickFlow Android (Step 52 foundation).
 *
 * SIMULATION-ONLY: no Accessibility Service, no MediaProjection, no overlay,
 * no real taps. The UI drives only the simulation engine.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ClickFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClickFlowTheme {
                ClickFlowApp(viewModel)
            }
        }
    }
}
