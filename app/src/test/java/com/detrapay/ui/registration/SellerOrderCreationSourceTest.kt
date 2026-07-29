package com.detrapay.ui.registration

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SellerOrderCreationSourceTest {

    @Test
    fun `new order screens use seller wizard styling`() {
        fun layout(name: String): String {
            val path = Paths.get("src/main/res/layout/$name.xml")
            return String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        }

        val data = layout("fragment_registration_order_data")
        val resume = layout("fragment_registration_order_resume")

        assertTrue(data.contains("@style/Widget.Detrapay.SellerWizardInput"))
        assertTrue(data.contains("@color/seller_canvas"))
        assertTrue(data.contains("android:text=\"Dados do pedido\""))
        assertTrue(resume.contains("@color/seller_canvas"))
        assertTrue(resume.contains("@style/Widget.Detrapay.SellerCard"))
    }

    @Test
    fun `created order opens official compose detail and legacy activity is removed`() {
        fun source(path: String): String {
            return String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)
        }

        val payment = source(
            "src/main/java/com/detrapay/ui/registration/payment_method/RegistrationPaymentMethodFragment.kt",
        )
        val resume = source(
            "src/main/java/com/detrapay/ui/registration/resume/RegistrationResumeFragment.kt",
        )
        val ordersFragment = source(
            "src/main/java/com/detrapay/ui/home/orders/OrdersFragment.kt",
        )
        val ordersRoute = source(
            "src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt",
        )
        val manifest = source("src/main/AndroidManifest.xml")

        assertTrue(payment.contains("finishWithCreatedOrder"))
        assertTrue(resume.contains("finishWithCreatedOrder"))
        assertTrue(ordersFragment.contains("registerForActivityResult"))
        assertTrue(ordersRoute.contains("OrderFlowReducer.showDetail(localState, orderToOpen)"))
        assertFalse(manifest.contains("OrderDetailsActivity"))
        assertFalse(
            Files.exists(
                Paths.get(
                    "src/main/java/com/detrapay/ui/order_details/OrderDetailsActivity.kt",
                ),
            ),
        )
        assertFalse(Files.exists(Paths.get("src/main/res/layout/activity_order_details.xml")))
    }
}
