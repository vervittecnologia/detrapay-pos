package com.detrapay.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LoggedInUserTest {

    @Test
    fun `active seller mode uses dealership mode instead of legacy user app mode`() {
        val user = loggedInUser(
            companies = listOf(Company(id = 39, name = "CONCESSIONARIA TESTE", sellerAppMode = "standard")),
            appMode = LoggedInUser.APP_MODE_SIMPLIFIED,
        )

        assertEquals(SellerAppMode.COMPLETE, user.activeSellerAppMode)
        assertFalse(user.isSimplifiedMode)
    }

    private fun loggedInUser(
        companies: List<Company>,
        appMode: String,
    ) = LoggedInUser(
        id = "user-1",
        sessionToken = "token",
        displayName = "Usuario",
        username = "usuario@example.com",
        cpfCnpj = "11222333000181",
        email = "usuario@example.com",
        companies = companies,
        dispatchers = emptyList(),
        salesmen = emptyList(),
        appMode = appMode,
    )
}
