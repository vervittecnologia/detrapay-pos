package com.detrapay.ui.registration.payment_method

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.RegistrationPaymentInstallmentItemBinding
import com.detrapay.ui.util.Mask
import java.util.Locale

class InstallmentsAdapter(
    private val showRadioButton: Boolean = true,
    private val onSelected: (InstallmentFee) -> Unit,
) : RecyclerView.Adapter<InstallmentsAdapter.ViewHolder>() {

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

    fun clearSelection() {
        selectedPos = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RegistrationPaymentInstallmentItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val installmentValue = Mask.toSafeDouble(item.installmentValue)
        val totalValue = Mask.toSafeDouble(item.totalValue)
        val isSelected = position == selectedPos

        holder.binding.tvInstallmentName.text =
            "${item.installmentNumber}x de R$ ${"%,.2f".format(Locale("pt", "BR"), installmentValue)}"
        holder.binding.tvInstallmentDescription.text =
            "Total R$ ${"%,.2f".format(Locale("pt", "BR"), totalValue)}${if (item.noInterest) "" else " com juros"}"
        holder.binding.rbSelected.visibility = if (showRadioButton) View.VISIBLE else View.GONE
        holder.binding.rbSelected.isChecked = isSelected
        holder.binding.root.strokeColor = ContextCompat.getColor(
            holder.itemView.context,
            if (isSelected) R.color.primary_500 else R.color.neutral_300,
        )
        holder.binding.root.setCardBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.primary_100 else R.color.white,
            ),
        )

        holder.itemView.setOnClickListener {
            val oldPos = selectedPos
            selectedPos = holder.bindingAdapterPosition
            if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos)
            notifyItemChanged(selectedPos)
            onSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: RegistrationPaymentInstallmentItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
