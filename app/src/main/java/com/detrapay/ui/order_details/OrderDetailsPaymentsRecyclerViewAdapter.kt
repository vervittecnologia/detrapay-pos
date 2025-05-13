package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus.CANCELLED
import com.detrapay.data.model.OrderReceivableItemStatus.PAID
import com.detrapay.data.model.OrderReceivableItemStatus.PENDING
import com.detrapay.databinding.OrderPaymentListItemBinding
import java.util.Locale

class OrderDetailsPaymentsRecyclerViewAdapter(
    private var values: List<OrderReceivableItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<OrderDetailsPaymentsRecyclerViewAdapter.OrderDetailsPaymentsViewHolder>() {

    private val locale = Locale("pt", "BR")

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderDetailsPaymentsViewHolder {
        val itemBinding =
            OrderPaymentListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderDetailsPaymentsViewHolder(parent.context, itemBinding)
    }

    interface OnItemClickListener {
        fun onItemClick(item: OrderReceivableItem)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: OrderDetailsPaymentsViewHolder, position: Int) {
        val item = values[position]
        holder.bind(item, listener)
    }

    override fun getItemCount(): Int = values.size

    inner class OrderDetailsPaymentsViewHolder(
        val context: Context,
        val binding: OrderPaymentListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val paymentMethodImage: ImageView = binding.paymentMethodImage
        private val paymentMethodName: TextView = binding.paymentMethodName
        private val paymentMethodAmount: TextView = binding.paymentMethodAmount

        private val paymentMethodStatusView: LinearLayout = binding.paymentMethodStatusView
        private val statusTextView: TextView = binding.status

        private val paymentMethodCard: CardView = binding.paymentMethodCard

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(
            item: OrderReceivableItem,
            listener: OnItemClickListener
        ) {
            val imageDrawable = if (item.paymentMethod.name.contains("Cartão de crédito", true)) {
                R.drawable.ic_credit_card_outline
            } else if (item.paymentMethod.name.contains("Pix", true)) {
                R.drawable.ic_pix
            } else {
                R.drawable.ic_money
            }

            paymentMethodImage.setImageDrawable(context.getDrawable(imageDrawable))

            paymentMethodName.text = item.paymentMethod.name
            val receivableAmount = "%,.2f".format(locale, item.amount)
            paymentMethodAmount.text = "R$ $receivableAmount"

            statusTextView.text = item.status.toString()

            val cardBackground = when (item.status) {
                PENDING -> R.drawable.pending_status_background
                PAID -> R.drawable.paid_status_background
                CANCELLED -> R.drawable.cancelled_status_background
            }

            paymentMethodStatusView.background = context.getDrawable(cardBackground)

            paymentMethodCard.setOnClickListener {
                listener.onItemClick(item)
            }

        }

    }

}