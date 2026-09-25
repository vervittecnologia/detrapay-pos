package com.detrapay.ui.home

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.ui.home.orders.OrderFlowEffect
import com.detrapay.ui.home.orders.OrdersRoute
import com.detrapay.ui.home.orders.OrdersViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogViewModel
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.theme.DetrapayTheme
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val ordersViewModel: OrdersViewModel by viewModels()
    private val paymentViewModel: PaymentDialogViewModel by viewModels()

    private var createdOrder by mutableStateOf<Order?>(null)
    private var cameraCaptureError by mutableStateOf<String?>(null)
    private var pendingPhotoFile: File? = null
    private var pendingPhotoOrderId: Int? = null
    private var showLogoutConfirmation by mutableStateOf(false)
    private var sessionError by mutableStateOf<UnauthorizedException?>(null)

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
            ordersViewModel.uploadOrderPhoto(orderId, file)
        } else {
            file?.delete()
            cameraCaptureError = "Nao foi possivel salvar a foto. Tente novamente."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel.loadScreenContent()
        onBackPressedDispatcher.addCallback(this) { showLogoutConfirmation = true }
        setContent {
            DetrapayTheme {
                OrdersRoute(
                    homeViewModel = homeViewModel,
                    viewModel = ordersViewModel,
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

                if (showLogoutConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showLogoutConfirmation = false },
                        title = { Text(getString(R.string.logout_dialog_title)) },
                        text = { Text(getString(R.string.logout_dialog_message)) },
                        confirmButton = {
                            TextButton(onClick = ::logout) { Text(getString(android.R.string.ok)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutConfirmation = false }) {
                                Text(getString(android.R.string.cancel))
                            }
                        },
                    )
                }

                sessionError?.let { error ->
                    AlertDialog(
                        onDismissRequest = { sessionError = null },
                        title = { Text("Sessao invalida ou expirada") },
                        text = { Text(sessionErrorMessage(error)) },
                        confirmButton = {
                            TextButton(onClick = { sessionError = null }) { Text("OK") }
                        },
                    )
                }
            }
        }
    }

    private fun handleEffect(effect: OrderFlowEffect) {
        when (effect) {
            OrderFlowEffect.ShowLogoutConfirmation -> showLogoutConfirmation = true
            OrderFlowEffect.NavigateToRegistration -> registrationLauncher.launch(
                Intent(this, RegistrationActivity::class.java),
            )
            is OrderFlowEffect.CopyPaymentText -> copyText("pix_code", effect.text, getString(R.string.payment_dialog_pix_copied))
            is OrderFlowEffect.CopySimulatorText -> copyText("Simulacao de Credito", effect.text, "Parcelamento copiado.")
            is OrderFlowEffect.ShareSimulatorText -> shareSimulatorText(effect.text)
            is OrderFlowEffect.ShowToast -> Toast.makeText(
                this,
                effect.message,
                if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
            is OrderFlowEffect.ShowSessionExpired -> {
                sessionError = effect.exception as? UnauthorizedException
            }
        }
    }

    private fun copyText(label: String, text: String, confirmation: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, confirmation, Toast.LENGTH_SHORT).show()
    }

    private fun shareSimulatorText(text: String) {
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, text),
                    "Compartilhar simulacao",
                ),
            )
        }.onFailure {
            Toast.makeText(this, "Nao foi possivel compartilhar a simulacao.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openOrderCamera(orderId: Int) {
        cameraCaptureError = null
        runCatching {
            val directory = File(cacheDir, "order_photos").apply { mkdirs() }
            val file = File.createTempFile("pedido_${orderId}_", ".jpg", directory)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
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

    private fun hasCameraCapture(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager) != null

    private fun getCreatedOrder(data: Intent?): Order? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        data?.getSerializableExtra(RegistrationActivity.EXTRA_CREATED_ORDER, Order::class.java)
    } else {
        @Suppress("DEPRECATION")
        data?.getSerializableExtra(RegistrationActivity.EXTRA_CREATED_ORDER) as? Order
    }

    private fun getSerialForPrePay(): String = if (BuildConfig.DEBUG) {
        DebugConstants.DEBUG_SPLIT_DEVICE_ID
    } else {
        DeviceUtils.getSerialNumber()
    }

    private fun logout() {
        showLogoutConfirmation = false
        homeViewModel.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun sessionErrorMessage(error: UnauthorizedException): String = buildString {
        append("Sua sessao foi rejeitada pelo backend. O login local foi mantido, mas esta tela nao consegue continuar.")
        if (error.endpoint.isNotBlank()) append("\n\nEndpoint: ${error.endpoint}")
        if (!error.backendMessage.isNullOrBlank()) append("\nDetalhe: ${error.backendMessage}")
    }
}
