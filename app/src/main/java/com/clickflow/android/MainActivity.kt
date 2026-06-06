package com.clickflow.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.clickflow.android.core.ClickFlowViewModel
import com.clickflow.android.ui.ClickFlowApp
import com.clickflow.android.ui.ClickFlowTheme

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
