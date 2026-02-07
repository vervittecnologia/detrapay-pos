package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus.CANCELLED
import com.detrapay.data.model.OrderReceivableItemStatus.PAID
import com.detrapay.data.model.OrderReceivableItemStatus.PENDING
import com.detrapay.data.model.OrderReceivableItemStatus.REFUNDED
import com.detrapay.databinding.OrderPaymentListItemBinding
import java.util.Locale

class OrderDetailsPaymentsRecyclerViewAdapter(
    private var values: List<OrderReceivableItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderDetailsPaymentsRecyclerViewAdapter.OrderDetailsPaymentsViewHolder>() {

    private val locale = Locale("pt", "BR")

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderDetailsPaymentsViewHolder {
        val itemBinding =
            OrderPaymentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderDetailsPaymentsViewHolder(parent.context, itemBinding)
    }

    interface OnItemClickListener {
        fun onItemClick(receivable: OrderReceivableItem)
        fun onRefundClick(receivable: OrderReceivableItem)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: OrderDetailsPaymentsViewHolder, position: Int) {
        val item = values[position]
        holder.bind(item, listener)
    }

    override fun getItemCount(): Int = values.size

    inner class OrderDetailsPaymentsViewHolder(
        val context: Context,
        val binding: OrderPaymentListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val paymentMethodImage: ImageView = binding.paymentMethodImage
        private val paymentMethodName: TextView = binding.paymentMethodName
        private val paymentMethodAmount: TextView = binding.paymentMethodAmount
        private val paymentMethodInstallmentAmount: TextView = binding.paymentMethodInstallmentAmount

        private val paymentMethodStatusView: LinearLayout = binding.paymentMethodStatusView
        private val statusTextView: TextView = binding.status

        private val paymentMethodCard: CardView = binding.paymentMethodCard
        private val paymentDetails: LinearLayout = binding.paymentDetails
        private val paymentRefund: TextView = binding.paymentRefund
        private val paymentRefundDate: TextView = binding.paymentRefundDate
        private val paymentDate: TextView = binding.paymentDate
        private val paymentInfo: TextView = binding.paymentInfo
        private val cardArrow: ImageView = binding.cardArrow

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(
            item: OrderReceivableItem,
            listener: OnItemClickListener
        ) {
            val isCreditCard =  item.paymentMethod.name.contains("Crédito", true) ||
                    item.paymentMethod.name.contains("Débito", true) ||
                    item.paymentMethod.name.contains("Cartão de crédito", true) ||
                    item.paymentMethod.name.contains("VISA", true) ||
                    item.paymentMethod.name.contains("Mastercard", true)
            val imageDrawable = if (isCreditCard) {
                R.drawable.ic_credit_card_outline
            } else if (item.paymentMethod.name.contains("Pix", true)) {
                R.drawable.ic_pix
            } else {
                R.drawable.ic_money
            }

            paymentMethodImage.setImageDrawable(context.getDrawable(imageDrawable))

            paymentMethodName.text = item.paymentMethod.name

            if (isCreditCard) {
                paymentMethodInstallmentAmount.visibility = View.VISIBLE
                
                val amountOriginalFormatted = "%,.2f".format(locale, item.amountOriginal)
                val amountFinalFormatted = "%,.2f".format(locale, item.amountFinal)
                val installmentAmount = item.amountFinal / item.installments
                val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)
                
                paymentMethodAmount.text = "R$ $amountOriginalFormatted em ${item.installments}x de R$ $installmentFormattedValue (R$ $amountFinalFormatted)"
            } else {
                paymentMethodInstallmentAmount.visibility = View.GONE
                val amount = "%,.2f".format(locale, item.amountOriginal)
                paymentMethodAmount.text = "R$ $amount"
            }

            statusTextView.text = item.status.toString()

            val cardBackground = when (item.status) {
                PENDING -> R.drawable.pending_status_background
                PAID -> R.drawable.paid_status_background
                CANCELLED -> R.drawable.cancelled_status_background
                REFUNDED -> R.drawable.refunded_status_background
            }

            if (item.status == PAID ) {
                cardArrow.visibility = View.GONE
                paymentDetails.visibility = View.VISIBLE
                paymentDate.text = "Pagamento realizado em ${item.paymentDate}"

                if (isCreditCard) {
                    paymentRefund.visibility = View.VISIBLE
                    paymentInfo.visibility = View.VISIBLE
                    paymentRefundDate.visibility = View.GONE
                    paymentInfo.text = "Com cartão com final ${item.cardLast4} do titular ${item.cardHolder}"
                } else {
                    paymentRefundDate.visibility = View.GONE
                    paymentRefund.visibility = View.GONE
                    paymentInfo.visibility = View.GONE
                }
            } else if (item.status == REFUNDED) {
                cardArrow.visibility = View.GONE
                paymentDetails.visibility = View.VISIBLE
                paymentRefundDate.visibility = View.VISIBLE
                paymentInfo.visibility = View.VISIBLE
                paymentRefund.visibility = View.GONE
                paymentDate.text = "Pagamento realizado em ${item.paymentDate}"
                paymentInfo.text = "Com cartão com final ${item.cardLast4} do titular ${item.cardHolder}"
                paymentRefundDate.text = "Pagamento estornado em ${item.refundDate}"
            } else {
                cardArrow.visibility = View.VISIBLE
                paymentDetails.visibility = View.GONE
            }

            paymentMethodStatusView.background = context.getDrawable(cardBackground)

            paymentMethodCard.setOnClickListener {
                if (item.status != PAID && item.status != REFUNDED && item.status != CANCELLED) listener.onItemClick(item)
            }

            paymentRefund.setOnClickListener{
                listener.onRefundClick(item)
            }

        }

    }

}