package com.detrapay.data.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.detrapay.data.model.local.PendingPaymentCompletion

@Dao
interface PendingPaymentCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: PendingPaymentCompletion)

    @Query(
        """
        SELECT * FROM pending_payment_completions
        WHERE attempt_id = :attemptId AND session_user_id = :sessionUserId
        LIMIT 1
        """,
    )
    suspend fun findForAttempt(
        attemptId: String,
        sessionUserId: String,
    ): PendingPaymentCompletion?

    @Query(
        """
        SELECT * FROM pending_payment_completions
        WHERE session_user_id = :sessionUserId AND status = 'approved_pending_server'
        ORDER BY created_at ASC
        """,
    )
    suspend fun listPending(sessionUserId: String): List<PendingPaymentCompletion>

    @Query("DELETE FROM pending_payment_completions WHERE attempt_id = :attemptId")
    suspend fun deleteByAttempt(attemptId: String)
}
