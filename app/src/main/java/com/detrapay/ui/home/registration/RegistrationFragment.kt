package com.detrapay.ui.home.registration

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.databinding.FragmentRegistrationBinding
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.order_list.OrderListViewModel
import com.detrapay.ui.home.order_list.OrderRecyclerViewAdapter
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.order_details.OrderDetailsActivity
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.ImageUtils
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private lateinit var binding: FragmentRegistrationBinding
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val orderListViewModel: OrderListViewModel by viewModels()
    private lateinit var recentSalesAdapter: OrderRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecentSalesList()
        observeHomeState()
        observeRecentSalesState()
        binding.newRegistrationCard.setOnClickListener {
            binding.root.findNavController()
                .navigate(R.id.action_registrationFragment_to_registrationActivity)
        }
        binding.viewAllSales.setOnClickListener {
            binding.root.findNavController().navigate(R.id.orderListFragment)
        }
        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
        }
        orderListViewModel.loadScreenContent()
    }

    private fun observeHomeState() {
        homeViewModel.homeState.observe(viewLifecycleOwner, Observer { state ->
            if (state is UIState.Success) {
                renderOperator(
                    companyName = state.data?.companyName.orEmpty(),
                    companyDocument = state.data?.companyDocument.orEmpty(),
                    dispatcherName = state.data?.dispatcherName.orEmpty(),
                    logoKey = state.data?.companyLogoKey
                )
            } else {
                renderOperator(companyName = "", companyDocument = "", dispatcherName = "", logoKey = null)
            }
        })
    }

    private fun renderOperator(
        companyName: String,
        companyDocument: String,
        dispatcherName: String,
        logoKey: String?
    ) {
        binding.operatorTitle.text = companyName.ifBlank {
            getString(R.string.home_default_company_name)
        }
        binding.operatorDocument.text = getString(
            R.string.home_company_document,
            formatCnpj(companyDocument)
        )
        binding.operatorSubtitle.text = dispatcherName.ifBlank {
            getString(R.string.home_default_dispatcher_name)
        }
        if (!logoKey.isNullOrBlank()) {
            ImageUtils.loadImage(requireContext(), logoKey, binding.operatorLogo)
        } else {
            binding.operatorLogo.setImageResource(R.drawable.icon)
        }
    }

    private fun formatCnpj(document: String): String {
        val numbers = document.filter(Char::isDigit)
        if (numbers.length != 14) return if (document.isBlank()) "-" else document

        return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
            "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
    }

    private fun initRecentSalesList() {
        recentSalesAdapter =
            OrderRecyclerViewAdapter(emptyList(), object : OrderRecyclerViewAdapter.OnItemClickListener {
                override fun onItemClick(item: Order) {
                    val orderDetailsActivityIntent = Intent(
                        requireContext(),
                        OrderDetailsActivity::class.java
                    )
                    orderDetailsActivityIntent.putExtra("order", item)
                    orderDetailsActivityIntent.putExtra("orderId", item.id)
                    startActivity(orderDetailsActivityIntent)
                }
            })
        binding.recentSalesList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentSalesList.adapter = recentSalesAdapter
    }

    private fun observeRecentSalesState() {
        orderListViewModel.orderListState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UIState.Success -> {
                    val recentOrders = state.data
                        ?.sortedByDescending { it.id }
                        ?.take(2)
                        .orEmpty()
                    recentSalesAdapter.swapData(recentOrders)
                    binding.recentSalesList.visibility = if (recentOrders.isEmpty()) View.GONE else View.VISIBLE
                    binding.recentSalesEmpty.visibility = if (recentOrders.isEmpty()) View.VISIBLE else View.GONE
                }

                is UIState.Error -> {
                    recentSalesAdapter.swapData(emptyList())
                    binding.recentSalesList.visibility = View.GONE
                    binding.recentSalesEmpty.visibility = View.VISIBLE
                }

                else -> Unit
            }
        })
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                homeViewModel.logout()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}
