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
            
            // Icon and Name based on Type
            val type = item.paymentMethod.paymentType ?: ""
            setupTypeUI(type, binding)

            binding.tvId.text = "LANÇAMENTO #${item.id.toString().takeLast(4)}"
            
            val tax = item.paymentMethod.interestTax ?: 0.0
            binding.tvTax.text = "Taxa: ${"%.2f".format(tax * 100)}%"
            binding.tvTax.visibility = if (tax > 0) android.view.View.VISIBLE else android.view.View.GONE

            binding.tvAmountFinal.text = "Total c/ juros: R$ ${item.amountFinal}"
            binding.tvAmountFinal.visibility = if (tax > 0) android.view.View.VISIBLE else android.view.View.GONE

            // Amount Mask
            isInternalUpdate = true
            binding.etAmount.setText(item.amountOriginal)
            isInternalUpdate = false

            currentTextWatcher?.let { binding.etAmount.removeTextChangedListener(it) }
            
            currentTextWatcher = Mask.moneyMask(binding.etAmount) { stringValue ->
                if (isInternalUpdate) return@moneyMask
                val newItem = item.copy(
                    amountOriginal = stringValue,
                    amountFinal = calculateAmountFinal(item.paymentMethod.interestTax, stringValue)
                )
                listener.onItemUpdated(newItem)
            }
            binding.etAmount.addTextChangedListener(currentTextWatcher)

            binding.etAmount.setOnClickListener {
                binding.etAmount.setSelection(binding.etAmount.text?.length ?: 0)
            }

            binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.etAmount.setSelection(binding.etAmount.text?.length ?: 0)
                }
            }

            // Installments Dropdown
            val methods = viewModel.getPaymentMethodsByType(type)
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                methods.map { it.name }
            )
            binding.atvInstallments.setAdapter(adapter)
            binding.atvInstallments.setText(item.paymentMethod.name, false)

            binding.atvInstallments.setOnItemClickListener { _, _, pos, _ ->
                val newMethod = methods[pos]
                val newItem = item.copy(
                    paymentMethod = newMethod,
                    installment = newMethod.maxInstallments,
                    amountFinal = calculateAmountFinal(newMethod.interestTax, binding.etAmount.text.toString())
                )
                listener.onItemUpdated(newItem)
            }

            binding.btnDelete.setOnClickListener {
                listener.onDelete(item)
            }
        }

        private fun setupTypeUI(type: String, binding: RegistrationPaymentLaunchedItemBinding) {
            val context = binding.root.context
            when (type.lowercase()) {
                "credito" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_credit_card_outline)
                    binding.ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_500)
                    binding.tvMethodName.text = "CARTÃO DE CRÉDITO"
                    binding.tilInstallments.visibility = android.view.View.VISIBLE
                    binding.lblParcelas.visibility = android.view.View.VISIBLE
                }
                "debito" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_credit_card_outline)
                    binding.ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_400)
                    binding.tvMethodName.text = "CARTÃO DE DÉBITO"
                    binding.tilInstallments.visibility = android.view.View.GONE
                    binding.lblParcelas.visibility = android.view.View.GONE
                }
                "pix" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_pix)
                    binding.ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
                    binding.tvMethodName.text = "PIX"
                    binding.tilInstallments.visibility = android.view.View.GONE
                    binding.lblParcelas.visibility = android.view.View.GONE
                }
                "dinheiro" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_money)
                    binding.ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
                    binding.tvMethodName.text = "DINHEIRO"
                    binding.tilInstallments.visibility = android.view.View.GONE
                    binding.lblParcelas.visibility = android.view.View.GONE
                }
                else -> {
                    binding.tvMethodName.text = type.uppercase()
                    binding.ivIcon.setImageResource(R.drawable.ic_article)
                    binding.ivIcon.backgroundTintList = ContextCompat.getColorStateList(context, R.color.neutral_500)
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
