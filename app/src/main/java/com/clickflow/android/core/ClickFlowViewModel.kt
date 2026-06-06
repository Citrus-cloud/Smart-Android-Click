package com.clickflow.android.core

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.clickflow.android.permissions.PermissionStatus
import com.clickflow.android.permissions.PermissionsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Screens reachable in the app. */
enum class Screen { HOME, ADVANCED, PERMISSIONS }

/**
 * ViewModel for the shipping UI. The app exposes three screens (Home, Advanced,
 * Permissions); all real clicking happens in dedicated activities/services
 * (FloatingTapperOverlayService, ImageTemplateActivity, TextClickActivity,
 * ScenarioActivity). This ViewModel only tracks the current screen and the
 * runtime permission status.
 */
class ClickFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val appRef = app

    private val permissionsManager = PermissionsManager(app)
    private val _permissionStatus = MutableStateFlow(PermissionStatus.EMPTY)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissionStatus.value = permissionsManager.refresh()
    }

    fun navigateTo(screen: Screen) {
        _screen.value = screen
        if (screen == Screen.PERMISSIONS) refreshPermissions()
    }

    fun openOverlaySettings() {
        runCatching {
            val intent = permissionsManager.overlaySettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appRef.startActivity(intent)
        }
    }

    fun openAccessibilitySettings() {
        runCatching {
            val intent = permissionsManager.accessibilitySettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appRef.startActivity(intent)
        }
    }
}
