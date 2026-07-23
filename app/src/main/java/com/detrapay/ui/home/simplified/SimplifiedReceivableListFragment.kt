package com.detrapay.ui.home.simplified

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.SellerAppMode
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SimplifiedReceivableListFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: SimplifiedReceivableListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val homeState by homeViewModel.homeState.observeAsState()
                val receivableState by viewModel.receivableListState.observeAsState()

                val data = (homeState as? UIState.Success)?.data
                val receivables = (receivableState as? UIState.Success)?.data.orEmpty()
                val isLoading = receivableState is UIState.Loading
                val errorState = receivableState as? UIState.Error
                val errorMessage = errorState?.message
                
                if (errorState != null) {
                    validateErrorType(errorState.exception)
                }

                SimplifiedReceivableScreen(
                    companyName = data?.companyName.orEmpty(),
                    companyDocument = formatCnpj(data?.companyDocument.orEmpty()),
                    receivables = receivables,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogout = { showLogoutDialog() },
                    onReload = { viewModel.loadScreenContent(forceRefresh = true) },
                    onPay = { onPayClick(it) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadScreenContent()
    }

    private fun onPayClick(item: OrderReceivable) {
        PaymentDialogFragment(
            listener = object : PaymentDialogFragment.PaymentListener {
                override fun onResult(paymentData: PaymentData?) {
                    if (paymentData != null) {
                        viewModel.loadScreenContent(forceRefresh = true)
                    }
                }
            },
            orderId = item.order.id,
            receivableItem = item.receivable,
            serial = getSerialForPrePay()
        ).show(parentFragmentManager, "PaymentDialogFragment")
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

    private fun getSerialForPrePay(): String {
        return if (BuildConfig.DEBUG) {
            DebugConstants.DEBUG_SPLIT_DEVICE_ID
        } else {
            DeviceUtils.getSerialNumber()
        }
    }

    private fun formatCnpj(document: String): String {
        val numbers = document.filter(Char::isDigit)
        if (numbers.length != 14) return if (document.isBlank()) "-" else document

        return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
            "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager, error)
        }
    }
}
