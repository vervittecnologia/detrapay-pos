package com.detrapay.ui.home.order_list

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.OrderStatus.CANCELLED
import com.detrapay.data.model.OrderStatus.PAID
import com.detrapay.data.model.OrderStatus.PENDING
import com.detrapay.data.model.OrderStatus.AUTHORIZED
import com.detrapay.data.model.OrderStatus.COMPLETED
import com.detrapay.databinding.OrderListItemBinding
import java.util.Locale

class OrderRecyclerViewAdapter(
    private var values: List<Order>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderRecyclerViewAdapter.OrderViewHolder>() {


    private var filteredValues: MutableList<Order> = values.sortedByDescending { it.id }.toMutableList()
    private val locale = Locale("pt", "BR")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val itemBinding =
            OrderListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(parent.context, itemBinding)
    }

    interface OnItemClickListener {
        fun onItemClick(item: Order)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<Order>) {
        this.values = newList
        this.filteredValues = newList.sortedByDescending { it.id }.toMutableList()
        notifyDataSetChanged()
    }

    private fun stringToFormattedDate(
        date: String,
    ): String {
        val day = date.substring(8, 10)
        val month = date.substring(5, 7)
        val year = date.substring(0, 4)
        return "$day/$month/$year"
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item: Order = filteredValues[position]
        holder.bind(item, listener)
    }

    override fun getItemCount(): Int = filteredValues.size

    @SuppressLint("NotifyDataSetChanged")
    fun filterData(text: String) {
        filteredValues = values.filter {
            it.customer.name.contains(text, true) || it.customer.cpfCnpj.contains(text, true)
        }.sortedByDescending { it.id }.toMutableList()
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(val context: Context, val binding: OrderListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val clientNameView: TextView = binding.clientName
        private val serviceNameView: TextView = binding.serviceName
        private val serviceDateView: TextView = binding.serviceDate
        private val statusTextView: TextView = binding.status
        private val statusView: LinearLayout = binding.statusView
        private val orderCard: CardView = binding.orderCard

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(
            item: Order,
            listener: OnItemClickListener
        ) {
            val totalReceived = item.receivables
                .filter { it.status == OrderReceivableItemStatus.PAID }
                .sumOf { it.amountOriginal }

            val totalPending = item.originalAmount - totalReceived

            val nominalValueFormatted = "%,.2f".format(locale, item.originalAmount)
            val pendingValueFormatted = "%,.2f".format(locale, totalPending)

            clientNameView.text = "#${item.id} - ${item.customer.name}"
            serviceNameView.text = "TOTAL: R$ $nominalValueFormatted\nPENDENTE: R$ $pendingValueFormatted"
            serviceDateView.text = stringToFormattedDate(item.creationDate)
            statusTextView.text = item.status.toString()

            val (cardBackground, textColor) = when (item.status) {
                PENDING -> R.drawable.pending_status_background to "#0E5FB2"
                PAID -> R.drawable.paid_status_background to "#805AD5"
                AUTHORIZED -> R.drawable.authorized_status_background to "#B7791F"
                COMPLETED -> R.drawable.completed_status_background to "#2F855A"
                CANCELLED -> R.drawable.cancelled_status_background to "#FFFFFF"
            }

            statusView.background = ContextCompat.getDrawable(context, cardBackground)
            statusTextView.setTextColor(android.graphics.Color.parseColor(textColor))

            orderCard.setOnClickListener {
                listener.onItemClick(item)
            }

        }

    }

}