package com.detrapay.ui.registration.payment_method

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.RegistrationPaymentLaunchedItemBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.util.Mask
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

        private val locale = Locale("pt", "BR")
        private var isInternalUpdate = false
        private var currentTextWatcher: TextWatcher? = null

        fun bind(item: SimulationPayment) {
            val context = binding.root.context
            
            val type = item.paymentMethod.paymentType ?: ""
            setupTypeUI(type, binding)

            binding.tvMethodName.text = item.paymentMethod.paymentType?.uppercase() ?: "PAGAMENTO"
            binding.tvInstallmentsInfo.text = item.paymentMethod.name
            binding.tvInstallmentDetail.text = "${item.installment}X de R$ ${item.amountFinal}" // Simplified for now
            
            binding.tvAmount.text = "R$ ${item.amountOriginal}"

            binding.btnDelete.setOnClickListener {
                listener.onDelete(item)
            }

            binding.root.setOnClickListener {
                listener.onItemClicked(item)
            }
        }

        private fun setupTypeUI(type: String, binding: RegistrationPaymentLaunchedItemBinding) {
            val context = binding.root.context
            when (type.lowercase()) {
                "credito", "debito" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_card_launched)
                }
                "pix" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_pix_green)
                }
                "dinheiro" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_money_green)
                }
                else -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_article)
                }
            }
        }

        private fun calculateAmountFinal(interestTax: Double?, amountStr: String): String {
            return try {
                val value = Mask.doubleValue(amountStr)
                val tax = interestTax ?: 0.0
                val total = value * (1 + tax)
                "%,.2f".format(locale, total)
            } catch (e: Exception) {
                amountStr
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SimulationPayment>() {
        override fun areItemsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SimulationPayment, newItem: SimulationPayment) = oldItem == newItem
    }
}
