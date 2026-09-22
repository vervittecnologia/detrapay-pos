package com.detrapay.ui.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentOperationCoordinatorTest {

    private val coordinator = PaymentOperationCoordinator()

    @Test
    fun `approval after abort request remains recoverable`() {
        assertTrue(coordinator.begin("attempt-1"))
        coordinator.terminalStarted("attempt-1")
        coordinator.requestAbort("attempt-1")

        assertEquals(
            PaymentOperationState.ApprovedPendingPersistence("attempt-1"),
            coordinator.terminalApproved("attempt-1"),
        )
    }

    @Test
    fun `second payment cannot begin while abort is unresolved`() {
        assertTrue(coordinator.begin("attempt-1"))
        coordinator.terminalStarted("attempt-1")
        coordinator.requestAbort("attempt-1")

        assertFalse(coordinator.begin("attempt-2"))
        assertEquals(PaymentOperationState.AbortRequested("attempt-1"), coordinator.currentState())
    }

    @Test
    fun `late event for another operation cannot change active state`() {
        coordinator.begin("attempt-1")
        coordinator.terminalStarted("attempt-1")

        coordinator.terminalApproved("attempt-2")

        assertEquals(PaymentOperationState.TerminalActive("attempt-1"), coordinator.currentState())
    }

    @Test
    fun `same durable operation may retry server completion without terminal`() {
        coordinator.begin("attempt-1")
        coordinator.terminalStarted("attempt-1")
        coordinator.terminalApproved("attempt-1")
        coordinator.persistenceSucceeded("attempt-1")

        assertTrue(coordinator.begin("attempt-1"))
        assertFalse(coordinator.begin("attempt-2"))
        assertEquals(
            PaymentOperationState.ApprovedPendingServer("attempt-1"),
            coordinator.currentState(),
        )
    }
}
