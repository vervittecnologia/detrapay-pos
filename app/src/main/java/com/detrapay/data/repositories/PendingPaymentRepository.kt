package com.detrapay.data.repositories

import com.detrapay.data.datasources.local.PendingPaymentCompletionDao
import com.detrapay.data.model.local.PendingPaymentCompletion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingPaymentRepository @Inject constructor(
    private val dao: PendingPaymentCompletionDao,
) {
    suspend fun saveApproved(completion: PendingPaymentCompletion) {
        require(completion.sessionUserId.isNotBlank()) { "sessionUserId is required" }
        require(completion.transactionId.isNotBlank()) { "transactionId is required" }
        require(completion.status == PendingPaymentCompletion.STATUS_APPROVED) {
            "Only approved payments can be persisted"
        }
        dao.upsert(completion)
    }

    suspend fun findForAttempt(
        attemptId: String,
        sessionUserId: String,
    ): PendingPaymentCompletion? = dao.findForAttempt(attemptId, sessionUserId)

    suspend fun listPending(sessionUserId: String): List<PendingPaymentCompletion> =
        dao.listPending(sessionUserId)

    suspend fun deleteCompleted(attemptId: String) {
        dao.deleteByAttempt(attemptId)
    }
}
