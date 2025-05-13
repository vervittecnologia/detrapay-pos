package com.detrapay.ui.employee_selection

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.data.model.Employee
import com.detrapay.databinding.EmployeeListItemBinding

class EmployeeRecyclerViewAdapter(
    private var values: List<Employee>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<EmployeeRecyclerViewAdapter.EmployeeViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(item: Employee)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun swapData(newList: List<Employee>) {
        this.values = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val itemBinding =
            EmployeeListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val item: Employee = values[position]
        holder.bind(item, listener)
    }

    override fun getItemCount(): Int = values.size

    inner class EmployeeViewHolder(binding: EmployeeListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val nameView: TextView = binding.employeeName
        private val employeeCard: CardView = binding.employeeCard

        fun bind(item: Employee, listener: OnItemClickListener) {
            nameView.text = item.name
            employeeCard.setOnClickListener { listener.onItemClick(item) }
        }

    }

}