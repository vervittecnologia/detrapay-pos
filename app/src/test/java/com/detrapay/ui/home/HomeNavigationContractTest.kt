package com.detrapay.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeNavigationContractTest {

    @Test
    fun `orders is the only Home fragment destination`() {
        val graph = File("src/main/res/navigation/home_navigation.xml").readText()

        assertTrue(graph.contains("app:startDestination=\"@id/ordersFragment\""))
        assertTrue(graph.contains("com.detrapay.ui.home.orders.OrdersFragment"))
        assertFalse(graph.contains("com.detrapay.ui.registration.RegistrationActivity"))
        assertFalse(graph.contains("BottomNavigationView"))
        assertEqualsCount(1, graph, "<fragment")
        assertEqualsCount(0, graph, "<activity")
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

    private fun assertEqualsCount(expected: Int, source: String, token: String) {
        val actual = source.windowed(token.length).count { it == token }
        assertTrue("Expected $expected occurrences of $token but found $actual", actual == expected)
    }
}
