package com.clickflow.android.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {

    // ---- EMPTY default ----

    @Test
    fun empty_hasAllFalse() {
        val empty = PermissionStatus.EMPTY
        assertFalse(empty.overlayGranted)
        assertFalse(empty.accessibilityEnabledInSettings)
        assertFalse(empty.accessibilityRunning)
        assertFalse(empty.readError)
    }

    @Test
    fun empty_accessibilityReady_isFalse() {
        assertFalse(PermissionStatus.EMPTY.accessibilityReady)
    }

    @Test
    fun empty_accessibilityNeedsRestart_isFalse() {
        assertFalse(PermissionStatus.EMPTY.accessibilityNeedsRestart)
    }

    @Test
    fun empty_nothingGranted_isTrue() {
        assertTrue(PermissionStatus.EMPTY.nothingGranted)
    }

    // ---- accessibilityReady ----

    @Test
    fun accessibilityReady_bothTrue() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = true,
            accessibilityRunning = true,
        )
        assertTrue(status.accessibilityReady)
    }

    @Test
    fun accessibilityReady_enabledButNotRunning() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = true,
            accessibilityRunning = false,
        )
        assertFalse(status.accessibilityReady)
    }

    @Test
    fun accessibilityReady_runningButNotEnabled() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = false,
            accessibilityRunning = true,
        )
        assertFalse(status.accessibilityReady)
    }

    @Test
    fun accessibilityReady_bothFalse() {
        val status = PermissionStatus()
        assertFalse(status.accessibilityReady)
    }

    // ---- accessibilityNeedsRestart ----

    @Test
    fun needsRestart_enabledButNotRunning() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = true,
            accessibilityRunning = false,
        )
        assertTrue(status.accessibilityNeedsRestart)
    }

    @Test
    fun needsRestart_enabledAndRunning() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = true,
            accessibilityRunning = true,
        )
        assertFalse(status.accessibilityNeedsRestart)
    }

    @Test
    fun needsRestart_notEnabled() {
        val status = PermissionStatus(
            accessibilityEnabledInSettings = false,
            accessibilityRunning = false,
        )
        assertFalse(status.accessibilityNeedsRestart)
    }

    // ---- accessibilityEnabled alias ----

    @Test
    fun accessibilityEnabled_aliasesToEnabledInSettings() {
        val status = PermissionStatus(accessibilityEnabledInSettings = true)
        assertTrue(status.accessibilityEnabled)

        val status2 = PermissionStatus(accessibilityEnabledInSettings = false)
        assertFalse(status2.accessibilityEnabled)
    }

    // ---- nothingGranted ----

    @Test
    fun nothingGranted_overlayTrue() {
        val status = PermissionStatus(overlayGranted = true)
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_accessibilityTrue() {
        val status = PermissionStatus(accessibilityEnabledInSettings = true)
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_bothTrue() {
        val status = PermissionStatus(
            overlayGranted = true,
            accessibilityEnabledInSettings = true,
        )
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_readError() {
        val status = PermissionStatus(readError = true)
        assertTrue(status.nothingGranted)
    }

    // ---- readError ----

    @Test
    fun readError_treatedAsNotGranted() {
        val status = PermissionStatus(readError = true, overlayGranted = true)
        // readError is just a flag; overlayGranted is still true
        assertTrue(status.overlayGranted)
        assertTrue(status.readError)
    }

    // ---- data class equality ----

    @Test
    fun equalStatuses_areEqual() {
        val a = PermissionStatus(
            overlayGranted = true,
            accessibilityEnabledInSettings = true,
            accessibilityRunning = false,
            readError = false,
        )
        val b = PermissionStatus(
            overlayGranted = true,
            accessibilityEnabledInSettings = true,
            accessibilityRunning = false,
            readError = false,
        )
        assertEquals(a, b)
    }

    @Test
    fun differentStatuses_areNotEqual() {
        val a = PermissionStatus(overlayGranted = true)
        val b = PermissionStatus(overlayGranted = false)
        assertFalse(a == b)
    }
}
