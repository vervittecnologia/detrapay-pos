package com.detrapay.data.repositories

import com.detrapay.data.datasources.local.PendingPaymentCompletionDao
import com.detrapay.data.model.local.PendingPaymentCompletion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PendingPaymentRepositoryTest {

    private val dao = mockk<PendingPaymentCompletionDao>(relaxed = true)
    private lateinit var repository: PendingPaymentRepository

    @Before
    fun setUp() {
        repository = PendingPaymentRepository(dao)
    }

    @Test
    fun `approved payment is reloaded for the same session`() = runTest {
        val payment = pending(sessionUserId = "user-a")
        coEvery { dao.findForAttempt("attempt-1", "user-a") } returns payment

        repository.saveApproved(payment)

        assertEquals("tx-1", repository.findForAttempt("attempt-1", "user-a")?.transactionId)
        coVerify(exactly = 1) { dao.upsert(payment) }
    }

    @Test
    fun `pending payment is invisible to another session`() = runTest {
        coEvery { dao.findForAttempt("attempt-1", "user-b") } returns null

        repository.saveApproved(pending(sessionUserId = "user-a"))

        assertNull(repository.findForAttempt("attempt-1", "user-b"))
        coVerify(exactly = 1) { dao.findForAttempt("attempt-1", "user-b") }
    }

    @Test
    fun `pending list is scoped to the active session`() = runTest {
        val payment = pending(sessionUserId = "user-a")
        coEvery { dao.listPending("user-a") } returns listOf(payment)

        assertEquals(listOf(payment), repository.listPending("user-a"))
        coVerify(exactly = 1) { dao.listPending("user-a") }
    }

    @Test
    fun `completed payment is deleted by attempt`() = runTest {
        repository.deleteCompleted("attempt-1")

        coVerify(exactly = 1) { dao.deleteByAttempt("attempt-1") }
    }

    private fun pending(sessionUserId: String) = PendingPaymentCompletion(
        attemptId = "attempt-1",
        sessionUserId = sessionUserId,
        idempotencyKey = "idempotency-1",
        terminalReference = "PABC123456",
        orderId = 10,
        transactionId = "tx-1",
        transactionCode = "auth-1",
        date = "22/09/2026",
        time = "12:00:00",
        result = 0,
        paymentType = 1,
        installments = 1,
        cardBrand = "VISA",
        cardLast4 = "1234",
        cardHolder = "CLIENTE",
        pixTxIdCode = null,
        transactionLog = "{}",
        amountOriginal = 100.0,
        amountFinal = 100.0,
        status = PendingPaymentCompletion.STATUS_APPROVED,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
