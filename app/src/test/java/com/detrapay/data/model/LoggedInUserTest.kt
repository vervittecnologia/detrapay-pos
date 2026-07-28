package com.detrapay.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggedInUserTest {

    @Test
    fun `active seller mode is always direct checkout`() {
        val user = loggedInUser(
            companies = listOf(Company(id = 39, name = "CONCESSIONARIA TESTE", sellerAppMode = "standard")),
        )

        assertEquals(SellerAppMode.DIRECT_CHECKOUT, user.activeSellerAppMode)
    }

    private fun loggedInUser(
        companies: List<Company>,
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
        appMode = LoggedInUser.APP_MODE_DIRECT_CHECKOUT,
    )
}
