package com.detrapay.ui.home.orders

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
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.orders.OrdersViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogViewModel
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: OrdersViewModel by viewModels()
    private val paymentViewModel: PaymentDialogViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OrdersRoute(
                    homeViewModel = homeViewModel,
                    viewModel = viewModel,
                    paymentViewModel = paymentViewModel,
                    terminalSerial = getSerialForPrePay(),
                    defaultCompanyName = getString(R.string.home_default_company_name),
                    defaultCompanyDocument = getString(R.string.home_company_document_preview),
                    ordersErrorMessage = getString(R.string.orders_error),
                    installmentErrorMessage = getString(R.string.orders_installments_error),
                    addPaymentLoadErrorMessage = getString(R.string.order_details_add_payment_load_error),
                    paymentSuccessMessage = getString(R.string.order_details_payment_success_toast),
                    invalidSimulatorAmountMessage = "Informe um valor maior que zero.",
                    onEffect = ::handleEffect,
                )
            }
        }
    }

    private fun handleEffect(effect: OrderFlowEffect) {
        when (effect) {
            OrderFlowEffect.ShowLogoutConfirmation -> showLogoutConfirmation()
            OrderFlowEffect.NavigateToRegistration -> openNewOrderFlow()
            is OrderFlowEffect.ConfirmRecordOnlyPayment -> confirmRecordOnlyPayment(effect.request)
            is OrderFlowEffect.CopyPaymentText -> copyPaymentText(effect.text)
            is OrderFlowEffect.CopySimulatorText -> copySimulatorText(effect.text)
            is OrderFlowEffect.ShareSimulatorText -> shareSimulatorText(effect.text)
            is OrderFlowEffect.ShowToast -> Toast.makeText(
                requireContext(),
                effect.message,
                if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
            is OrderFlowEffect.ShowSessionExpired -> validateErrorType(effect.exception)
        }
    }

    private fun confirmRecordOnlyPayment(request: OrderPaymentRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar pagamento")
            .setMessage(
                "Registrar ${OrderPresentation.formatCurrency(request.amount)} como " +
                    "${request.paymentMethod.name}?",
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.recordOfflinePayment(request)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyPaymentText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("pix_code", text))
        Toast.makeText(requireContext(), getString(R.string.payment_dialog_pix_copied), Toast.LENGTH_SHORT).show()
    }

    private fun copySimulatorText(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Simulação de Crédito", text))
        Toast.makeText(requireContext(), "Parcelamento copiado.", Toast.LENGTH_SHORT).show()
    }

    private fun shareSimulatorText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        runCatching {
            startActivity(Intent.createChooser(intent, "Compartilhar simulação"))
        }.onFailure {
            Toast.makeText(requireContext(), "Não foi possível compartilhar a simulação.", Toast.LENGTH_SHORT).show()
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
