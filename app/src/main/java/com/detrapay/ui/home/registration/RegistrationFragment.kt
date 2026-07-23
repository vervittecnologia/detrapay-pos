package com.detrapay.ui.home.registration

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.detrapay.R
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.order_list.OrderListViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.order_details.OrderDetailsActivity
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val orderListViewModel: OrderListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val homeState by homeViewModel.homeState.observeAsState()
                val orderState by orderListViewModel.orderListState.observeAsState()

                val data = (homeState as? UIState.Success)?.data
                val recentOrders = (orderState as? UIState.Success)?.data
                    ?.sortedByDescending { it.id }
                    ?.take(2)
                    .orEmpty()

                HomeScreen(
                    companyName = data?.companyName.orEmpty(),
                    companyDocument = formatCnpj(data?.companyDocument.orEmpty()),
                    dispatcherName = data?.dispatcherName.orEmpty(),
                    logoKey = data?.companyLogoKey,
                    recentOrders = recentOrders,
                    onLogout = { showLogoutDialog() },
                    onNewRegistration = {
                        findNavController().navigate(R.id.action_registrationFragment_to_registrationActivity)
                    },
                    onViewAllSales = {
                        findNavController().navigate(R.id.orderListFragment)
                    },
                    onOrderClick = { order ->
                        val intent = Intent(requireContext(), OrderDetailsActivity::class.java).apply {
                            putExtra("order", order)
                            putExtra("orderId", order.id)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        orderListViewModel.loadScreenContent()
    }

    private fun formatCnpj(document: String): String {
        val numbers = document.filter(Char::isDigit)
        if (numbers.length != 14) return if (document.isBlank()) "-" else document

        return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
            "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
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
