package com.detrapay.ui.registration.payment_method

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.RegistrationPaymentInstallmentItemBinding
import java.util.Locale

class InstallmentsAdapter(
    private val showRadioButton: Boolean = true,
    private val onSelected: (InstallmentFee) -> Unit
) :
    RecyclerView.Adapter<InstallmentsAdapter.ViewHolder>() {

    private var items: List<InstallmentFee> = emptyList()
    private var selectedPos = -1

    fun submitList(newItems: List<InstallmentFee>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelected(fee: InstallmentFee) {
        selectedPos = items.indexOfFirst { it.installmentNumber == fee.installmentNumber }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RegistrationPaymentInstallmentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val installmentVal = com.detrapay.ui.util.Mask.toSafeDouble(item.installmentValue)
        holder.binding.tvInstallmentName.text = "${item.installmentNumber}X de R$ ${"%,.2f".format(Locale("pt", "BR"), installmentVal)}"
        
        val total = com.detrapay.ui.util.Mask.toSafeDouble(item.totalValue)
        holder.binding.tvInstallmentDescription.text = "TOTAL: R$ ${"%,.2f".format(Locale("pt", "BR"), total)} • ${if (item.noInterest) "SEM JUROS" else "COM JUROS"}"
        
        holder.binding.rbSelected.visibility = if (showRadioButton) View.VISIBLE else View.GONE
        holder.binding.rbSelected.isChecked = position == selectedPos
        
        holder.itemView.setOnClickListener {
            val oldPos = selectedPos
            selectedPos = holder.bindingAdapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPos)
            onSelected(item)
        }
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val binding: RegistrationPaymentInstallmentItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
