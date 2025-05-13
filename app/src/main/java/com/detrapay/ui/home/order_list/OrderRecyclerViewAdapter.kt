package com.detrapay.ui.home.order_list

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderStatus.CANCELLED
import com.detrapay.data.model.OrderStatus.PAID
import com.detrapay.data.model.OrderStatus.PENDING
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.OrderListItemBinding
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class OrderRecyclerViewAdapter(
    private var values: List<Order>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderRecyclerViewAdapter.OrderViewHolder>() {

    private val locale = Locale("pt", "BR")
    private var filteredValues: MutableList<Order> = values.toMutableList()

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
        this.filteredValues = newList.toMutableList()
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
        }.toMutableList()
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

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(
            item: Order,
            listener: OnItemClickListener
        ) {
            clientNameView.text = item.customer.name
            serviceNameView.text = item.serviceName
            serviceDateView.text = stringToFormattedDate(item.creationDate)
            statusTextView.text = item.status.toString()

            val cardBackground = when (item.status) {
                PENDING -> R.drawable.pending_status_background
                PAID -> R.drawable.paid_status_background
                CANCELLED -> R.drawable.cancelled_status_background
            }

            statusView.background = context.getDrawable(cardBackground)

            orderCard.setOnClickListener {
                listener.onItemClick(item)
            }

        }

    }

}