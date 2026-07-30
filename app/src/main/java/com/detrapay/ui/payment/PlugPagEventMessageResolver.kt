package com.detrapay.ui.payment

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData

/** Converts PlugPag event codes into stable, user-facing flow instructions. */
object PlugPagEventMessageResolver {
    fun resolve(eventCode: Int, customMessage: String?): String {
        return when (eventCode) {
            PlugPagEventData.EVENT_CODE_WAITING_CARD,
            PlugPagEventData.EVENT_CODE_USE_TARJA,
            PlugPagEventData.EVENT_CODE_USE_CHIP -> "Aproxime ou insira o cartão"

            PlugPagEventData.EVENT_CODE_INSERTED_CARD ->
                "Cartão inserido. Não retire o cartão"

            PlugPagEventData.EVENT_CODE_PIN_REQUESTED,
            PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD -> "Digite a senha"

            PlugPagEventData.EVENT_CODE_PIN_OK ->
                "Senha verificada. Processando pagamento"

            PlugPagEventData.EVENT_CODE_AUTHORIZING ->
                "Autorizando pagamento. Não retire o cartão"

            PlugPagEventData.EVENT_CODE_WAITING_REMOVE_CARD -> "Retire o cartão"

            PlugPagEventData.EVENT_CODE_REMOVED_CARD ->
                "Cartão retirado. Finalizando pagamento"

            PlugPagEventData.EVENT_CODE_SALE_APPROVED ->
                "Pagamento aprovado. Finalizando"

            PlugPagEventData.EVENT_CODE_SALE_NOT_APPROVED ->
                "Pagamento não aprovado. Finalizando"

            else -> customMessage?.trim().orEmpty().ifBlank {
                "Processando pagamento..."
            }
        }
    }
}
