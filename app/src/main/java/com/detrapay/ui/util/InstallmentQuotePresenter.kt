package com.detrapay.ui.util

import com.detrapay.data.model.remote.InstallmentFee
import java.util.Locale
import kotlin.math.roundToLong

data class InstallmentQuotePresentation(
    val originalValue: Double,
    val totalValue: Double,
    val installmentValue: Double,
    val originalLabel: String,
    val totalLabel: String,
    val installmentLabel: String,
)

object InstallmentQuotePresenter {
    private val locale = Locale("pt", "BR")

    fun present(
        amountOriginal: Double,
        quote: InstallmentFee,
    ): InstallmentQuotePresentation {
        return build(
            amountOriginal = amountOriginal,
            totalValue = parseMoney(quote.totalValue),
            installmentValue = parseMoney(quote.installmentValue),
            installments = quote.installmentNumber.coerceAtLeast(1),
            noInterest = quote.noInterest,
        )
    }

    fun present(
        amountOriginal: Double,
        amountFinal: Double,
        installments: Int,
    ): InstallmentQuotePresentation {
        val count = installments.coerceAtLeast(1)
        return build(
            amountOriginal = amountOriginal,
            totalValue = amountFinal,
            installmentValue = amountFinal / count,
            installments = count,
            noInterest = cents(amountFinal) <= cents(amountOriginal),
        )
    }

    private fun build(
        amountOriginal: Double,
        totalValue: Double,
        installmentValue: Double,
        installments: Int,
        noInterest: Boolean,
    ): InstallmentQuotePresentation {
        return InstallmentQuotePresentation(
            originalValue = amountOriginal,
            totalValue = totalValue,
            installmentValue = installmentValue,
            originalLabel = "Valor original: ${currency(amountOriginal)}",
            totalLabel = if (noInterest) {
                "Total: ${currency(totalValue)}"
            } else {
                "Total com juros: ${currency(totalValue)}"
            },
            installmentLabel = "${installments}x de ${currency(installmentValue)} ${
                if (noInterest) "sem juros" else "com juros"
            }",
        )
    }

    private fun currency(value: Double): String = "R$ %,.2f".format(locale, value)

    private fun cents(value: Double): Long = (value * 100.0).roundToLong()

    private fun parseMoney(value: String): Double {
        val clean = value
            .replace("R$", "")
            .replace("\\s".toRegex(), "")
            .trim()
        if (clean.isEmpty()) return 0.0
        val decimal = if (clean.contains(',')) {
            clean.replace(".", "").replace(',', '.')
        } else {
            clean
        }
        return decimal.toDoubleOrNull() ?: 0.0
    }
}
