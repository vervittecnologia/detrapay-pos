package com.detrapay.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.databinding.PaymentDialogBinding
import com.detrapay.ui.state.UIState
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

class PaymentDialogFragment(
    private val listener: PaymentListener,
    private val orderId: Int,
    private val receivableItem: OrderReceivableItem,
    private val serial: String
) : DialogFragment() {

    private val locale = Locale("pt", "BR")
    private lateinit var binding: PaymentDialogBinding
    private val viewModel: PaymentDialogViewModel by activityViewModels()

    private var result: PaymentData? = null

    interface PaymentListener {
        fun onResult(paymentData: PaymentData?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PaymentDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupView(orderId, receivableItem)
        startPayment(orderId, receivableItem)
    }

    private fun startPayment(orderId: Int, receivable: OrderReceivableItem){
        viewModel.payOrder(orderId, receivable, serial)
    }

    private fun setupObservers() {
        viewModel.init()
        viewModel.paymentState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    status.message.let {
                        binding.transactionMessage.text = it
                    }
                    binding.successView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                }

                is UIState.Success -> {
                    status.data?.let {
                        result = it
                        binding.errorView.visibility = View.GONE
                        binding.loadingView.visibility = View.GONE
                        binding.successView.visibility = View.VISIBLE
                        showSuccessContent(it)
                    }
                }

                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.successView.visibility = View.GONE
                    binding.errorMessage.text = status.message ?: getString(R.string.employees_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
                is UIState.Idle -> {}
            }
        })
    }

    private fun setupView(orderId: Int, receivableItem: OrderReceivableItem) {
        val paymentMethodName = receivableItem.paymentMethod.name

        val amountFinalFormatted = "%,.2f".format(locale, receivableItem.amountFinal)
        binding.paymentAmount.text = "R$ $amountFinalFormatted"

        if (receivableItem.installments > 1) {
            val installmentAmount = receivableItem.amountFinal / receivableItem.installments
            val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)
            binding.paymentInstallments.visibility = View.VISIBLE
            binding.paymentInstallments.text = "em ${receivableItem.installments}x de R$ $installmentFormattedValue"
        } else {
            binding.paymentInstallments.visibility = View.GONE
        }

        binding.paymentMethod.text = paymentMethodName.uppercase()

        binding.retryAction.setOnClickListener {
            startPayment(orderId, receivableItem)
        }

        binding.closeDialog.setOnClickListener {
            viewModel.abortPayment()
            this.dismiss()
        }

        binding.backSuccessBtn.setOnClickListener {
            this.dismiss()
        }

        binding.copyPixCodeBtn.setOnClickListener {
            val pixCode = result?.pixCopyPasteCode?.takeIf { code -> code.isNotBlank() }
                ?: result?.pixQrCodeContent?.takeIf { code -> code.isNotBlank() }
                ?: return@setOnClickListener

            val clipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("pix_code", pixCode))
            Toast.makeText(requireContext(), "Codigo PIX copiado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessContent(paymentData: PaymentData) {
        if (!paymentData.pendingConfirmation) {
            binding.successMessage.text = "Pagamento realizado com sucesso!"
            binding.pixQrCodeImage.visibility = View.GONE
            binding.pixCopyPasteLabel.visibility = View.GONE
            binding.pixCopyPasteValue.visibility = View.GONE
            binding.pixMetaInfo.visibility = View.GONE
            binding.copyPixCodeBtn.visibility = View.GONE
            binding.backSuccessBtn.text = "Voltar ao pedido"
            return
        }

        binding.successMessage.text = "QR Code PIX gerado"
        binding.backSuccessBtn.text = "Fechar"

        val pixCode = paymentData.pixCopyPasteCode?.takeIf { it.isNotBlank() }
            ?: paymentData.pixQrCodeContent.orEmpty()

        binding.pixCopyPasteLabel.visibility = View.VISIBLE
        binding.pixCopyPasteValue.visibility = View.VISIBLE
        binding.copyPixCodeBtn.visibility = View.VISIBLE
        binding.pixCopyPasteValue.text = pixCode

        val metaInfo = buildList {
            paymentData.pixTxIdCode?.takeIf { it.isNotBlank() }?.let { add("TxId: $it") }
            paymentData.pixExpiresAt?.takeIf { it.isNotBlank() }?.let { add("Expira em: $it") }
        }.joinToString("\n")

        if (metaInfo.isBlank()) {
            binding.pixMetaInfo.visibility = View.GONE
        } else {
            binding.pixMetaInfo.visibility = View.VISIBLE
            binding.pixMetaInfo.text = metaInfo
        }

        val qrBitmap = paymentData.pixQrCodeBase64?.let(::decodeBase64Bitmap)
            ?: pixCode.takeIf { it.isNotBlank() }?.let(::generateQrBitmap)

        if (qrBitmap != null) {
            binding.pixQrCodeImage.visibility = View.VISIBLE
            binding.pixQrCodeImage.setImageBitmap(qrBitmap)
        } else {
            binding.pixQrCodeImage.visibility = View.GONE
        }
    }

    private fun decodeBase64Bitmap(base64Content: String): Bitmap? {
        return runCatching {
            val normalized = base64Content.substringAfter("base64,", base64Content)
            val decodedBytes = Base64.decode(normalized, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }.getOrNull()
    }

    private fun generateQrBitmap(content: String): Bitmap? {
        return runCatching {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 640, 640)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        }.getOrNull()
    }

    override fun onDismiss(dialog: DialogInterface) {
        listener.onResult(result)
        super.onDismiss(dialog)
    }
}
