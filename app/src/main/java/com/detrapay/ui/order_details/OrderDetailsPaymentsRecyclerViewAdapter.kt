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
import com.detrapay.ui.util.ImageUtils
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
        private val paymentActionLabel: TextView = binding.paymentActionLabel

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(
            item: OrderReceivableItem,
            listener: OnItemClickListener
        ) {
            val paymentMethod = item.paymentMethod
            val brand = paymentMethod.name
            val type = paymentMethod.paymentType ?: ""

            setupBrandUI(type, brand, paymentMethodImage)

            val isCreditCard = type.contains("credit", true) || type.contains("debit", true) ||
                    brand.contains("VISA", true) || brand.contains("Mastercard", true) || brand.contains("ELO", true)

            paymentMethodName.text = paymentMethod.name

            val amountOriginalFormatted = "%,.2f".format(locale, item.amountOriginal)
            val amountFinalFormatted = "%,.2f".format(locale, item.amountFinal)

            if (item.max_installments > 1) {
                val installmentAmount = item.amountFinal / item.max_installments
                val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)

                paymentMethodInstallmentAmount.visibility = View.VISIBLE
                paymentMethodInstallmentAmount.text = "${item.max_installments}x de R$ $installmentFormattedValue"
                paymentMethodAmount.text = "Total com juros: R$ $amountFinalFormatted (Base: R$ $amountOriginalFormatted)"
            } else {
                paymentMethodInstallmentAmount.visibility = View.VISIBLE
                paymentMethodInstallmentAmount.text = "À vista: R$ $amountFinalFormatted"
                paymentMethodAmount.text = "Valor original: R$ $amountOriginalFormatted"
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
                paymentActionLabel.visibility = View.GONE
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
                paymentActionLabel.visibility = View.GONE
                paymentDetails.visibility = View.VISIBLE
                paymentRefundDate.visibility = View.VISIBLE
                paymentInfo.visibility = View.VISIBLE
                paymentRefund.visibility = View.GONE
                paymentDate.text = "Pagamento realizado em ${item.paymentDate}"
                paymentInfo.text = "Com cartão com final ${item.cardLast4} do titular ${item.cardHolder}"
                paymentRefundDate.text = "Pagamento estornado em ${item.refundDate}"
            } else if (item.status == PENDING) {
                cardArrow.visibility = View.VISIBLE
                paymentActionLabel.visibility = View.VISIBLE
                paymentDetails.visibility = View.GONE
            } else {
                cardArrow.visibility = View.GONE
                paymentActionLabel.visibility = View.GONE
                paymentDetails.visibility = View.GONE
            }

            paymentMethodStatusView.background = context.getDrawable(cardBackground)

            paymentMethodCard.setOnClickListener {
                if (item.status == PENDING) listener.onItemClick(item)
            }

            paymentRefund.setOnClickListener{
                listener.onRefundClick(item)
            }

        }

        private fun setupBrandUI(type: String, brand: String, imageView: ImageView) {
            val t = type.lowercase()
            val b = brand.lowercase()

            // Use only local bundled icons
            val iconResId = when {
                b.contains("visa") -> R.drawable.ic_visa
                b.contains("mastercard") || b.contains("master") -> R.drawable.ic_mastercard
                b.contains("elo") -> R.drawable.ic_elo
                else -> 0
            }

            if (iconResId != 0) {
                imageView.setImageResource(iconResId)
                return
            }

            // General Fallback
            val imageDrawable = when {
                t.contains("pix") || b.contains("pix") -> R.drawable.ic_pix
                t.contains("dinheiro") || t.contains("cash") || b.contains("dinheiro") -> R.drawable.ic_money
                else -> R.drawable.ic_credit_card_outline
            }
            imageView.setImageResource(imageDrawable)
        }
    }
}