package com.detrapay.ui.registration.payment_method

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.RegistrationPaymentLaunchedItemBinding
import com.detrapay.ui.registration.RegistrationViewModel
import java.text.NumberFormat
import java.util.Locale

interface OnItemClickListener {
    fun onAdd(item: SimulationPayment)
    fun onDelete(item: SimulationPayment)
    fun onItemUpdated(newItem: SimulationPayment)
    fun onItemClicked(item: SimulationPayment)
}

class RegistrationPaymentMethodRecyclerViewAdapter(
    private val viewModel: RegistrationViewModel,
    private val listener: OnItemClickListener,
) : ListAdapter<SimulationPayment, RegistrationPaymentMethodRecyclerViewAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RegistrationPaymentLaunchedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: RegistrationPaymentLaunchedItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val locale = Locale("pt", "BR")

        fun bind(item: SimulationPayment) {
            val type = item.paymentMethod.paymentType.orEmpty()
            val normalizedType = type.lowercase(locale)

            setupBrandUI(normalizedType)
            bindStatus()
            bindTexts(item, normalizedType)

            binding.btnDelete.setOnClickListener { listener.onDelete(item) }
            binding.btnPay.setOnClickListener { listener.onItemClicked(item) }
            binding.root.setOnClickListener { listener.onItemClicked(item) }
        }

        private fun bindStatus() {
            binding.statusContainer.background = ContextCompat.getDrawable(
                binding.root.context,
                R.drawable.registration_payment_status_pending_background,
            )
            binding.ivStatusIcon.setImageResource(R.drawable.ic_status_pending_small)
            binding.tvAmountLabel.text = binding.root.context.getString(R.string.order_details_status_pending)
            binding.tvAmountLabel.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
        }

        private fun bindTexts(item: SimulationPayment, normalizedType: String) {
            binding.tvMethodName.text = formatMethodName(normalizedType)

            val amount = parseAmount(item.amountFinal)
            binding.tvAmount.text = formatCurrency(amount)

            val detailText = buildInstallmentText(item)
            if (detailText == null) {
                binding.tvInstallmentDetail.visibility = View.GONE
            } else {
                binding.tvInstallmentDetail.visibility = View.VISIBLE
                binding.tvInstallmentDetail.text = detailText
            }

            binding.btnPay.text = when {
                normalizedType == "dinheiro" || normalizedType == "cash" || normalizedType == "store_credit" ->
                    binding.root.context.getString(R.string.registration_payment_confirm_receipt_cta)

                else -> binding.root.context.getString(R.string.registration_payment_pay_now_cta)
            }
        }

        private fun buildInstallmentText(item: SimulationPayment): CharSequence? {
            val amount = parseAmount(item.amountFinal)
            return when {
                item.installment > 1 -> {
                    val prefix = "${item.installment}x de "
                    val amountText = formatCurrency(amount / item.installment)
                    val builder = SpannableStringBuilder(prefix + amountText)
                    builder.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.neutral_500)),
                        0,
                        prefix.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    builder.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.neutral_900)),
                        prefix.length,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    builder.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD),
                        prefix.length,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    builder
                }

                parseAmount(item.amountOriginal) != amount -> {
                    val prefix = "Total final "
                    val amountText = formatCurrency(amount)
                    SpannableStringBuilder(prefix + amountText).apply {
                        setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.neutral_500)),
                            0,
                            prefix.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.neutral_900)),
                            prefix.length,
                            length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        setSpan(
                            StyleSpan(android.graphics.Typeface.BOLD),
                            prefix.length,
                            length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }

                else -> null
            }
        }

        private fun formatMethodName(normalizedType: String): String {
            return when (normalizedType) {
                "credito", "credit" -> "CREDITO"
                "debito", "debit" -> "DEBITO"
                "pix" -> "PIX"
                "dinheiro", "cash" -> "DINHEIRO"
                "store_credit" -> "CREDITO LOJA"
                else -> normalizedType.uppercase(locale)
            }
        }

        private fun parseAmount(amount: String): Double {
            return amount
                .replace(".", "")
                .replace(",", ".")
                .toDoubleOrNull() ?: 0.0
        }

        private fun formatCurrency(amount: Double): String {
            val numberFormatter = NumberFormat.getCurrencyInstance(locale)
            return numberFormatter.format(amount)
        }

        private fun setupBrandUI(normalizedType: String) {
            val context = binding.root.context
            when (normalizedType) {
                "pix" -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.registration_payment_icon_pix_background)
                    binding.ivIcon.setImageResource(R.drawable.ic_pix)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_pix_icon)
                }

                "dinheiro", "cash", "store_credit" -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.registration_payment_icon_cash_background)
                    binding.ivIcon.setImageResource(R.drawable.ic_money)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_cash_icon)
                }

                "debito", "debit" -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.registration_payment_icon_debit_background)
                    binding.ivIcon.setImageResource(R.drawable.ic_credit_card_outline)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_debit_icon)
                }

                else -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.registration_payment_icon_credit_background)
                    binding.ivIcon.setImageResource(R.drawable.ic_credit_card_outline)
                    binding.ivIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.payment_credit_icon)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SimulationPayment>() {
        override fun areItemsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem == newItem
    }
}
