package com.detrapay.ui.home.orders

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersRefreshContractTest {

    @Test
    fun `pull to refresh forces a fresh orders request and shows refreshing state`() {
        val routeSource = File("src/main/java/com/detrapay/ui/home/orders/OrdersRoute.kt")
            .readText()
            .replace("\r\n", "\n")

        assertTrue(
            Regex(
                """onRefresh\s*=\s*\{\s*isRefreshing\s*=\s*true\s*""" +
                    """viewModel\.loadOrders\(forceRefresh\s*=\s*true\)\s*\}"""
            ).containsMatchIn(routeSource)
        )
    }
}
