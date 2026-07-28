package com.detrapay.ui.util

import com.detrapay.data.model.remote.InstallmentFee
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentQuotePresenterTest {

    @Test
    fun `interest quote shows original total and installment with interest`() {
        val result = InstallmentQuotePresenter.present(
            amountOriginal = 100.0,
            quote = quote(
                installmentValue = "36.00",
                totalValue = "108.00",
                noInterest = false,
            ),
        )

        assertEquals(100.0, result.originalValue, 0.0)
        assertEquals(108.0, result.totalValue, 0.0)
        assertEquals(36.0, result.installmentValue, 0.0)
        assertEquals("Valor original: R$ 100,00", result.originalLabel)
        assertEquals("Total com juros: R$ 108,00", result.totalLabel)
        assertEquals("3x de R$ 36,00 com juros", result.installmentLabel)
    }

    @Test
    fun `no interest quote is labeled without interest`() {
        val result = InstallmentQuotePresenter.present(
            amountOriginal = 100.0,
            quote = quote(
                installments = 2,
                installmentValue = "50,00",
                totalValue = "100,00",
                noInterest = true,
            ),
        )

        assertEquals("Valor original: R$ 100,00", result.originalLabel)
        assertEquals("Total: R$ 100,00", result.totalLabel)
        assertEquals("2x de R$ 50,00 sem juros", result.installmentLabel)
    }

    @Test
    fun `persisted installment uses final amount for total and installment`() {
        val result = InstallmentQuotePresenter.present(
            amountOriginal = 100.0,
            amountFinal = 108.0,
            installments = 3,
        )

        assertEquals("Valor original: R$ 100,00", result.originalLabel)
        assertEquals("Total com juros: R$ 108,00", result.totalLabel)
        assertEquals("3x de R$ 36,00 com juros", result.installmentLabel)
    }

    private fun quote(
        installments: Int = 3,
        installmentValue: String,
        totalValue: String,
        noInterest: Boolean,
    ) = InstallmentFee(
        installmentNumber = installments,
        installmentValue = installmentValue,
        totalValue = totalValue,
        interestValue = if (noInterest) "0.00" else "8.00",
        noInterest = noInterest,
    )
}
