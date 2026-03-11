package com.detrapay.ui.registration.payment_method

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        @SuppressLint("SetTextI18n")
        fun bind(item: SimulationPayment) {
            val type = item.paymentMethod.paymentType ?: ""
            val normalizedType = type.lowercase()

            setupBrandUI(normalizedType)

            val methodLabel = when (normalizedType) {
                "credito", "credit" -> "Crédito"
                "debito", "debit" -> "Débito"
                "pix" -> "Pix"
                "dinheiro", "cash" -> "Dinheiro"
                "store_credit" -> "Crédito loja"
                else -> type.replaceFirstChar { it.uppercase() }
            }

            val methodName = if (normalizedType in listOf("credito", "credit") && item.installment > 1) {
                "$methodLabel ${item.installment}x"
            } else {
                methodLabel
            }
            binding.tvMethodName.text = methodName

            binding.tvAmount.text = "R$ ${item.amountFinal}"
            binding.tvAmountLabel.text = binding.root.context.getString(R.string.order_details_status_pending).uppercase(Locale("pt", "BR"))
            binding.tvInstallmentsInfo.visibility = View.GONE

            val amountFinal = parseAmount(item.amountFinal)
            val amountOriginal = parseAmount(item.amountOriginal)
            val detailText = when {
                item.installment > 1 -> {
                    val installmentValue = amountFinal / item.installment
                    "(${item.installment}x de R$ ${formatAmount(installmentValue)})"
                }
                amountOriginal > 0.0 && amountOriginal != amountFinal -> {
                    "(original R$ ${formatAmount(amountOriginal)})"
                }
                else -> ""
            }
            binding.tvInstallmentDetail.text = detailText
            binding.tvInstallmentDetail.visibility = if (detailText.isBlank()) View.GONE else View.VISIBLE

            binding.btnDelete.setOnClickListener {
                listener.onDelete(item)
            }

            binding.btnPay.setOnClickListener {
                listener.onItemClicked(item)
            }

            binding.root.setOnClickListener {
                listener.onItemClicked(item)
            }
        }

        private fun parseAmount(amount: String): Double {
            return amount
                .replace(".", "")
                .replace(",", ".")
                .toDoubleOrNull() ?: 0.0
        }

        private fun formatAmount(amount: Double): String {
            val numberFormatter = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return numberFormatter.format(amount)
        }

        private fun setupBrandUI(normalizedType: String) {
            when {
                normalizedType == "pix" -> binding.ivIcon.setImageResource(R.drawable.ic_pix_green)
                normalizedType == "dinheiro" || normalizedType == "cash" -> binding.ivIcon.setImageResource(R.drawable.ic_money)
                normalizedType in listOf("credito", "debito", "credit", "debit", "store_credit") -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_card_launched)
                }

                else -> binding.ivIcon.setImageResource(R.drawable.ic_article)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SimulationPayment>() {
        override fun areItemsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem == newItem
    }
}
