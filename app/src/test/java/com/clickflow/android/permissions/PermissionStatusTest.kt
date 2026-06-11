package com.clickflow.android.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun empty_hasAllFalse() {
        val empty = PermissionStatus.EMPTY
        assertFalse(empty.overlayGranted)
        assertFalse(empty.accessibilityEnabled)
        assertFalse(empty.readError)
    }

    @Test
    fun empty_nothingGranted_isTrue() {
        assertTrue(PermissionStatus.EMPTY.nothingGranted)
    }

    @Test
    fun nothingGranted_overlayTrue() {
        val status = PermissionStatus(overlayGranted = true)
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_accessibilityTrue() {
        val status = PermissionStatus(accessibilityEnabled = true)
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_bothTrue() {
        val status = PermissionStatus(overlayGranted = true, accessibilityEnabled = true)
        assertFalse(status.nothingGranted)
    }

    @Test
    fun nothingGranted_readError() {
        val status = PermissionStatus(readError = true)
        assertTrue(status.nothingGranted)
    }

    @Test
    fun equalStatuses_areEqual() {
        val a = PermissionStatus(overlayGranted = true, accessibilityEnabled = true, readError = false)
        val b = PermissionStatus(overlayGranted = true, accessibilityEnabled = true, readError = false)
        assertEquals(a, b)
    }

    @Test
    fun differentStatuses_areNotEqual() {
        val a = PermissionStatus(overlayGranted = true)
        val b = PermissionStatus(overlayGranted = false)
        assertFalse(a == b)
    }

    @Test
    fun dataClass_copy() {
        val original = PermissionStatus(overlayGranted = true, accessibilityEnabled = false)
        val modified = original.copy(accessibilityEnabled = true)
        assertTrue(modified.overlayGranted)
        assertTrue(modified.accessibilityEnabled)
    }
}
