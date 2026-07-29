package com.detrapay.ui.home.orders

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailScreenSourceTest {

    @Test
    fun `order detail mirrors seller totals and payments cards`() {
        val sourcePath = Paths.get("src/main/java/com/detrapay/ui/home/orders/screens/DetailScreen.kt")
        val source = String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8)
        val fragmentPath = Paths.get("src/main/java/com/detrapay/ui/home/orders/OrdersFragment.kt")
        val fragmentSource = String(Files.readAllBytes(fragmentPath), StandardCharsets.UTF_8)

        assertTrue(source.contains("SellerOrderTotalsCard("))
        assertTrue(source.contains("SellerPaymentsCard("))
        assertTrue(source.contains("SellerOrderPhotosCard("))
        assertTrue(source.contains("SellerPaymentRow("))
        assertTrue(fragmentSource.contains("ActivityResultContracts.TakePicture()"))
        assertTrue(fragmentSource.contains("FileProvider.getUriForFile("))
        assertTrue(source.contains("text = \"Tirar foto\""))
        assertTrue(source.contains("text = \"Documentação\""))
        assertTrue(source.indexOf("SellerPaymentsCard(") < source.indexOf("SellerOrderPhotosCard("))
        assertTrue(source.contains("ZoomablePhotoDialog("))
        assertTrue(source.contains("detectTransformGestures"))
        assertTrue(source.contains("coerceIn(1f, 5f)"))
        assertTrue(source.contains("text = \"Adicionar\""))
        assertTrue(source.contains("text = \"VALOR TOTAL DO PEDIDO\""))
        assertFalse(source.contains("****"))
        assertFalse(source.contains("CardChip("))
        assertFalse(source.contains("DETRAPAY"))
        assertFalse(source.contains("AnalyzerMetricCard("))
        assertFalse(source.contains("OrderSummaryCard("))
    }
}
