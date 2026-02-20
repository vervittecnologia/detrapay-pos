package com.detrapay.ui.registration.resume

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Typeface
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.RegistrationResumeListItemBinding
import java.util.Locale

class RegistrationResumeRecyclerViewAdapter(
    private var values: List<SimulationItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val locale = Locale("pt", "BR")

    interface OnItemClickListener {
        fun onRemoveDiscount(item: SimulationItem, itemPosition: Int)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<SimulationItem>) {
        this.values = newList
        notifyDataSetChanged()
    }

    fun updateItem(newList: List<SimulationItem>, itemPosition: Int) {
        this.values = newList
        notifyItemChanged(itemPosition)
    }

    override fun getItemId(position: Int): Long {
        return if (position == values.size) -1L else values[position].id.toLong()
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_TOTAL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == values.size) TYPE_TOTAL else TYPE_ITEM
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TOTAL) {
            val itemBinding = RegistrationResumeListItemBinding.inflate(inflater, parent, false)
            TotalViewHolder(itemBinding)
        } else {
            val itemBinding = RegistrationResumeListItemBinding.inflate(inflater, parent, false)
            RegistrationResumeViewHolder(itemBinding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RegistrationResumeViewHolder) {
            val item: SimulationItem = values[position]
            holder.bind(item, locale, position)
        } else if (holder is TotalViewHolder) {
            val total = values.sumOf { (it.price - (it.discount ?: 0.0)) }
            holder.bind(total, locale)
        }
    }

    override fun getItemCount(): Int = values.size + 1

    inner class TotalViewHolder(binding: RegistrationResumeListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val itemName: TextView = binding.itemName
        private val itemValue: TextView = binding.itemValue
        private val itemDiscount: TextView = binding.itemDiscount
        private val btnDiscount: ImageView = binding.discountBtn

        @SuppressLint("SetTextI18n")
        fun bind(total: Double, locale: Locale) {
            itemName.text = "TOTAL"
            itemName.textStyle = Typeface.BOLD
            val formattedTotal = "%,.2f".format(locale, total)
            itemValue.text = "R$ $formattedTotal"
            itemValue.textStyle = Typeface.BOLD
            itemDiscount.visibility = View.GONE
            btnDiscount.visibility = View.GONE
        }
    }

    private var TextView.textStyle: Int
        get() = typeface.style
        set(value) {
            setTypeface(typeface, value)
        }

    inner class RegistrationResumeViewHolder(binding: RegistrationResumeListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val itemName: TextView = binding.itemName
        private val itemValue: TextView = binding.itemValue
        private val itemDiscount: TextView = binding.itemDiscount
        private val btnDiscount: ImageView = binding.discountBtn

        @SuppressLint("SetTextI18n")
        fun bind(item: SimulationItem, locale: Locale, position: Int) {
            val price = "%,.2f".format(locale, item.price)
            itemName.text = item.name
            itemValue.text = "R$ $price"

            if (item.discount != null) {
                itemDiscount.visibility = View.VISIBLE
                btnDiscount.visibility = View.VISIBLE

                val discount = "%,.2f".format(locale, item.discount)
                itemDiscount.text = "- R$ $discount"

                btnDiscount.setOnClickListener {
                    listener.onRemoveDiscount(item, position)
                }
            } else {
                itemDiscount.visibility = View.GONE
                btnDiscount.visibility = View.GONE
            }
        }
    }

}