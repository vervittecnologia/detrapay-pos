package com.detrapay.ui.payment

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import org.junit.Assert.assertEquals
import org.junit.Test

class PlugPagEventMessageResolverTest {
    @Test
    fun `waiting card event requests the card visibly`() {
        assertEquals(
            "Aproxime ou insira o cartão",
            PlugPagEventMessageResolver.resolve(
                PlugPagEventData.EVENT_CODE_WAITING_CARD,
                "AGUARDE",
            ),
        )
    }

    @Test
    fun `pin events immediately request password`() {
        listOf(
            PlugPagEventData.EVENT_CODE_PIN_REQUESTED,
            PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD,
        ).forEach { eventCode ->
            assertEquals(
                "Digite a senha",
                PlugPagEventMessageResolver.resolve(eventCode, "PROCESSANDO"),
            )
        }
    }

    @Test
    fun `pin confirmation stops showing password prompt`() {
        assertEquals(
            "Senha verificada. Processando pagamento",
            PlugPagEventMessageResolver.resolve(
                PlugPagEventData.EVENT_CODE_PIN_OK,
                "SENHA VERIFICADA",
            ),
        )
    }

    @Test
    fun `remove card event takes precedence over stale processing copy`() {
        assertEquals(
            "Retire o cartão",
            PlugPagEventMessageResolver.resolve(
                PlugPagEventData.EVENT_CODE_WAITING_REMOVE_CARD,
                "PROCESSANDO",
            ),
        )
    }

    @Test
    fun `unknown event preserves PlugPag custom message`() {
        assertEquals(
            "Mensagem do terminal",
            PlugPagEventMessageResolver.resolve(999, "  Mensagem do terminal  "),
        )
    }

    @Test
    fun `sale event waits for final result before claiming approval`() {
        assertEquals(
            "Finalizando transação...",
            PlugPagEventMessageResolver.resolve(PlugPagEventData.EVENT_CODE_SALE_APPROVED, null),
        )
        assertEquals(
            "Processando pagamento...",
            PlugPagEventMessageResolver.resolve(999, "Pagamento aprovado"),
        )
    }

    @Test
    fun `pix events never request a card or announce early approval`() {
        listOf(
            PlugPagEventData.EVENT_CODE_WAITING_CARD,
            PlugPagEventData.EVENT_CODE_INSERTED_CARD,
            PlugPagEventData.EVENT_CODE_AUTHORIZING,
            PlugPagEventData.EVENT_CODE_WAITING_REMOVE_CARD,
        ).forEach { eventCode ->
            assertEquals(
                "Aguardando confirmação do Pix...",
                PlugPagEventMessageResolver.resolve(eventCode, "Retire o cartão", isPix = true),
            )
        }
        assertEquals(
            "Finalizando transação Pix...",
            PlugPagEventMessageResolver.resolve(
                PlugPagEventData.EVENT_CODE_SALE_APPROVED,
                "Pagamento aprovado",
                isPix = true,
            ),
        )
    }
}
