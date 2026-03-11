package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus.CANCELLED
import com.detrapay.data.model.OrderReceivableItemStatus.PAID
import com.detrapay.data.model.OrderReceivableItemStatus.PENDING
import com.detrapay.data.model.OrderReceivableItemStatus.REFUNDED
import com.detrapay.data.model.canBeDeleted
import com.detrapay.databinding.OrderPaymentListItemBinding
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class OrderDetailsPaymentsRecyclerViewAdapter(
    private var values: List<OrderReceivableItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderDetailsPaymentsRecyclerViewAdapter.OrderDetailsPaymentsViewHolder>() {

    private val locale = Locale("pt", "BR")
    private val expandedReceivableIds = mutableSetOf<Int>()

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
        private val paymentMethodCard: MaterialCardView = binding.paymentMethodCard
        private val paymentPayButton: MaterialButton = binding.paymentPayButton
        private val paymentDetails: LinearLayout = binding.paymentDetails
        private val paymentRefund: TextView = binding.paymentRefund
        private val paymentRefundDate: TextView = binding.paymentRefundDate
        private val paymentDate: TextView = binding.paymentDate
        private val paymentInfo: TextView = binding.paymentInfo
        private val paymentActionRow: LinearLayout = binding.paymentActionRow
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
            val isCardPayment = isCardPayment(type, brand)
            val isPending = item.status == PENDING
            val canDelete = item.canBeDeleted()
            val isExpandable = item.status == PAID || item.status == REFUNDED || (item.status == PENDING && isManualPayment)
            val isExpanded = expandedReceivableIds.contains(item.id) && isExpandable

            setupBrandUI(type, brand, paymentMethodImage)
            paymentMethodName.text = formatPaymentMethodName(item)
            val (mainAmount, detailAmount) = formatAmount(item, isCreditPayment)
            paymentMethodAmount.text = mainAmount
            paymentMethodInstallmentAmount.text = detailAmount
            paymentMethodInstallmentAmount.visibility = if (detailAmount.isBlank()) View.GONE else View.VISIBLE

            val (statusLabel, statusBackground, statusTextColor) = getStatusUi(item)
            statusTextView.text = statusLabel.uppercase(locale)
            paymentMethodStatusView.background = ContextCompat.getDrawable(context, statusBackground)
            statusTextView.setTextColor(ContextCompat.getColor(context, statusTextColor))

            paymentDeletePending.visibility = if (canDelete) View.VISIBLE else View.GONE
            paymentDeletePending.imageTintList = ContextCompat.getColorStateList(context, R.color.neutral_300)
            paymentDeletePending.setOnClickListener { listener.onDeletePendingClick(item) }

            setupActionArea(item, isPending, isExpandable, isExpanded, listener)
            setupDetailsArea(item, isManualPayment, isCardPayment, isExpanded, listener)
            setupCardClick(item, isPending, isExpandable, listener)
        }

        private fun setupActionArea(
            item: OrderReceivableItem,
            isPending: Boolean,
            isExpandable: Boolean,
            isExpanded: Boolean,
            listener: OnItemClickListener
        ) {
            when {
                isPending -> {
                    paymentPayButton.visibility = View.VISIBLE
                    paymentPayButton.setOnClickListener { listener.onItemClick(item) }
                    paymentActionRow.visibility = View.GONE
                    paymentActionLabel.visibility = View.GONE
                    cardArrow.visibility = View.GONE
                }

                isExpandable -> {
                    paymentPayButton.visibility = View.GONE
                    paymentActionRow.visibility = View.VISIBLE
                    paymentActionLabel.visibility = View.GONE
                    cardArrow.visibility = View.VISIBLE
                    cardArrow.rotation = if (isExpanded) 90f else 0f
                    cardArrow.imageTintList = ContextCompat.getColorStateList(context, R.color.neutral_500)
                }

                else -> {
                    paymentPayButton.visibility = View.GONE
                    paymentActionRow.visibility = View.GONE
                    paymentActionLabel.visibility = View.GONE
                    cardArrow.visibility = View.GONE
                }
            }

            if (item.status == CANCELLED) {
                paymentPayButton.visibility = View.GONE
                paymentActionRow.visibility = View.GONE
                paymentActionLabel.visibility = View.GONE
                cardArrow.visibility = View.GONE
            }
        }

        private fun setupDetailsArea(
            item: OrderReceivableItem,
            isManualPayment: Boolean,
            isCardPayment: Boolean,
            isExpanded: Boolean,
            listener: OnItemClickListener
        ) {
            paymentDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            paymentDate.visibility = View.GONE
            paymentInfo.visibility = View.GONE
            paymentRefundDate.visibility = View.GONE
            paymentRefund.visibility = View.GONE
            paymentRefund.setOnClickListener(null)

            when (item.status) {
                PENDING -> {
                    if (isManualPayment && isExpanded) {
                        paymentInfo.visibility = View.VISIBLE
                        paymentInfo.text = context.getString(R.string.order_details_manual_payment_pending_hint)
                    }
                }

                PAID -> {
                    if (!isExpanded) return

                    item.paymentDate
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            paymentDate.visibility = View.VISIBLE
                            paymentDate.text = context.getString(R.string.order_details_paid_at, it)
                        }

                    if (isCardPayment) {
                        buildCardInfo(item)?.let {
                            paymentInfo.visibility = View.VISIBLE
                            paymentInfo.text = it
                        }
                        paymentRefund.visibility = View.VISIBLE
                        paymentRefund.text = context.getString(R.string.order_details_refund_payment)
                        paymentRefund.setOnClickListener { listener.onRefundClick(item) }
                    }
                }

                REFUNDED -> {
                    if (!isExpanded) return

                    item.paymentDate
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            paymentDate.visibility = View.VISIBLE
                            paymentDate.text = context.getString(R.string.order_details_paid_at, it)
                        }

                    buildCardInfo(item)?.let {
                        paymentInfo.visibility = View.VISIBLE
                        paymentInfo.text = it
                    }

                    item.refundDate
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            paymentRefundDate.visibility = View.VISIBLE
                            paymentRefundDate.text = context.getString(R.string.order_details_refunded_at, it)
                        }
                }

                CANCELLED -> {
                    paymentDetails.visibility = View.GONE
                }
            }
        }

        private fun setupCardClick(
            item: OrderReceivableItem,
            isPending: Boolean,
            isExpandable: Boolean,
            listener: OnItemClickListener
        ) {
            when {
                isPending -> {
                    paymentMethodCard.setOnClickListener { listener.onItemClick(item) }
                }

                isExpandable -> {
                    paymentMethodCard.setOnClickListener {
                        if (expandedReceivableIds.contains(item.id)) {
                            expandedReceivableIds.remove(item.id)
                        } else {
                            expandedReceivableIds.add(item.id)
                        }
                        val position = bindingAdapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            notifyItemChanged(position)
                        }
                    }
                }

                else -> {
                    paymentMethodCard.setOnClickListener(null)
                }
            }
        }

        private fun buildCardInfo(item: OrderReceivableItem): String? {
            val finalDigits = item.cardLast4?.takeIf { it.isNotBlank() }
            val holder = item.cardHolder?.takeIf { it.isNotBlank() }
            return when {
                finalDigits != null && holder != null -> {
                    context.getString(R.string.order_details_card_info_with_holder, finalDigits, holder)
                }

                finalDigits != null -> {
                    context.getString(R.string.order_details_card_info, finalDigits)
                }

                holder != null -> holder
                else -> null
            }
        }

        private fun formatAmount(item: OrderReceivableItem, isCreditPayment: Boolean): Pair<String, String> {
            val amountOriginalFormatted = "%,.2f".format(locale, item.amountOriginal)
            val amountFinalFormatted = "%,.2f".format(locale, item.amountFinal)
            val installments = item.installments.coerceAtLeast(1)
            val installmentAmount = item.amountFinal / installments
            val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)

            return if (isCreditPayment) {
                if (installments > 1) {
                    "R$ $amountFinalFormatted" to "${installments}x de R$ $installmentFormattedValue"
                } else {
                    "R$ $amountFinalFormatted" to ""
                }
            } else {
                if (item.amountOriginal != item.amountFinal) {
                    "R$ $amountFinalFormatted" to "(original R$ $amountOriginalFormatted)"
                } else {
                    "R$ $amountFinalFormatted" to ""
                }
            }
        }

        private fun isManualPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()

            val isCash = normalizedType.contains("dinheiro") ||
                normalizedType.contains("cash") ||
                normalizedBrand.contains("dinheiro")

            val isStoreCredit = normalizedType.contains("credito loja") ||
                normalizedType.contains("store credit") ||
                normalizedType.contains("store_credit") ||
                normalizedBrand.contains("credito loja") ||
                normalizedBrand.contains("store credit")

            return isCash || isStoreCredit
        }

        private fun getStatusUi(item: OrderReceivableItem): Triple<String, Int, Int> {
            return when (item.status) {
                PENDING -> Triple(
                    context.getString(R.string.order_details_status_pending),
                    R.drawable.home_status_pending_background,
                    R.color.home_status_pending_text
                )

                PAID -> Triple(
                    context.getString(R.string.order_details_status_paid),
                    R.drawable.home_status_finished_background,
                    R.color.home_status_finished_text
                )

                CANCELLED -> Triple(
                    context.getString(R.string.order_details_status_cancelled),
                    R.drawable.sales_list_status_cancelled_background,
                    R.color.red
                )

                REFUNDED -> Triple(
                    context.getString(R.string.order_details_status_refunded),
                    R.drawable.home_status_finished_background,
                    R.color.home_status_finished_text
                )
            }
        }

        private fun isCreditPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()
            return normalizedType.contains("credit") ||
                normalizedType.contains("credito") ||
                normalizedBrand.contains("credit") ||
                normalizedBrand.contains("credito")
        }

        private fun isCardPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()
            return normalizedType.contains("credit") ||
                normalizedType.contains("debito") ||
                normalizedType.contains("debit") ||
                normalizedBrand.contains("visa") ||
                normalizedBrand.contains("mastercard") ||
                normalizedBrand.contains("master") ||
                normalizedBrand.contains("elo")
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
                type.contains("pix") || brand.contains("pix") -> "PIX"
                type.contains("dinheiro") || type.contains("cash") || brand.contains("dinheiro") -> "DINHEIRO"
                type.contains("debit") || type.contains("debito") || brand.contains("debito") -> "DEBITO"
                type.contains("credit") || type.contains("credito") || brand.contains("credito") -> "CREDITO"
                else -> item.paymentMethod.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }.uppercase(locale)
            }
        }
    }
}

