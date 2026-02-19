package com.detrapay.ui.registration.payment_method

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.RegistrationPaymentInstallmentItemBinding
import java.util.Locale

class InstallmentsAdapter(private val onSelected: (InstallmentFee) -> Unit) :
    RecyclerView.Adapter<InstallmentsAdapter.ViewHolder>() {

    private var items: List<InstallmentFee> = emptyList()
    private var selectedPos = -1

    fun submitList(newItems: List<InstallmentFee>) {
        items = newItems
        selectedPos = -1
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
        val installmentVal = item.installmentValue.toDouble()
        holder.binding.tvInstallmentName.text = "${item.installmentNumber}X de R$ ${"%,.2f".format(Locale("pt", "BR"), installmentVal)}"
        
        val total = item.totalValue.toDouble()
        holder.binding.tvInstallmentDescription.text = "TOTAL: R$ ${"%,.2f".format(Locale("pt", "BR"), total)} • ${if (item.noInterest) "SEM JUROS" else "COM JUROS"}"
        
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
