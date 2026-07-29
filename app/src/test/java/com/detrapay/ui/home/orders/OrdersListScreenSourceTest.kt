package com.detrapay.ui.home.orders

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersListScreenSourceTest {

    @Test
    fun `orders list mirrors seller visual hierarchy without wallet dashboard`() {
        val sourcePath = Paths.get("src/main/java/com/detrapay/ui/home/orders/screens/OrdersListScreen.kt")
        val source = String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8)

        assertTrue(source.contains("text = \"Pedidos\""))
        assertTrue(source.contains("SellerSearchButton"))
        assertTrue(source.contains("SellerBottomNavigation"))
        assertTrue(source.contains("SellerBottomItem(\"Início\""))
        assertTrue(source.contains("SellerBottomItem(\"Pedidos\""))
        assertTrue(source.contains("SellerBottomItem(\"Perfil\""))
        assertTrue(source.contains("SellerFloatingActionButton"))
        assertTrue(source.contains("RoundedCornerShape(16.dp)"))
        assertTrue(source.contains("text = \"PENDENTE VENDEDOR\""))
        assertTrue(source.contains("SellerExactFontFamily"))
        assertTrue(source.contains("Font(R.font.inter, FontWeight.ExtraBold)"))
        assertTrue(source.contains("SellerMetricsGrid"))
        assertTrue(source.contains(".requiredWidth(148.dp)"))
        assertTrue(source.contains(".heightIn(min = 129.dp)"))

        assertFalse(source.contains("WalletBalanceBlock"))
        assertFalse(source.contains("WalletServicesBlock"))
        assertFalse(source.contains("WalletActionDock"))
        assertFalse(source.contains("Saldo atualizado"))
        assertFalse(source.contains("Ações rápidas"))
    }
}
