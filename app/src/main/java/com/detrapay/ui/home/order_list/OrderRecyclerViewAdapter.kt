package com.detrapay.ui.home.order_list

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderStatus
import com.detrapay.databinding.HomeRecentSaleCardBinding
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import java.util.Locale

class OrderRecyclerViewAdapter(
    private var values: List<Order>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderRecyclerViewAdapter.OrderViewHolder>() {

    private var filteredValues: MutableList<Order> = values.sortedByDescending { it.id }.toMutableList()
    private val locale = Locale("pt", "BR")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val itemBinding =
            HomeRecentSaleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

    @SuppressLint("NotifyDataSetChanged")
    fun swapDataSortedByStatus(newList: List<Order>) {
        this.values = newList
        this.filteredValues = newList.sortedWith(
            compareByDescending<Order> {
                when (it.status) {
                OrderStatus.COMPLETED, OrderStatus.PAID, OrderStatus.AUTHORIZED -> 0
                OrderStatus.CANCELLED -> 1
                else -> 2
                }
            }.thenByDescending { it.id }
        ).toMutableList()
        notifyDataSetChanged()
    }

    private fun stringToFormattedDate(date: String): String {
        val day = date.substring(8, 10)
        val month = date.substring(5, 7)
        val year = date.substring(0, 4)
        return "$day/$month/$year"
    }

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

    inner class OrderViewHolder(val context: Context, val binding: HomeRecentSaleCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val clientNameView: TextView = binding.clientName
        private val serviceNameView: TextView = binding.serviceName
        private val customerDocumentView: TextView = binding.customerDocument
        private val valueView: TextView = binding.saleValue
        private val serviceDateView: TextView = binding.serviceDate
        private val statusTextView: TextView = binding.saleStatus
        private val orderCard: CardView = binding.orderCard

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(
            item: Order,
            listener: OnItemClickListener
        ) {
            val valueFormatted = "%,.2f".format(locale, item.originalAmount)
            val salesmanName = item.salesman?.name.orEmpty()
            val salesmanLabel = if (salesmanName.isNotBlank()) {
                "Vendedor: $salesmanName"
            } else {
                "Vendedor: -"
            }
            val customerDocument = formatCpfCnpj(item.customer.cpfCnpj)
            val customerDocumentLabel = if (customerDocument.isNotBlank()) {
                "CPF/CNPJ: $customerDocument"
            } else {
                "CPF/CNPJ: -"
            }

            clientNameView.text = "#${item.id} - ${item.customer.name}"
            serviceNameView.text = salesmanLabel
            customerDocumentView.text = customerDocumentLabel
            valueView.text = "R$ $valueFormatted"
            serviceDateView.text = stringToFormattedDate(item.creationDate)
            statusTextView.text = statusLabel(item.status)

            val (cardBackground, textColorRes) = when (item.status) {
                OrderStatus.PENDING -> R.drawable.home_status_pending_background to R.color.home_status_pending_text
                OrderStatus.PAID -> R.drawable.home_status_finished_background to R.color.home_status_finished_text
                OrderStatus.AUTHORIZED -> R.drawable.sales_list_status_authorized_background to R.color.orange
                OrderStatus.COMPLETED -> R.drawable.home_status_finished_background to R.color.home_status_finished_text
                OrderStatus.CANCELLED -> R.drawable.sales_list_status_cancelled_background to R.color.red
            }

            statusTextView.background = ContextCompat.getDrawable(context, cardBackground)
            statusTextView.setTextColor(ContextCompat.getColor(context, textColorRes))

            orderCard.setOnClickListener {
                listener.onItemClick(item)
            }
        }

        private fun statusLabel(status: OrderStatus): String =
            DirectCheckoutOrderPresentation.statusLabel(status)

        private fun formatCpfCnpj(cpfCnpj: String): String {
            if (cpfCnpj.length == 11) {
                val first = cpfCnpj.substring(0, 3)
                val second = cpfCnpj.substring(3, 6)
                val third = cpfCnpj.substring(6, 9)
                val fourth = cpfCnpj.substring(9, 11)
                return "$first.$second.$third-$fourth"
            }
            if (cpfCnpj.length == 14) {
                val first = cpfCnpj.substring(0, 2)
                val second = cpfCnpj.substring(2, 5)
                val third = cpfCnpj.substring(5, 8)
                val fourth = cpfCnpj.substring(8, 12)
                val fifth = cpfCnpj.substring(12, 14)
                return "$first.$second.$third/$fourth-$fifth"
            }
            return cpfCnpj
        }
    }
}
