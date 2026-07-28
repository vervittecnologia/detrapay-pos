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

    @Test
    fun `installment choices expose selectable semantics`() {
        val blocks = File(
            "src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt",
        ).readText()
        val simulator = File(
            "src/main/java/com/detrapay/ui/home/orders/screens/InstallmentSimulatorScreen.kt",
        ).readText()

        assertTrue(blocks.contains(".selectable("))
        assertTrue(simulator.contains(".selectable("))
        assertTrue(blocks.contains("Role.RadioButton"))
        assertTrue(simulator.contains("Role.RadioButton"))
    }

    @Test
    fun `waiting state avoids decorative infinite animation`() {
        val source = File(
            "src/main/java/com/detrapay/ui/home/orders/screens/WaitingScreen.kt",
        ).readText()

        assertFalse(source.contains("rememberInfiniteTransition"))
        assertTrue(source.contains("LiveRegionMode.Polite"))
        assertTrue(source.contains("LiveRegionMode.Assertive"))
    }

    @Test
    fun `small informational text uses accessible neutral token`() {
        val source = File(
            "src/main/java/com/detrapay/ui/home/orders/components/OrderFlowColors.kt",
        ).readText()

        assertTrue(source.contains("val Faint = Color(0xFF475569)"))
    }

    @Test
    fun `physical back and order payment action remain explicit`() {
        val screen = File(
            "src/main/java/com/detrapay/ui/home/orders/OrdersScreen.kt",
        ).readText()
        val list = File(
            "src/main/java/com/detrapay/ui/home/orders/screens/OrdersListScreen.kt",
        ).readText()

        assertTrue(screen.contains("OnBackPressedCallback("))
        assertTrue(list.contains("onPay = { onOrderPay(order) }"))
        assertTrue(list.contains("""Text("Pagar agora""""))
    }
}
