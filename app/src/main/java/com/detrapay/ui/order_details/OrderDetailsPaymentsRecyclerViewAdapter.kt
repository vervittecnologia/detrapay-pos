package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
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
import com.detrapay.ui.util.InstallmentQuotePresenter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class OrderDetailsPaymentsRecyclerViewAdapter(
    private var values: List<OrderReceivableItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderDetailsPaymentsRecyclerViewAdapter.OrderDetailsPaymentsViewHolder>() {

    private val locale = Locale("pt", "BR")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailsPaymentsViewHolder {
        val itemBinding =
            OrderPaymentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderDetailsPaymentsViewHolder(parent.context, itemBinding)
    }

    interface OnItemClickListener {
        fun onItemClick(receivable: OrderReceivableItem)
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
        private val paymentMethodSecondaryInfo: TextView = binding.paymentMethodSecondaryInfo
        private val paymentMethodStatusView: LinearLayout = binding.paymentMethodStatusView
        private val statusTextView: TextView = binding.status
        private val statusIcon: ImageView = binding.statusIcon
        private val paymentMethodCard: MaterialCardView = binding.paymentMethodCard
        private val paymentPayButton: MaterialButton = binding.paymentPayButton
        private val paymentDetails: LinearLayout = binding.paymentDetails
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
            val isCardSummaryPayment = isCardSummaryPayment(type, brand)
            val isPending = item.status == PENDING
            val canDelete = item.canBeDeleted()

            setupBrandUI(type, brand, paymentMethodImage)
            paymentMethodName.text = formatPaymentMethodName(item)

            val (mainAmount, detailAmount, secondaryInfo) = formatAmount(item, isCardSummaryPayment, isManualPayment)
            paymentMethodAmount.text = mainAmount
            paymentMethodInstallmentAmount.text = detailAmount
            paymentMethodInstallmentAmount.visibility = if (detailAmount.isBlank()) View.GONE else View.VISIBLE
            paymentMethodSecondaryInfo.text = secondaryInfo
            paymentMethodSecondaryInfo.visibility = if (secondaryInfo.isBlank()) View.GONE else View.VISIBLE

            val (statusLabel, statusBackground, statusTextColor) = getStatusUi(item)
            statusTextView.text = statusLabel.uppercase(locale)
            paymentMethodStatusView.background = ContextCompat.getDrawable(context, statusBackground)
            statusTextView.setTextColor(ContextCompat.getColor(context, statusTextColor))
            statusIcon.setImageResource(getStatusIcon(item))

            paymentDeletePending.visibility = View.GONE
            paymentDeletePending.setOnClickListener(null)
            paymentDetails.visibility = View.GONE

            setupActionArea(item, isPending, canDelete, listener)
            setupCardClick(item, isPending, listener)
            
            if (isPending && canDelete && item.status != REFUNDED) {
                paymentDeletePending.visibility = View.VISIBLE
                paymentDeletePending.setOnClickListener { listener.onDeletePendingClick(item) }
            } else {
                paymentDeletePending.visibility = View.GONE
                paymentDeletePending.setOnClickListener(null)
            }
        }

        private fun setupActionArea(
            item: OrderReceivableItem,
            isPending: Boolean,
            canDelete: Boolean,
            listener: OnItemClickListener
        ) {
            paymentPayButton.visibility = if (isPending) View.VISIBLE else View.GONE
            if (isPending) {
                paymentPayButton.text = if (isManualPayment(item.paymentMethod.paymentType.orEmpty(), item.paymentMethod.name)) {
                    context.getString(R.string.registration_payment_confirm_receipt_cta)
                } else {
                    context.getString(R.string.registration_payment_pay_now_cta)
                }
                paymentPayButton.setOnClickListener { listener.onItemClick(item) }
            } else {
                paymentPayButton.setOnClickListener(null)
            }

            paymentActionRow.visibility = View.VISIBLE
            paymentActionLabel.visibility = View.GONE
            cardArrow.visibility = View.VISIBLE
            cardArrow.alpha = if (canDelete) 1f else 0.45f

            if (canDelete) {
                setupOverflowMenu(item, listener)
            } else {
                paymentActionRow.setOnClickListener(null)
                cardArrow.setOnClickListener(null)
            }
        }

        private fun setupOverflowMenu(item: OrderReceivableItem, listener: OnItemClickListener) {
            val clickListener = View.OnClickListener { anchor ->
                PopupMenu(context, anchor).apply {
                    menu.add(0, MENU_DELETE_ID, 0, context.getString(R.string.order_details_delete_pending_payment))
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            MENU_DELETE_ID -> {
                                listener.onDeletePendingClick(item)
                                true
                            }

                            else -> false
                        }
                    }
                    show()
                }
            }
            paymentActionRow.setOnClickListener(clickListener)
            cardArrow.setOnClickListener(clickListener)
        }

        private fun setupCardClick(item: OrderReceivableItem, isPending: Boolean, listener: OnItemClickListener) {
            if (isPending) {
                paymentMethodCard.setOnClickListener { listener.onItemClick(item) }
            } else {
                paymentMethodCard.setOnClickListener(null)
            }
        }

        private fun formatAmount(
            item: OrderReceivableItem,
            isCardSummaryPayment: Boolean,
            isManualPayment: Boolean
        ): Triple<String, String, String> {
            val amountOriginalFormatted = "%,.2f".format(locale, item.amountOriginal)
            val amountFinalFormatted = "%,.2f".format(locale, item.amountFinal)
            val installments = item.installments.coerceAtLeast(1)

            return if (isCardSummaryPayment) {
                val presentation = InstallmentQuotePresenter.present(
                    amountOriginal = item.amountOriginal,
                    amountFinal = item.amountFinal,
                    installments = installments,
                )
                Triple(
                    presentation.originalLabel,
                    presentation.installmentLabel,
                    presentation.totalLabel,
                )
            } else if (isManualPayment) {
                val receivedInfo = if (item.status == PAID) "Recebido: R$ $amountFinalFormatted" else ""
                val difference = item.amountFinal - item.amountOriginal
                val secondaryInfo = when {
                    item.status != PAID -> ""
                    difference > 0.0 -> "Troco: R$ %,.2f".format(locale, difference)
                    difference < 0.0 -> "Faltante: R$ %,.2f".format(locale, -difference)
                    else -> "Valor exato ✓"
                }
                Triple("R$ $amountOriginalFormatted", receivedInfo, secondaryInfo)
            } else if (item.amountOriginal != item.amountFinal) {
                Triple("R$ $amountOriginalFormatted", "Total recebido: R$ $amountFinalFormatted", "")
            } else {
                Triple("R$ $amountOriginalFormatted", "", "")
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
                    R.drawable.registration_payment_status_pending_background,
                    R.color.white
                )

                PAID -> Triple(
                    context.getString(R.string.order_details_status_paid),
                    R.drawable.registration_payment_status_paid_background,
                    R.color.white
                )

                CANCELLED -> Triple(
                    context.getString(R.string.order_details_status_cancelled),
                    R.drawable.registration_payment_status_overdue_background,
                    R.color.white
                )

                REFUNDED -> Triple(
                    context.getString(R.string.order_details_status_refunded),
                    R.drawable.registration_payment_status_overdue_background,
                    R.color.white
                )
            }
        }

        private fun getStatusIcon(item: OrderReceivableItem): Int {
            return when (item.status) {
                PENDING -> R.drawable.ic_status_pending_small
                PAID -> R.drawable.ic_status_paid_small
                CANCELLED, REFUNDED -> R.drawable.ic_status_overdue_small
            }
        }

        private fun isCardSummaryPayment(type: String, brand: String): Boolean {
            val normalizedType = type.lowercase()
            val normalizedBrand = brand.lowercase()
            return normalizedType.contains("credit") ||
                normalizedType.contains("credito") ||
                normalizedType.contains("debit") ||
                normalizedType.contains("debito") ||
                normalizedBrand.contains("credit") ||
                normalizedBrand.contains("credito") ||
                normalizedBrand.contains("debit") ||
                normalizedBrand.contains("debito")
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
                binding.paymentIconContainer.setBackgroundResource(R.drawable.registration_payment_icon_credit_background)
                imageView.setImageResource(iconResId)
                imageView.imageTintList = null
                return
            }

            when {
                normalizedType.contains("pix") || normalizedBrand.contains("pix") -> {
                    binding.paymentIconContainer.setBackgroundResource(R.drawable.registration_payment_icon_pix_background)
                    imageView.setImageResource(R.drawable.ic_pix)
                    imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_pix_icon)
                }

                normalizedType.contains("dinheiro") || normalizedType.contains("cash") || normalizedBrand.contains("dinheiro") -> {
                    binding.paymentIconContainer.setBackgroundResource(R.drawable.registration_payment_icon_cash_background)
                    imageView.setImageResource(R.drawable.ic_money)
                    imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_cash_icon)
                }

                normalizedType.contains("debit") || normalizedType.contains("debito") || normalizedBrand.contains("debito") -> {
                    binding.paymentIconContainer.setBackgroundResource(R.drawable.registration_payment_icon_debit_background)
                    imageView.setImageResource(R.drawable.ic_credit_card_outline)
                    imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_debit_icon)
                }

                else -> {
                    binding.paymentIconContainer.setBackgroundResource(R.drawable.registration_payment_icon_credit_background)
                    imageView.setImageResource(R.drawable.ic_credit_card_outline)
                    imageView.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_credit_icon)
                }
            }
        }

        private fun formatPaymentMethodName(item: OrderReceivableItem): String {
            val type = item.paymentMethod.paymentType.orEmpty().lowercase()
            val brand = item.paymentMethod.name.lowercase()

            return when {
                type.contains("pix") || brand.contains("pix") -> "PIX"
                type.contains("dinheiro") || type.contains("cash") || brand.contains("dinheiro") -> "DINHEIRO"
                type.contains("debit") || type.contains("debito") || brand.contains("debito") -> "DÉBITO"
                type.contains("credit") || type.contains("credito") || brand.contains("credito") -> "CRÉDITO"
                else -> item.paymentMethod.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }.uppercase(locale)
            }
        }
    }

    companion object {
        private const val MENU_DELETE_ID = 1
    }
}
