package com.projectred.scrollguard.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationActionReceiverTest {

    @Test
    fun `test action constants are well defined`() {
        assertEquals("com.projectred.scrollguard.ACTION_TOGGLE_MASTER", NotificationActionReceiver.ACTION_TOGGLE_MASTER)
        assertEquals("com.projectred.scrollguard.ACTION_SNOOZE_15", NotificationActionReceiver.ACTION_SNOOZE_15)
        assertEquals("com.projectred.scrollguard.ACTION_RESUME", NotificationActionReceiver.ACTION_RESUME)
        assertEquals("com.projectred.scrollguard.ACTION_NOTIFICATION_DISMISSED", NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED)
    }
}
