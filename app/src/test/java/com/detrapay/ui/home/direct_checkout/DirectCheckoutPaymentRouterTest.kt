package com.detrapay.ui.home.direct_checkout

import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectCheckoutPaymentRouterTest {

    @Test
    fun `pix and card payments start in page instead of opening dialog`() {
        assertTrue(
            DirectCheckoutPaymentRouter.routeFor(pendingPayment("pix"))
                is DirectCheckoutPaymentRoute.StartInPagePayment,
        )
        assertTrue(
            DirectCheckoutPaymentRouter.routeFor(pendingPayment("credito"))
                is DirectCheckoutPaymentRoute.StartInPagePayment,
        )
        assertTrue(
            DirectCheckoutPaymentRouter.routeFor(pendingPayment("debito"))
                is DirectCheckoutPaymentRoute.StartInPagePayment,
        )
    }

    @Test
    fun `manual payment types keep manual confirmation`() {
        assertTrue(
            DirectCheckoutPaymentRouter.routeFor(pendingPayment("dinheiro"))
                is DirectCheckoutPaymentRoute.ConfirmManually,
        )
        assertTrue(
            DirectCheckoutPaymentRouter.routeFor(pendingPayment("store_credit"))
                is DirectCheckoutPaymentRoute.ConfirmManually,
        )
    }

    private fun pendingPayment(paymentType: String): DirectCheckoutPendingPayment {
        return DirectCheckoutPendingPayment(
            order = TestDirectCheckoutFixtures.order(),
            receivable = TestDirectCheckoutFixtures.receivable(paymentType),
            paymentType = paymentType,
            amount = 100.0,
        )
    }
}
