package com.detrapay.ui.home.orders

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.core.content.FileProvider
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.orders.OrdersViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogViewModel
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: OrdersViewModel by viewModels()
    private val paymentViewModel: PaymentDialogViewModel by activityViewModels()
    private var createdOrder by mutableStateOf<Order?>(null)
    private var cameraCaptureError by mutableStateOf<String?>(null)
    private var pendingPhotoFile: File? = null
    private var pendingPhotoOrderId: Int? = null
    private val registrationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            createdOrder = getCreatedOrder(result.data)
        }
    }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val file = pendingPhotoFile
        val orderId = pendingPhotoOrderId
        pendingPhotoFile = null
        pendingPhotoOrderId = null
        if (saved && file?.exists() == true && file.length() > 0L && orderId != null) {
            cameraCaptureError = null
            viewModel.uploadOrderPhoto(orderId, file)
        } else {
            file?.delete()
            cameraCaptureError = "Nao foi possivel salvar a foto. Tente novamente."
        }
    }

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
                    orderToOpen = createdOrder,
                    onOrderOpened = { createdOrder = null },
                    cameraAvailable = hasCameraCapture(),
                    cameraCaptureError = cameraCaptureError,
                    onTakeOrderPhoto = ::openOrderCamera,
                    onEffect = ::handleEffect,
                )
            }
        }
    }

    private fun handleEffect(effect: OrderFlowEffect) {
        when (effect) {
            OrderFlowEffect.ShowLogoutConfirmation -> showLogoutConfirmation()
            OrderFlowEffect.NavigateToRegistration -> openNewOrderFlow()
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
        registrationLauncher.launch(Intent(requireContext(), RegistrationActivity::class.java))
    }

    private fun hasCameraCapture(): Boolean {
        val context = requireContext()
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }

    private fun openOrderCamera(orderId: Int) {
        cameraCaptureError = null
        runCatching {
            val context = requireContext()
            val directory = File(context.cacheDir, "order_photos").apply { mkdirs() }
            val file = File.createTempFile("pedido_${orderId}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            pendingPhotoFile = file
            pendingPhotoOrderId = orderId
            cameraLauncher.launch(uri)
        }.onFailure {
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
            pendingPhotoOrderId = null
            cameraCaptureError = "Nao foi possivel abrir a camera deste terminal."
        }
    }

    private fun getCreatedOrder(data: Intent?): Order? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data?.getSerializableExtra(RegistrationActivity.EXTRA_CREATED_ORDER, Order::class.java)
        } else {
            @Suppress("DEPRECATION")
            data?.getSerializableExtra(RegistrationActivity.EXTRA_CREATED_ORDER) as? Order
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
