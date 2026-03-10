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
import java.util.Locale

interface OnItemClickListener {
    fun onAdd(item: SimulationPayment)
    fun onDelete(item: SimulationPayment)
    fun onItemUpdated(newItem: SimulationPayment)
    fun onItemClicked(item: SimulationPayment)
}

class RegistrationPaymentMethodRecyclerViewAdapter(
    private val viewModel: RegistrationViewModel,
    private val listener: OnItemClickListener
) : ListAdapter<SimulationPayment, RegistrationPaymentMethodRecyclerViewAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RegistrationPaymentLaunchedItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: RegistrationPaymentLaunchedItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: SimulationPayment) {
            val type = item.paymentMethod.paymentType ?: ""
            val brand = item.paymentMethod.name

            setupBrandUI(type, brand, binding)

            binding.tvMethodName.text = when (type.lowercase()) {
                "credito", "credit" -> "Crédito"
                "debito", "debit" -> "Débito"
                "pix" -> "Pix"
                "dinheiro", "cash" -> "Dinheiro"
                "store_credit" -> "Crédito loja"
                else -> type.lowercase()
                    .replace("pix", "Pix")
                    .replace("store_credit", "Crédito loja")
                    .replace("credit", "Crédito")
                    .replace("debit", "Débito")
                    .replace("cash", "Dinheiro")
                    .replaceFirstChar { it.uppercase() }
            }

            if (
                type.lowercase() == "pix" ||
                type.lowercase() == "dinheiro" ||
                type.lowercase() == "cash" ||
                type.lowercase() == "store_credit"
            ) {
                binding.tvInstallmentDetail.text = "À vista"
                binding.tvInstallmentsInfo.visibility = View.GONE
            } else {
                val installmentValue = item.amountFinal.replace(".", "").replace(",", ".").toDouble() / item.installment
                binding.tvInstallmentDetail.text =
                    "${item.installment}x de R$ ${"%.2f".format(Locale.getDefault(), installmentValue)}"
                binding.tvInstallmentsInfo.text = brand
                binding.tvInstallmentsInfo.visibility = if (isBrandIconSet(brand)) View.GONE else View.VISIBLE
            }

            binding.tvAmount.text = "R$ ${item.amountFinal}"

            binding.btnDelete.setOnClickListener {
                listener.onDelete(item)
            }

            binding.root.setOnClickListener {
                listener.onItemClicked(item)
            }
        }

        private fun isBrandIconSet(brand: String): Boolean {
            val b = brand.lowercase()
            return b.contains("visa") || b.contains("mastercard") || b.contains("elo")
        }

        private fun setupBrandUI(type: String, brand: String, binding: RegistrationPaymentLaunchedItemBinding) {
            val t = type.lowercase()
            val b = brand.lowercase()

            val iconResId = when {
                b.contains("visa") -> R.drawable.ic_visa
                b.contains("mastercard") || b.contains("master") -> R.drawable.ic_mastercard
                b.contains("elo") -> R.drawable.ic_elo
                else -> 0
            }

            if (iconResId != 0) {
                binding.ivIcon.setImageResource(iconResId)
                return
            }

            when {
                t == "pix" -> binding.ivIcon.setImageResource(R.drawable.ic_pix_green)
                t == "dinheiro" || t == "cash" -> binding.ivIcon.setImageResource(R.drawable.ic_money_green)
                t == "credito" || t == "debito" || t == "credit" || t == "debit" || t == "store_credit" -> binding.ivIcon.setImageResource(R.drawable.ic_card_launched)
                else -> binding.ivIcon.setImageResource(R.drawable.ic_article)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SimulationPayment>() {
        override fun areItemsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem == newItem
    }
}
