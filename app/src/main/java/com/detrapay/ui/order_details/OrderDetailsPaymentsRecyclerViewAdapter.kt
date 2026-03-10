package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
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
        fun onDeletePendingClick(receivable: OrderReceivableItem)
    }

    override fun onBindViewHolder(holder: OrderDetailsPaymentsViewHolder, position: Int) {
        holder.bind(values[position], listener)
    }

    override fun getItemCount(): Int = values.size

    inner class OrderDetailsPaymentsViewHolder(
        private val context: Context,
        private val binding: OrderPaymentListItemBinding
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
        private val paymentDeletePending: ImageView = binding.paymentDeletePending

        @SuppressLint("SetTextI18n")
        fun bind(item: OrderReceivableItem, listener: OnItemClickListener) {
            val paymentMethod = item.paymentMethod
            val brand = paymentMethod.name
            val type = paymentMethod.paymentType.orEmpty()
            val isManualPayment = isManualPayment(type, brand)
            val isCreditPayment = isCreditPayment(type, brand)

            setupBrandUI(type, brand, paymentMethodImage)

            val isCardPayment = type.contains("credit", true) ||
                type.contains("debit", true) ||
                brand.contains("VISA", true) ||
                brand.contains("Mastercard", true) ||
                brand.contains("ELO", true)

            paymentMethodName.text = formatPaymentMethodName(item)

            val amountOriginalFormatted = "%,.2f".format(locale, item.amountOriginal)
            val amountFinalFormatted = "%,.2f".format(locale, item.amountFinal)
            val installments = item.installments.coerceAtLeast(1)
            val installmentAmount = item.amountFinal / installments
            val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)

            if (isCreditPayment) {
                paymentMethodInstallmentAmount.visibility = View.VISIBLE
                paymentMethodInstallmentAmount.text = "R$ $amountFinalFormatted"
                paymentMethodAmount.text = "${installments}x de R$ $installmentFormattedValue"
            } else {
                paymentMethodInstallmentAmount.visibility = View.VISIBLE
                paymentMethodInstallmentAmount.text = "R$ $amountFinalFormatted"
                paymentMethodAmount.text = "Valor original: R$ $amountOriginalFormatted"
            }

            val (statusLabel, cardBackground, statusTextColor) = getStatusUi(item)
            statusTextView.text = statusLabel
            paymentMethodStatusView.background = ContextCompat.getDrawable(context, cardBackground)
            statusTextView.setTextColor(ContextCompat.getColor(context, statusTextColor))
            paymentDeletePending.visibility = if (item.status == PENDING) View.VISIBLE else View.GONE

            when (item.status) {
                PAID -> {
                    cardArrow.visibility = View.GONE
                    paymentActionLabel.visibility = View.GONE
                    paymentDetails.visibility = View.VISIBLE
                    paymentDate.visibility = View.VISIBLE
                    paymentDate.text = "Pagamento realizado em ${item.paymentDate.orEmpty()}"

                    if (isCardPayment) {
                        paymentRefund.visibility = View.VISIBLE
                        paymentInfo.visibility = View.VISIBLE
                        paymentRefundDate.visibility = View.GONE
                        paymentInfo.text = "Com cartão final ${item.cardLast4.orEmpty()} do titular ${item.cardHolder.orEmpty()}"
                    } else {
                        paymentRefundDate.visibility = View.GONE
                        paymentRefund.visibility = View.GONE
                        paymentInfo.visibility = View.GONE
                    }
                }

                REFUNDED -> {
                    cardArrow.visibility = View.GONE
                    paymentActionLabel.visibility = View.GONE
                    paymentDetails.visibility = View.VISIBLE
                    paymentDate.visibility = View.VISIBLE
                    paymentRefundDate.visibility = View.VISIBLE
                    paymentInfo.visibility = View.VISIBLE
                    paymentRefund.visibility = View.GONE
                    paymentDate.text = "Pagamento realizado em ${item.paymentDate.orEmpty()}"
                    paymentInfo.text = "Com cartão final ${item.cardLast4.orEmpty()} do titular ${item.cardHolder.orEmpty()}"
                    paymentRefundDate.text = "Pagamento estornado em ${item.refundDate.orEmpty()}"
                }

                PENDING -> {
                    if (isManualPayment) {
                        cardArrow.visibility = View.GONE
                        paymentActionLabel.visibility = View.GONE
                        paymentDetails.visibility = View.VISIBLE
                        paymentDate.visibility = View.GONE
                        paymentRefund.visibility = View.GONE
                        paymentRefundDate.visibility = View.GONE
                        paymentInfo.visibility = View.VISIBLE
                        paymentInfo.text = "Pagamentos manuais sao atualizados apos conferencia."
                    } else {
                        cardArrow.visibility = View.VISIBLE
                        paymentActionLabel.visibility = View.VISIBLE
                        paymentDetails.visibility = View.GONE
                    }
                }

                CANCELLED -> {
                    cardArrow.visibility = View.GONE
                    paymentActionLabel.visibility = View.GONE
                    paymentDetails.visibility = View.GONE
                }
            }

            paymentMethodCard.setOnClickListener {
                if (item.status == PENDING && !isManualPayment) {
                    listener.onItemClick(item)
                }
            }

            paymentRefund.setOnClickListener {
                listener.onRefundClick(item)
            }

            paymentDeletePending.setOnClickListener {
                listener.onDeletePendingClick(item)
            }
        }

        private fun isManualPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()

            val isCash = normalizedType.contains("dinheiro") ||
                normalizedType.contains("cash") ||
                normalizedBrand.contains("dinheiro")

            val isStoreCredit = normalizedType.contains("credito loja") ||
                normalizedType.contains("crédito loja") ||
                normalizedType.contains("store credit") ||
                normalizedType.contains("store_credit") ||
                normalizedBrand.contains("credito loja") ||
                normalizedBrand.contains("crédito loja") ||
                normalizedBrand.contains("store credit")

            return isCash || isStoreCredit
        }

        private fun getStatusUi(item: OrderReceivableItem): Triple<String, Int, Int> {
            return when (item.status) {
                PENDING -> Triple("Pendente", R.drawable.home_status_pending_background, R.color.home_status_pending_text)
                PAID -> Triple("Pago", R.drawable.home_status_finished_background, R.color.home_status_finished_text)
                CANCELLED -> Triple("Cancelado", R.drawable.sales_list_status_cancelled_background, R.color.red)
                REFUNDED -> Triple("Estornado", R.drawable.home_status_finished_background, R.color.home_status_finished_text)
            }
        }

        private fun isCreditPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()
            return normalizedType.contains("credit") ||
                normalizedType.contains("credito") ||
                normalizedType.contains("crédito") ||
                normalizedBrand.contains("credit") ||
                normalizedBrand.contains("credito") ||
                normalizedBrand.contains("crédito")
        }

        private fun setupBrandUI(type: String, brand: String, imageView: ImageView) {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()

            val iconResId = when {
                normalizedBrand.contains("visa") -> R.drawable.ic_visa
                normalizedBrand.contains("mastercard") || normalizedBrand.contains("master") -> R.drawable.ic_mastercard
                normalizedBrand.contains("elo") -> R.drawable.ic_elo
                else -> 0
            }

            if (iconResId != 0) {
                imageView.setImageResource(iconResId)
                imageView.imageTintList = null
                return
            }

            val imageDrawable = when {
                normalizedType.contains("pix") || normalizedBrand.contains("pix") -> R.drawable.ic_pix
                normalizedType.contains("dinheiro") || normalizedType.contains("cash") || normalizedBrand.contains("dinheiro") -> R.drawable.ic_money
                else -> R.drawable.ic_credit_card_outline
            }
            imageView.setImageResource(imageDrawable)
            imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.primary_500)
        }

        private fun formatPaymentMethodName(item: OrderReceivableItem): String {
            val type = item.paymentMethod.paymentType.orEmpty().lowercase()
            val brand = item.paymentMethod.name.lowercase()

            return when {
                type.contains("pix") || brand.contains("pix") -> "Pix"
                type.contains("dinheiro") || type.contains("cash") || brand.contains("dinheiro") -> "Dinheiro"
                type.contains("debit") || type.contains("debito") || brand.contains("débito") || brand.contains("debito") -> "Débito"
                type.contains("credit") || type.contains("credito") || brand.contains("crédito") || brand.contains("credito") -> {
                    if (item.installments > 1) {
                        "Crédito ${item.installments}x"
                    } else {
                        "Crédito"
                    }
                }

                else -> item.paymentMethod.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            }
        }
    }
}

