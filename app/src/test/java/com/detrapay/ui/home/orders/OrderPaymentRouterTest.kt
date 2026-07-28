package com.detrapay.ui.home.orders

import com.detrapay.data.model.PaymentMethod
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPaymentRouterTest {

    @Test
    fun `online property routes credit debit and pix to PagBank`() {
        listOf("credito", "debito", "pix").forEach { type ->
            assertTrue(
                OrderPaymentRouter.routeFor(request(type, online = true))
                    is OrderPaymentRoute.Online,
            )
        }
    }

    @Test
    fun `offline property routes store credit pix transfer and cash to immediate record`() {
        listOf("store_credit", "pix_manual", "dinheiro").forEach { type ->
            assertTrue(
                OrderPaymentRouter.routeFor(request(type, online = false))
                    is OrderPaymentRoute.RecordOnly,
            )
        }
    }

    @Test
    fun `routing follows property even when method name resembles another flow`() {
        assertTrue(
            OrderPaymentRouter.routeFor(request("pix", online = false))
                is OrderPaymentRoute.RecordOnly,
        )
        assertTrue(
            OrderPaymentRouter.routeFor(request("pix_manual", online = true))
                is OrderPaymentRoute.Online,
        )
    }

    private fun request(paymentType: String, online: Boolean): OrderPaymentRequest =
        OrderPaymentRequest(
            order = TestOrderFixtures.order(),
            paymentMethod = PaymentMethod(
                id = 1,
                name = paymentType,
                installments = 1,
                interestTax = 0.0,
                paymentType = paymentType,
                isOnlinePayment = online,
            ),
            amount = 100.0,
            installments = 1,
            idempotencyKey = "test-$paymentType-$online",
        )
}
