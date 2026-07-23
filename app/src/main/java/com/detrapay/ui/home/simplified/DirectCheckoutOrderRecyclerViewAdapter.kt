package com.detrapay.ui.home.simplified

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.data.model.Order
import com.detrapay.databinding.DirectCheckoutOrderListItemBinding

class DirectCheckoutOrderRecyclerViewAdapter(
    private var values: List<Order>,
    private val listener: OnOrderActionListener
) : RecyclerView.Adapter<DirectCheckoutOrderRecyclerViewAdapter.ViewHolder>() {

    private var filteredValues = values.toMutableList()

    interface OnOrderActionListener {
        fun onOrderClick(item: Order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DirectCheckoutOrderListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = filteredValues.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredValues[position], listener)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<Order>) {
        values = newList
        filteredValues = newList.sortedByDescending { it.id }.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterData(text: String) {
        val query = text.trim()
        val digits = query.filter(Char::isDigit)
        filteredValues = values.filter { order ->
            query.isBlank() ||
                order.id.toString().contains(query, ignoreCase = true) ||
                order.customer.name.contains(query, ignoreCase = true) ||
                (digits.isNotBlank() && order.customer.cpfCnpj.contains(digits, ignoreCase = true))
        }.sortedByDescending { it.id }.toMutableList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: DirectCheckoutOrderListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Order, listener: OnOrderActionListener) {
            val summary = DirectCheckoutOrderPresentation.summary(item)

            binding.orderTitle.text = "#${item.id}"
            binding.customerName.text = item.customer.name.ifBlank { "Cliente" }
            binding.pendingAmount.text = DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount)

            binding.orderCard.setOnClickListener { listener.onOrderClick(item) }
        }
    }
}
