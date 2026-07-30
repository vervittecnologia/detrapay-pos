package com.detrapay.ui.payment

import com.detrapay.data.model.PaymentData
import com.detrapay.ui.state.UIState
import java.text.Normalizer

enum class CardPaymentStage {
    STARTING,
    PRESENT_CARD,
    PROCESSING,
    ENTER_PIN,
    REMOVE_CARD,
    APPROVED,
    DECLINED,
}

object CardPaymentStageResolver {
    fun resolve(state: UIState<PaymentData>): CardPaymentStage = when (state) {
        is UIState.Success -> CardPaymentStage.APPROVED
        is UIState.Error -> CardPaymentStage.DECLINED
        is UIState.Idle -> CardPaymentStage.STARTING
        is UIState.Loading -> resolveLoadingMessage(state.message)
    }

    internal fun resolveLoadingMessage(message: String?): CardPaymentStage {
        val normalized = normalize(message.orEmpty())
        return when {
            normalized.contains("retire o cartao") ||
                normalized.contains("remova o cartao") -> CardPaymentStage.REMOVE_CARD

            normalized.contains("senha verificada") ||
                normalized.contains("pin verificado") -> CardPaymentStage.PROCESSING

            normalized.contains("digite a senha") ||
                normalized.contains("informe a senha") ||
                normalized.contains("digite o pin") ||
                normalized.contains("informe o pin") ->
                CardPaymentStage.ENTER_PIN

            normalized.contains("aproxim") ||
                normalized.contains("insira") ||
                normalized.contains("passe o cartao") -> CardPaymentStage.PRESENT_CARD

            normalized.contains("inici") ||
                normalized.contains("conect") ||
                normalized.contains("prepar") -> CardPaymentStage.STARTING

            else -> CardPaymentStage.PROCESSING
        }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
}

object CardPaymentErrorPresenter {
    fun title(message: String?): String {
        val normalized = Normalizer
            .normalize(message.orEmpty().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")

        return when {
            normalized.contains("recus") ||
                normalized.contains("negad") ||
                normalized.contains("declin") -> "Pagamento recusado"

            normalized.contains("tempo de resposta") ||
                normalized.contains("comunicacao") ||
                normalized.contains("conexao") ||
                normalized.contains("timeout") -> "Pagamento não concluído"

            else -> "Falha no pagamento"
        }
    }
}
