package com.detrapay.ui.registration

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderCreationNavigationContractTest {

    @Test
    fun `creating an order never presents payment success before a payment`() {
        val registrationSources = File("src/main/java/com/detrapay/ui/registration")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(registrationSources.contains("putExtra(\"isSuccess\", true)"))
        assertTrue(registrationSources.contains("putExtra(\"isSuccess\", false)"))
    }
}
