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
        assertFalse(source.contains("SellerBottomNavigation"))
        assertFalse(source.contains("SellerFloatingActionButton"))
        assertTrue(source.contains("SellerNewOrderButton"))
        assertTrue(source.contains("text = \"Novo pedido\""))
        assertTrue(source.contains("contentDescription = \"Abrir perfil\""))
        assertTrue(source.contains("onSectionSelected(SellerHomeSection.Profile)"))
        assertTrue(source.contains(".clickable(onClick = onClick)"))
        assertTrue(source.contains("RoundedCornerShape(16.dp)"))
        assertTrue(source.contains("text = status.uppercase()"))
        assertTrue(source.contains("text = card.paymentStatusLabel"))
        assertTrue(source.contains("SellerExactFontFamily"))
        assertTrue(source.contains("DetrapayFontFamily"))
        assertTrue(source.contains("SellerMetricsGrid"))
        assertTrue(source.contains(".widthIn(min = 112.dp)"))
        assertTrue(source.contains(".heightIn(min = 120.dp)"))

        val profileSource = String(
            Files.readAllBytes(Paths.get("src/main/java/com/detrapay/ui/home/orders/screens/SellerProfileScreen.kt")),
            StandardCharsets.UTF_8,
        )
        assertFalse(profileSource.contains("SellerBottomNavigation"))
        assertTrue(profileSource.contains("contentDescription = \"Voltar para pedidos\""))

        assertFalse(source.contains("WalletBalanceBlock"))
        assertFalse(source.contains("WalletServicesBlock"))
        assertFalse(source.contains("WalletActionDock"))
        assertFalse(source.contains("Saldo atualizado"))
        assertFalse(source.contains("Ações rápidas"))
    }
}
