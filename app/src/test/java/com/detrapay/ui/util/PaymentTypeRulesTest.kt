package com.detrapay.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentTypeRulesTest {
    @Test
    fun `pix and manual pix transfer remain distinct after normalization`() {
        assertEquals("pix", PaymentTypeRules.normalize("pix"))
        assertEquals("pix_manual", PaymentTypeRules.normalize("pix_manual"))
        assertEquals("pix_manual", PaymentTypeRules.normalize("transferencia_pix"))
    }

    @Test
    fun `normalization keeps display aliases without deciding payment flow`() {
        assertEquals("credito", PaymentTypeRules.normalize("credit"))
        assertEquals("debito", PaymentTypeRules.normalize("debit"))
        assertEquals("dinheiro", PaymentTypeRules.normalize("cash"))
        assertEquals("store_credit", PaymentTypeRules.normalize("credito loja"))
    }
}
