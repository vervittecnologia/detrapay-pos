package com.detrapay.ui.home.direct_checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.PaymentData
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.simplified.DirectCheckoutPendingPayment
import com.detrapay.ui.home.simplified.SimplifiedReceivableListViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DirectCheckoutFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: SimplifiedReceivableListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DirectCheckoutRoute(
                    homeViewModel = homeViewModel,
                    viewModel = viewModel,
                    defaultCompanyName = getString(R.string.home_default_company_name),
                    defaultCompanyDocument = getString(R.string.home_company_document_preview),
                    directCheckoutErrorMessage = getString(R.string.direct_checkout_error),
                    installmentErrorMessage = getString(R.string.direct_checkout_installments_error),
                    addPaymentLoadErrorMessage = getString(R.string.order_details_add_payment_load_error),
                    paymentSuccessMessage = getString(R.string.order_details_payment_success_toast),
                    invalidSimulatorAmountMessage = "Informe um valor maior que zero.",
                    onEffect = ::handleEffect,
                )
            }
        }
    }

    private fun handleEffect(effect: DirectCheckoutEffect) {
        when (effect) {
            DirectCheckoutEffect.ShowLogoutConfirmation -> showLogoutConfirmation()
            DirectCheckoutEffect.NavigateToRegistration -> openNewOrderFlow()
            is DirectCheckoutEffect.OpenPaymentDialog -> openPaymentDialog(effect.pendingPayment, effect.onResult)
            is DirectCheckoutEffect.ConfirmManualPayment -> {
                viewModel.confirmDirectCheckoutManualPayment(effect.pendingPayment, effect.paymentData)
            }
            is DirectCheckoutEffect.CopySimulatorText -> copySimulatorText(effect.text)
            is DirectCheckoutEffect.ShareSimulatorText -> shareSimulatorText(effect.text)
            is DirectCheckoutEffect.ShowToast -> Toast.makeText(
                requireContext(),
                effect.message,
                if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
            is DirectCheckoutEffect.ShowSessionExpired -> validateErrorType(effect.exception)
        }
    }

    private fun openPaymentDialog(
        pendingPayment: DirectCheckoutPendingPayment,
        onResult: (PaymentData?) -> Unit,
    ) {
        viewModel.clearDirectCheckoutPaymentState()
        PaymentDialogFragment(
            listener = object : PaymentDialogFragment.PaymentListener {
                override fun onResult(paymentData: PaymentData?) {
                    onResult(paymentData)
                }
            },
            orderId = pendingPayment.order.id,
            receivableItem = pendingPayment.receivable,
            serial = getSerialForPrePay(),
        ).show(parentFragmentManager, "PaymentDialogFragment")
    }

    private fun copySimulatorText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("SimulaÃ§Ã£o de CrÃ©dito", text))
        Toast.makeText(requireContext(), "Parcelamento copiado.", Toast.LENGTH_SHORT).show()
    }

    private fun shareSimulatorText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        runCatching {
            startActivity(Intent.createChooser(intent, "Compartilhar simulaÃ§Ã£o"))
        }.onFailure {
            Toast.makeText(requireContext(), "NÃ£o foi possÃ­vel compartilhar a simulaÃ§Ã£o.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNewOrderFlow() {
        runCatching {
            findNavController().navigate(R.id.registrationActivity)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.home_orders_empty_cta),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun showLogoutConfirmation() {
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

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager, error)
        }
    }
}
