package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.detrapay.data.model.OrderItem
import com.detrapay.databinding.OrderDetailsListItemBinding
import java.util.Locale

class OrderDetailsItemsRecyclerViewAdapter(
    private var values: List<OrderItem>
) : RecyclerView.Adapter<OrderDetailsItemsRecyclerViewAdapter.RegistrationResumeViewHolder>() {

    private val locale = Locale("pt", "BR")

    override fun getItemId(position: Int): Long {
        return values[position].id.toLong()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RegistrationResumeViewHolder {
        val itemBinding = OrderDetailsListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RegistrationResumeViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RegistrationResumeViewHolder, position: Int) {
        val item: OrderItem = values[position]
        holder.bind(item, locale)
    }

    override fun getItemCount(): Int = values.size

    inner class RegistrationResumeViewHolder(binding: OrderDetailsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val itemName: TextView = binding.itemName
        private val itemValue: TextView = binding.itemValue
        private val itemDiscount: TextView = binding.itemDiscount

        @SuppressLint("SetTextI18n")
        fun bind(item: OrderItem, locale: Locale) {
            val price = "%,.2f".format(locale, item.totalPrice)
            itemName.text = item.name
            itemValue.text = "R$ $price"

            if (item.discount > 0) {
                itemDiscount.visibility = View.VISIBLE
                val discount = "%,.2f".format(locale, item.discount)
                itemDiscount.text = "- R$ $discount"
            } else {
                // RecyclerView reuses views; reset discount state when item has no discount.
                itemDiscount.visibility = View.GONE
                itemDiscount.text = ""
            }
        }
    }

}
