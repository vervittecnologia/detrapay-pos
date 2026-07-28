package com.detrapay.ui.home.orders

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersUxContractTest {

    @Test
    fun `payment methods do not style credit as preselected`() {
        val source = File("src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt")
            .readText()

        assertFalse(source.contains("""title == "Crédito""""))
    }

    @Test
    fun `orders exposes labeled primary and simulator actions`() {
        val source = File("src/main/java/com/detrapay/ui/home/orders/screens/OrdersListScreen.kt")
            .readText()

        assertTrue(Regex("""Text\s*\(\s*"Novo pedido"""").containsMatchIn(source))
        assertTrue(Regex("""Text\s*\(\s*"Simular parcelas"""").containsMatchIn(source))
    }
}
