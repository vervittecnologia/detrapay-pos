package com.detrapay.ui.order_details

import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData

data class RetryDataModel (val receivableItem: OrderReceivableItem, val paymentData: PaymentData )