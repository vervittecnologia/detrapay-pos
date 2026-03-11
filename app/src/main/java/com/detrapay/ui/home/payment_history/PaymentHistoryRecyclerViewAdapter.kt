package com.detrapay.ui.home.payment_history

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.local.Payment
import com.detrapay.databinding.PaymentListItemBinding

class PaymentHistoryRecyclerViewAdapter(
    private var values: List<Payment>,
) : RecyclerView.Adapter<PaymentHistoryRecyclerViewAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val itemBinding = PaymentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(parent.context, itemBinding)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<Payment>) {
        values = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(values[position])
    }

    override fun getItemCount(): Int = values.size

    inner class PaymentViewHolder(
        private val context: Context,
        private val binding: PaymentListItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Payment) {
            binding.ordeId.text = "Pedido #${item.orderId}"
            binding.amount.text = item.amount
            binding.installments.text = "Parcelas: ${item.installments}"
            binding.paymentMethod.text = "Metodo: ${item.paymentType}"
            binding.paymentDate.text = item.date.orEmpty()

            val isSuccess = item.result == 0
            binding.status.text = if (isSuccess) "Sucesso" else "Erro"
            binding.status.background = ContextCompat.getDrawable(
                context,
                if (isSuccess) R.drawable.payment_success_status_background else R.drawable.payment_error_status_background,
            )
            binding.status.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSuccess) R.color.green else R.color.red,
                ),
            )

            toggleText(binding.transactionId, item.transactionId, "Transacao")
            toggleText(binding.transactionCode, item.transactionCode, "Codigo")
            toggleText(binding.errorCode, item.errorCode, "Codigo do erro")
            toggleText(binding.message, item.message, "Detalhes")
            toggleText(binding.cardBrand, item.cardBrand, "Bandeira")
            toggleText(binding.cardHolder, item.cardHolder, "Titular")
            toggleText(binding.cardLast4, item.cardLast4, "Final do cartao")
            toggleText(binding.pixIdentification, item.pixTxIdCode, "Identificacao PIX")
        }

        private fun toggleText(view: android.widget.TextView, value: String?, label: String) {
            if (value.isNullOrBlank()) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                view.text = "$label: $value"
            }
        }
    }
}
