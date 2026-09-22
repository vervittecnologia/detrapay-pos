package com.detrapay.ui.payment

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

sealed interface PaymentOperationState {
    data object Idle : PaymentOperationState
    data class Preparing(val operationId: String) : PaymentOperationState
    data class TerminalActive(val operationId: String) : PaymentOperationState
    data class AbortRequested(val operationId: String) : PaymentOperationState
    data class ApprovedPendingPersistence(val operationId: String) : PaymentOperationState
    data class ApprovedPendingServer(val operationId: String) : PaymentOperationState
    data class Completed(val operationId: String) : PaymentOperationState
    data class Failed(val operationId: String) : PaymentOperationState
}

class PaymentOperationCoordinator @Inject constructor() {
    private val mutex = Mutex()

    @Volatile
    private var state: PaymentOperationState = PaymentOperationState.Idle

    fun currentState(): PaymentOperationState = state

    fun begin(operationId: String): Boolean = locked(false) {
        when (val current = state) {
            PaymentOperationState.Idle,
            is PaymentOperationState.Completed,
            is PaymentOperationState.Failed,
            -> {
                state = PaymentOperationState.Preparing(operationId)
                true
            }
            is PaymentOperationState.ApprovedPendingServer -> current.operationId == operationId
            else -> false
        }
    }

    fun terminalStarted(operationId: String): PaymentOperationState = transition(operationId) {
        if (it is PaymentOperationState.Preparing) {
            PaymentOperationState.TerminalActive(operationId)
        } else {
            it
        }
    }

    fun requestAbort(operationId: String): PaymentOperationState = transition(operationId) {
        when (it) {
            is PaymentOperationState.Preparing,
            is PaymentOperationState.TerminalActive,
            -> PaymentOperationState.AbortRequested(operationId)
            else -> it
        }
    }

    fun terminalApproved(operationId: String): PaymentOperationState = transition(operationId) {
        when (it) {
            is PaymentOperationState.TerminalActive,
            is PaymentOperationState.AbortRequested,
            -> PaymentOperationState.ApprovedPendingPersistence(operationId)
            else -> it
        }
    }

    fun terminalRejected(operationId: String): PaymentOperationState = transition(operationId) {
        when (it) {
            is PaymentOperationState.Preparing,
            is PaymentOperationState.TerminalActive,
            is PaymentOperationState.AbortRequested,
            -> PaymentOperationState.Failed(operationId)
            else -> it
        }
    }

    fun persistenceSucceeded(operationId: String): PaymentOperationState = transition(operationId) {
        if (it is PaymentOperationState.ApprovedPendingPersistence) {
            PaymentOperationState.ApprovedPendingServer(operationId)
        } else {
            it
        }
    }

    fun resumeApproved(operationId: String): Boolean = locked(false) {
        val currentId = operationId(state)
        if (state == PaymentOperationState.Idle ||
            state is PaymentOperationState.Completed ||
            state is PaymentOperationState.Failed ||
            currentId == operationId
        ) {
            state = PaymentOperationState.ApprovedPendingServer(operationId)
            true
        } else {
            false
        }
    }

    fun completionSucceeded(operationId: String): PaymentOperationState = transition(operationId) {
        if (it is PaymentOperationState.ApprovedPendingServer) {
            PaymentOperationState.Completed(operationId)
        } else {
            it
        }
    }

    fun acceptsTerminalEvents(): Boolean = when (state) {
        is PaymentOperationState.TerminalActive,
        is PaymentOperationState.AbortRequested,
        -> true
        else -> false
    }

    private fun transition(
        operationId: String,
        transform: (PaymentOperationState) -> PaymentOperationState,
    ): PaymentOperationState = locked(state) {
        if (operationId(state) != operationId) return@locked state
        state = transform(state)
        state
    }

    private fun operationId(value: PaymentOperationState): String? = when (value) {
        PaymentOperationState.Idle -> null
        is PaymentOperationState.Preparing -> value.operationId
        is PaymentOperationState.TerminalActive -> value.operationId
        is PaymentOperationState.AbortRequested -> value.operationId
        is PaymentOperationState.ApprovedPendingPersistence -> value.operationId
        is PaymentOperationState.ApprovedPendingServer -> value.operationId
        is PaymentOperationState.Completed -> value.operationId
        is PaymentOperationState.Failed -> value.operationId
    }

    private inline fun <T> locked(fallback: T, block: () -> T): T {
        if (!mutex.tryLock()) return fallback
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}
