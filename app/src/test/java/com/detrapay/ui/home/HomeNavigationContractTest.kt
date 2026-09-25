package com.detrapay.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationContractTest {

    @Test
    fun `home hosts orders directly in compose`() {
        val home = File("src/main/java/com/detrapay/ui/home/HomeActivity.kt").readText()

        assertTrue(home.contains("setContent"))
        assertTrue(home.contains("OrdersRoute("))
        assertTrue(home.contains("DetrapayTheme"))
        assertFalse(File("src/main/res/navigation/home_navigation.xml").exists())
        assertFalse(File("src/main/java/com/detrapay/ui/home/orders/OrdersFragment.kt").exists())
    }

    @Test
    fun `legacy home surfaces do not exist`() {
        val removedPaths = listOf(
            "src/main/java/com/detrapay/ui/home/registration",
            "src/main/java/com/detrapay/ui/home/order_list",
            "src/main/java/com/detrapay/ui/home/profile",
            "src/main/java/com/detrapay/ui/home/payment_history",
            "src/main/java/com/detrapay/ui/notification",
            "src/main/res/menu/home_navigation_menu.xml",
        )
        removedPaths.forEach { path ->
            assertFalse("Legacy Home path still exists: $path", File(path).exists())
        }
    }

    @Test
    fun `compose home exposes functional top level sections`() {
        val contract = File(
            "src/main/java/com/detrapay/ui/home/orders/OrderFlowContract.kt",
        ).readText()
        val screen = File(
            "src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt",
        ).readText()

        assertTrue(contract.contains("enum class SellerHomeSection"))
        assertTrue(contract.contains("SelectHomeSection"))
        assertTrue(screen.contains("SellerHomeSection.Orders -> OrdersListScreen"))
        assertTrue(screen.contains("SellerHomeSection.Profile -> SellerProfileScreen"))
        assertFalse(contract.contains("Home,"))
    }
}
