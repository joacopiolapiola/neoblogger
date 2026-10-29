package com.neoblogger.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPreferencesTest {

    @Test
    fun adStateTransitions_workAsExpected() {
        var state = AdState.PENDING
        assertTrue(state == AdState.PENDING)

        state = AdState.COMPLETED_SKIPPED
        assertFalse(state == AdState.PENDING)
        assertEquals(AdState.COMPLETED_SKIPPED, state)

        state = AdState.COMPLETED_SHOWN
        assertFalse(state == AdState.PENDING)
        assertEquals(AdState.COMPLETED_SHOWN, state)
    }
}