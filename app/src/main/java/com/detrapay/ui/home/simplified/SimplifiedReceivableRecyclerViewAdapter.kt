package com.detrapay.ui.home.simplified

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.databinding.SimplifiedReceivableListItemBinding
import java.util.Locale

class SimplifiedReceivableRecyclerViewAdapter(
    private var values: List<OrderReceivable>,
    private val listener: OnPayClickListener
) : RecyclerView.Adapter<SimplifiedReceivableRecyclerViewAdapter.ViewHolder>() {

    private val locale = Locale("pt", "BR")
    private var filteredValues = values.toMutableList()

    interface OnPayClickListener {
        fun onPayClick(item: OrderReceivable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SimplifiedReceivableListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(parent.context, binding)
    }

    override fun getItemCount(): Int = filteredValues.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredValues[position], listener)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<OrderReceivable>) {
        values = newList
        filteredValues = newList.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterData(text: String) {
        val query = text.trim()
        filteredValues = values.filter { item ->
            val order = item.order
            query.isBlank() ||
                order.id.toString().contains(query, ignoreCase = true) ||
                order.customer.name.contains(query, ignoreCase = true) ||
                order.customer.cpfCnpj.contains(query.filter(Char::isDigit), ignoreCase = true)
        }.toMutableList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val context: Context,
        private val binding: SimplifiedReceivableListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderReceivable, listener: OnPayClickListener) {
            val order = item.order
            val receivable = item.receivable
            val amountFormatted = "%,.2f".format(locale, receivable.amountOriginal)
            val document = formatCpfCnpj(order.customer.cpfCnpj)
            val installmentLabel = if (receivable.installments > 1) {
                "${receivable.installments}x"
            } else {
                "A vista"
            }

            binding.orderTitle.text = "#${order.id} - ${order.customer.name}"
            binding.orderSalesman.text = "Vendedor: ${order.salesman?.name?.takeIf { it.isNotBlank() } ?: "-"}"
            binding.orderDocument.text = "CPF/CNPJ: ${document.ifBlank { "-" }}  •  $installmentLabel"
            binding.orderAmount.text = "R$ $amountFormatted"
            binding.orderDate.text = formatDate(receivable.paymentDate ?: order.creationDate)
            binding.orderStatus.text = statusLabel(receivable.status)

            val (backgroundRes, textColorRes) = when (receivable.status) {
                OrderReceivableItemStatus.PENDING ->
                    R.drawable.home_status_pending_background to R.color.home_status_pending_text
                OrderReceivableItemStatus.PAID ->
                    R.drawable.home_status_finished_background to R.color.home_status_finished_text
                OrderReceivableItemStatus.CANCELLED,
                OrderReceivableItemStatus.REFUNDED ->
                    R.drawable.sales_list_status_cancelled_background to R.color.red
            }
            binding.orderStatus.background = ContextCompat.getDrawable(context, backgroundRes)
            binding.orderStatus.setTextColor(ContextCompat.getColor(context, textColorRes))

            val canPay = receivable.status == OrderReceivableItemStatus.PENDING
            binding.payAction.visibility = if (canPay) View.VISIBLE else View.GONE
            binding.payAction.setOnClickListener {
                listener.onPayClick(item)
            }
            binding.orderCard.setOnClickListener {
                if (canPay) listener.onPayClick(item)
            }
        }

        private fun statusLabel(status: OrderReceivableItemStatus): String = when (status) {
            OrderReceivableItemStatus.PENDING -> "Pendente"
            OrderReceivableItemStatus.PAID -> "Pago"
            OrderReceivableItemStatus.CANCELLED -> "Cancelado"
            OrderReceivableItemStatus.REFUNDED -> "Estornado"
        }

        private fun formatDate(date: String): String {
            return runCatching {
                val normalized = date.take(10)
                "${normalized.substring(8, 10)}/${normalized.substring(5, 7)}/${normalized.substring(0, 4)}"
            }.getOrDefault(date)
        }

        private fun formatCpfCnpj(value: String): String {
            val digits = value.filter(Char::isDigit)
            return when (digits.length) {
                11 -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9, 11)}"
                14 -> "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
                else -> value
            }
        }
    }
}
