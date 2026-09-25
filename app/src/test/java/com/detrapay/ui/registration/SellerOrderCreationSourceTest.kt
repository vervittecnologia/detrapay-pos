package com.detrapay.ui.registration

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SellerOrderCreationSourceTest {

    @Test
    fun `new order screens use compose material 3`() {
        val source = source("src/main/java/com/detrapay/ui/registration/RegistrationActivity.kt")

        assertTrue(source.contains("DetrapayTheme"))
        assertTrue(source.contains("RegistrationDataScreen"))
        assertTrue(source.contains("RegistrationSummaryScreen"))
        assertTrue(source.contains("MaterialTheme.colorScheme"))
        assertTrue(source.contains("DatePickerDialog"))
    }

    @Test
    fun `created order opens official compose detail and fragments are removed`() {
        val registration = source("src/main/java/com/detrapay/ui/registration/RegistrationActivity.kt")
        val home = source("src/main/java/com/detrapay/ui/home/HomeActivity.kt")
        val ordersRoute = source("src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt")
        val manifest = source("src/main/AndroidManifest.xml")

        assertTrue(registration.contains("finishWithCreatedOrder"))
        assertTrue(home.contains("registerForActivityResult"))
        assertTrue(ordersRoute.contains("OrderFlowReducer.showDetail(localState, orderToOpen)"))
        assertFalse(manifest.contains("OrderDetailsActivity"))
        assertFalse(Files.exists(Paths.get("src/main/java/com/detrapay/ui/home/orders/OrdersFragment.kt")))
        assertFalse(Files.exists(Paths.get("src/main/java/com/detrapay/ui/order_details/OrderDetailsActivity.kt")))
        assertFalse(Files.exists(Paths.get("src/main/res/layout/activity_order_details.xml")))
    }

    private fun source(path: String): String =
        String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)
}
