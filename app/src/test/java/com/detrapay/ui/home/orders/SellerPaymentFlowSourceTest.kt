package com.detrapay.ui.home.orders

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SellerPaymentFlowSourceTest {

    @Test
    fun `payment flow uses seller components while retaining native keypad`() {
        fun source(name: String): String {
            val path = Paths.get("src/main/java/com/detrapay/ui/home/orders/screens/$name.kt")
            return String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        }

        assertTrue(source("MethodScreen").contains("SellerPaymentMethodCard"))
        assertTrue(source("MethodScreen").contains("Escolha a forma de pagamento"))
        assertTrue(source("KeypadScreen").contains("SellerNativeKeypad"))
        assertTrue(source("KeypadScreen").contains("order.customer.name"))
        assertTrue(source("KeypadScreen").contains("VALOR DO PAGAMENTO"))
        assertTrue(source("InstallmentsScreen").contains("SellerInstallmentCard"))
        assertTrue(source("ReviewScreen").contains("SellerPaymentReviewCard"))
        assertTrue(source("ReviewScreen").contains("Confirmar pagamento"))
    }

    @Test
    fun `payment method screen shows types and pending amount copy`() {
        val path = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/screens/MethodScreen.kt",
        )
        val methodScreen = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val presentationPath = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/OrderPresentation.kt",
        )
        val presentation = String(Files.readAllBytes(presentationPath), StandardCharsets.UTF_8)

        assertTrue(methodScreen.contains("OrderPresentation.paymentMethodTypes(paymentMethods)"))
        assertTrue(methodScreen.contains("Valor pendente"))
        assertTrue(presentation.contains("Transferência Pix"))
        assertTrue(!methodScreen.contains("Saldo disponível"))
    }

    @Test
    fun `amount screen uses clean numeric keypad and single uppercase action`() {
        val path = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/screens/KeypadScreen.kt",
        )
        val keypad = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val nativeKeypad = keypad.substringAfter("private fun SellerNativeKeypad")

        assertTrue(keypad.contains("paymentMethodTypeLabel"))
        assertTrue(keypad.contains("order.customer.name"))
        assertTrue(keypad.contains("VALOR DO PAGAMENTO"))
        assertTrue(keypad.contains("text = \"CONTINUAR\""))
        assertFalse(keypad.contains("Revisar as condições do pagamento"))
        assertFalse(keypad.contains("Teclado da maquininha"))
        assertFalse(keypad.contains("Informe o valor e toque em Continuar"))
        assertFalse(keypad.contains(".height(170.dp)"))
        assertFalse(nativeKeypad.contains("\"ABC\""))
        assertFalse(nativeKeypad.contains("border = BorderStroke"))
    }

    @Test
    fun `credit quote displays animated installment consultation feedback`() {
        val path = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/screens/InstallmentsScreen.kt",
        )
        val installments = String(Files.readAllBytes(path), StandardCharsets.UTF_8)

        assertTrue(installments.contains("CircularProgressIndicator"))
        assertTrue(installments.contains("Buscando melhores condições"))
        assertTrue(installments.contains("Consultando as opções de parcelamento..."))
        assertTrue(installments.contains("Tentar novamente"))
    }

    @Test
    fun `installments are compact with pinned continue and wizard close action`() {
        val installmentsPath = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/screens/InstallmentsScreen.kt",
        )
        val installments = String(Files.readAllBytes(installmentsPath), StandardCharsets.UTF_8)
        val blocksPath = Paths.get(
            "src/main/java/com/detrapay/ui/home/orders/components/OrderFlowBlocks.kt",
        )
        val blocks = String(Files.readAllBytes(blocksPath), StandardCharsets.UTF_8)

        assertTrue(installments.contains("bottomBar ="))
        assertTrue(installments.contains("NavBar(\"Parcelamento\", onBack, onClose)"))
        assertTrue(blocks.contains("contentDescription = \"Sair do pagamento\""))
        assertTrue(blocks.contains("por parcela"))
        val installmentRow = blocks
            .substringAfter("fun InstallmentRow")
            .substringBefore("fun Metric")
        assertFalse(installmentRow.contains("presentation.originalLabel"))
    }
}
