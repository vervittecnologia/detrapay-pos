package com.detrapay.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentTypeRulesTest {

    @Test
    fun `terminal approval is required only for pix credit and debit`() {
        assertTrue(PaymentTypeRules.requiresTerminalApproval("pix"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("credito"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("crédito"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("credit"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("debito"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("débito"))
        assertTrue(PaymentTypeRules.requiresTerminalApproval("debit"))

        assertFalse(PaymentTypeRules.requiresTerminalApproval("dinheiro"))
        assertFalse(PaymentTypeRules.requiresTerminalApproval("store_credit"))
        assertFalse(PaymentTypeRules.requiresTerminalApproval("boleto"))
        assertFalse(PaymentTypeRules.requiresTerminalApproval("transferencia"))
        assertFalse(PaymentTypeRules.requiresTerminalApproval(null))
    }
}
