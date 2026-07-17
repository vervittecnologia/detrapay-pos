package com.detrapay.ui.home.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.Salesman
import com.detrapay.databinding.ProfileSalesmanCardItemBinding

class ProfileSalesmenAdapter :
    ListAdapter<Salesman, ProfileSalesmenAdapter.ProfileSalesmanViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileSalesmanViewHolder {
        val binding = ProfileSalesmanCardItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileSalesmanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileSalesmanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfileSalesmanViewHolder(
        private val binding: ProfileSalesmanCardItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(salesman: Salesman) {
            val context = binding.root.context
            binding.salesmanName.text = salesman.name
            binding.salesmanAvatarContainer.avatarTxtView.text = buildInitials(salesman.name)
            binding.salesmanPhone.text = salesman.phoneNumber?.takeIf { it.isNotBlank() }
                ?.let(::formatPhone)
                ?: context.getString(R.string.profile_not_informed)
            binding.salesmanEmail.text = salesman.email?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.profile_not_informed)
        }

        private fun buildInitials(name: String): String {
            val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return "--"

            return parts
                .take(2)
                .joinToString(separator = "") { it.first().uppercase() }
        }

        private fun formatPhone(phone: String): String {
            val digits = phone.filter(Char::isDigit)
            return when (digits.length) {
                10 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6, 10)}"
                11 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7, 11)}"
                else -> phone
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Salesman>() {
        override fun areItemsTheSame(oldItem: Salesman, newItem: Salesman): Boolean {
            return oldItem.id == newItem.id && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Salesman, newItem: Salesman): Boolean {
            return oldItem == newItem
        }
    }
}
