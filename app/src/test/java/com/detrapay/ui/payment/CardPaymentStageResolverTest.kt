package com.detrapay.ui.payment

import com.detrapay.data.model.PaymentData
import com.detrapay.ui.state.UIState
import org.junit.Assert.assertEquals
import org.junit.Test

class CardPaymentStageResolverTest {
    @Test
    fun `preparing message maps to starting`() {
        assertEquals(
            CardPaymentStage.STARTING,
            CardPaymentStageResolver.resolve(UIState.Loading("Aguarde, preparando a maquininha.")),
        )
    }

    @Test
    fun `terminal card prompts map to present card`() {
        listOf(
            "Aproxime ou insira seu cartão",
            "INSIRA O CARTAO",
            "Passe o cartão",
        ).forEach { message ->
            assertEquals(
                CardPaymentStage.PRESENT_CARD,
                CardPaymentStageResolver.resolve(UIState.Loading(message)),
            )
        }
    }

    @Test
    fun `password prompts map to enter pin`() {
        assertEquals(
            CardPaymentStage.ENTER_PIN,
            CardPaymentStageResolver.resolve(UIState.Loading("Digite a senha")),
        )
        assertEquals(
            CardPaymentStage.ENTER_PIN,
            CardPaymentStageResolver.resolve(UIState.Loading("INFORME O PIN")),
        )
    }

    @Test
    fun `verified password maps back to processing`() {
        assertEquals(
            CardPaymentStage.PROCESSING,
            CardPaymentStageResolver.resolve(UIState.Loading("SENHA VERIFICADA")),
        )
    }

    @Test
    fun `remove card prompts map to remove card`() {
        listOf("RETIRE O CARTÃO", "Remova o cartao").forEach { message ->
            assertEquals(
                CardPaymentStage.REMOVE_CARD,
                CardPaymentStageResolver.resolve(UIState.Loading(message)),
            )
        }
    }

    @Test
    fun `generic terminal work maps to processing`() {
        assertEquals(
            CardPaymentStage.PROCESSING,
            CardPaymentStageResolver.resolve(UIState.Loading("Processando transação...")),
        )
    }

    @Test
    fun `final states map to approved and declined`() {
        assertEquals(
            CardPaymentStage.APPROVED,
            CardPaymentStageResolver.resolve(UIState.Success(PaymentData())),
        )
        assertEquals(
            CardPaymentStage.DECLINED,
            CardPaymentStageResolver.resolve(UIState.Error("Transação recusada")),
        )
    }

    @Test
    fun `communication timeout is not presented as bank decline`() {
        assertEquals(
            "Pagamento não concluído",
            CardPaymentErrorPresenter.title("A011 - Tempo de resposta excedido"),
        )
        assertEquals(
            "Pagamento não concluído",
            CardPaymentErrorPresenter.title("Problema na comunicação, tente novamente"),
        )
    }

    @Test
    fun `actual denial is presented as declined`() {
        assertEquals(
            "Pagamento recusado",
            CardPaymentErrorPresenter.title("Transação negada pelo emissor"),
        )
    }
}
