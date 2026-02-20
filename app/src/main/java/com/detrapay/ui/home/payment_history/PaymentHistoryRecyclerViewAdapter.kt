package com.detrapay.ui.home.payment_history

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.detrapay.R
import com.detrapay.data.model.local.Payment
import com.detrapay.databinding.PaymentListItemBinding

class PaymentHistoryRecyclerViewAdapter(
    private var values: List<Payment>
) : RecyclerView.Adapter<PaymentHistoryRecyclerViewAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val itemBinding =
            PaymentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(parent.context, itemBinding)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<Payment>) {
        this.values = newList
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
    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val item: Payment = values[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = values.size

    inner class PaymentViewHolder(val context: Context, val binding: PaymentListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val orderIdView: TextView = binding.ordeId
        private val amountView: TextView = binding.amount
        private val installmentView: TextView = binding.installments
        private val paymentMethodView: TextView = binding.paymentMethod
        private val dateView: TextView = binding.paymentDate
        private val statusTextView: TextView = binding.status
        private val transactionId: TextView = binding.transactionId
        private val transactionCode: TextView = binding.transactionCode
        private val message: TextView = binding.message
        private val errorCode: TextView = binding.errorCode

        private val cardBrand: TextView = binding.cardBrand
        private val cardLast4: TextView = binding.cardLast4
        private val cardHolder: TextView = binding.cardHolder
        private val pixIdentification: TextView = binding.pixIdentification

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(
            item: Payment
        ) {
            orderIdView.text = "Pedido n: ${item.orderId}"
            amountView.text = "Valor: ${item.amount}"
            installmentView.text = "Parcelas: ${item.installments}"
            paymentMethodView.text = "Parcelas: ${item.paymentType}"
            statusTextView.text = if (item.result == 0 ) { "Sucesso" } else { "Erro" }
            statusTextView.background = if (item.result == 0 ) {context.getDrawable(R.drawable.payment_success_status_background)} else {context.getDrawable(R.drawable.payment_error_status_background)}
            dateView.text = item.date ?: ""

            item.transactionId?.let {
                if (it.isNotEmpty()) {
                    transactionId.visibility = View.VISIBLE
                    transactionId.text = "Id da transação: $it"
                } else {
                    transactionId.visibility = View.GONE
                }
            }

            item.transactionCode?.let {
                if (it.isNotEmpty()) {
                    transactionCode.visibility = View.VISIBLE
                    transactionCode.text = "Código da transação: $it"
                } else {
                    transactionCode.visibility = View.GONE
                }
            }

            item.errorCode?.let {
                if (it.isNotEmpty()) {
                    errorCode.visibility = View.VISIBLE
                    errorCode.text = "Código de erro: $it"
                } else {
                    errorCode.visibility = View.GONE
                }
            }

            item.message?.let {
                if (it.isNotEmpty()) {
                    message.visibility = View.VISIBLE
                    message.text = "Detalhes: $it"
                } else {
                    message.visibility = View.GONE
                }
            }

            item.cardBrand?.let {
                if (it.isNotEmpty()) {
                    cardBrand.visibility = View.VISIBLE
                    cardBrand.text = "Bandeira do cartão: $it"
                } else {
                    cardBrand.visibility = View.GONE
                }
            }

            item.cardHolder?.let {
                if (it.isNotEmpty()) {
                    cardHolder.visibility = View.VISIBLE
                    cardHolder.text = "Titular do cartão: $it"
                } else {
                    cardHolder.visibility = View.GONE
                }
            }

            item.cardLast4?.let {
                if (it.isNotEmpty()) {
                    cardLast4.visibility = View.VISIBLE
                    cardLast4.text = "Últimos digitos do cartão: $it"
                } else {
                    cardLast4.visibility = View.GONE
                }
            }

            item.pixTxIdCode?.let {
                if (it.isNotEmpty()) {
                    pixIdentification.visibility = View.VISIBLE
                    pixIdentification.text = "Identificação do pix: $it"
                } else {
                    pixIdentification.visibility = View.GONE
                }
            }
        }

    }

}