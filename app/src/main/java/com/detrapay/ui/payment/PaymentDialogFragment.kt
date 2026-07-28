package com.detrapay.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.detrapay.R
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.databinding.PaymentDialogBinding
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.InstallmentQuotePresenter
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun startPayment(orderId: Int, receivable: OrderReceivableItem){
        viewModel.payOrder(orderId, receivable, serial)
    }

    private fun setupObservers() {
        viewModel.init()
        viewModel.paymentState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    binding.dialogTitle.text = getString(R.string.payment_dialog_title_processing)
                    binding.transactionMessage.text =
                        status.message ?: getString(R.string.payment_dialog_loading_default)
                    binding.loadingSupportMessage.visibility = View.VISIBLE
                    binding.loadingSupportMessage.text = getString(R.string.payment_dialog_support_loading)
                    binding.successView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                }

                is UIState.Success -> {
                    binding.dialogTitle.text = getString(R.string.payment_dialog_title_success)
                    status.data?.let {
                        result = it
                        binding.errorView.visibility = View.GONE
                        binding.loadingView.visibility = View.GONE
                        binding.successView.visibility = View.VISIBLE
                        showSuccessContent(it)
                    }
                }

                is UIState.Error -> {
                    binding.dialogTitle.text = getString(R.string.payment_dialog_title_error)
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

        if (receivableItem.installments > 1) {
            val presentation = InstallmentQuotePresenter.present(
                amountOriginal = receivableItem.amountOriginal,
                amountFinal = receivableItem.amountFinal,
                installments = receivableItem.installments,
            )
            binding.paymentAmount.text = presentation.totalLabel
            binding.paymentInstallments.visibility = View.VISIBLE
            binding.paymentInstallments.text =
                "${presentation.originalLabel}\n${presentation.installmentLabel}"
        } else {
            val amountFinalFormatted = "%,.2f".format(locale, receivableItem.amountFinal)
            binding.paymentAmount.text = "R$ $amountFinalFormatted"
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
            Toast.makeText(requireContext(), getString(R.string.payment_dialog_pix_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessContent(paymentData: PaymentData) {
        if (!paymentData.pendingConfirmation) {
            binding.successMessage.text = getString(R.string.payment_dialog_success_title)
            binding.successSupportMessage.visibility = View.VISIBLE
            binding.successSupportMessage.text = getString(R.string.payment_dialog_support_success)
            binding.pixQrCodeImage.visibility = View.GONE
            binding.pixCopyPasteLabel.visibility = View.GONE
            binding.pixCopyPasteValue.visibility = View.GONE
            val successMeta = buildList {
                paymentData.date?.takeIf { it.isNotBlank() }?.let { date ->
                    val time = paymentData.time?.takeIf { value -> value.isNotBlank() }
                    if (time != null) {
                        add(getString(R.string.payment_dialog_meta_date_time, date, time))
                    } else {
                        add(getString(R.string.payment_dialog_meta_date, date))
                    }
                }
                paymentData.transactionId?.takeIf { it.isNotBlank() }?.let {
                    add(getString(R.string.payment_dialog_meta_transaction, it))
                }
            }.joinToString("\n")

            if (successMeta.isBlank()) {
                binding.pixMetaInfo.visibility = View.GONE
            } else {
                binding.pixMetaInfo.visibility = View.VISIBLE
                binding.pixMetaInfo.text = successMeta
            }
            binding.copyPixCodeBtn.visibility = View.GONE
            binding.backSuccessBtn.text = getString(R.string.payment_dialog_success_continue)
            return
        }

        binding.successMessage.text = getString(R.string.payment_dialog_pix_generated)
        binding.successSupportMessage.visibility = View.VISIBLE
        binding.successSupportMessage.text = getString(R.string.payment_dialog_support_pix)
        binding.backSuccessBtn.text = getString(R.string.payment_dialog_success_close)

        val pixCode = paymentData.pixCopyPasteCode?.takeIf { it.isNotBlank() }
            ?: paymentData.pixQrCodeContent.orEmpty()

        binding.pixCopyPasteLabel.visibility = View.VISIBLE
        binding.pixCopyPasteValue.visibility = View.VISIBLE
        binding.copyPixCodeBtn.visibility = View.VISIBLE
        binding.pixCopyPasteValue.text = pixCode

        val metaInfo = buildList {
            paymentData.pixTxIdCode?.takeIf { it.isNotBlank() }?.let {
                add(getString(R.string.payment_dialog_meta_txid, it))
            }
            paymentData.pixExpiresAt?.takeIf { it.isNotBlank() }?.let {
                add(getString(R.string.payment_dialog_meta_expires, it))
            }
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
