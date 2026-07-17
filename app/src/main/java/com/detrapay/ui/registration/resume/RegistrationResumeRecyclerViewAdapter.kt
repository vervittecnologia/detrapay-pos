package com.detrapay.ui.registration.resume

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.RegistrationResumeListItemBinding
import java.util.Locale

class RegistrationResumeRecyclerViewAdapter(
    private var values: List<SimulationItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RegistrationResumeRecyclerViewAdapter.RegistrationResumeViewHolder>() {

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

    override fun getItemId(position: Int): Long = values[position].id.toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RegistrationResumeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding = RegistrationResumeListItemBinding.inflate(inflater, parent, false)
        return RegistrationResumeViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RegistrationResumeViewHolder, position: Int) {
        val item: SimulationItem = values[position]
        holder.bind(item, locale, position)
    }

    override fun getItemCount(): Int = values.size

    inner class RegistrationResumeViewHolder(binding: RegistrationResumeListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val itemName: TextView = binding.itemName
        private val itemValue: TextView = binding.itemValue
        private val itemDiscount: TextView = binding.itemDiscount
        private val btnDiscount: ImageView = binding.discountBtn

        @SuppressLint("SetTextI18n")
        fun bind(item: SimulationItem, locale: Locale, position: Int) {
            itemName.text = item.name
            val discount = item.discount ?: 0.0
            val price = "%,.2f".format(locale, item.price)
            itemValue.text = "R$ $price"

            if (discount > 0) {
                itemDiscount.visibility = View.VISIBLE
                btnDiscount.visibility = View.VISIBLE

                val formattedDiscount = "%,.2f".format(locale, discount)
                itemDiscount.text = "-R$ $formattedDiscount"

                btnDiscount.setOnClickListener {
                    listener.onRemoveDiscount(item, position)
                }
            } else {
                itemDiscount.visibility = View.GONE
                btnDiscount.visibility = View.GONE
                btnDiscount.setOnClickListener(null)
            }
        }
    }

}
