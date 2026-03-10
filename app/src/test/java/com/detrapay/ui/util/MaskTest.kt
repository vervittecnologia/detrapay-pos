package com.detrapay.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskTest {

    @Test
    fun replaceChars_removesMaskAndCurrencyCharacters() {
        val result = Mask.replaceChars("R$ 12.345,67 / (89)-0")

        assertEquals("1234567890", result)
    }

    @Test
    fun toSafeDouble_parsesBrazilianCurrency() {
        val result = Mask.toSafeDouble("R$ 1.250,50")

        assertEquals(1250.50, result, 0.0)
    }

    @Test
    fun toSafeDouble_parsesStandardDecimal() {
        val result = Mask.toSafeDouble("1250.50")

        assertEquals(1250.50, result, 0.0)
    }

    @Test
    fun toSafeDouble_returnsZeroForInvalidValue() {
        val result = Mask.toSafeDouble("valor-invalido")

        assertEquals(0.0, result, 0.0)
    }
}
