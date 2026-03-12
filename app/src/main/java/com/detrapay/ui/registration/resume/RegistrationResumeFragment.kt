package com.detrapay.ui.registration.resume

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.FragmentRegistrationOrderResumeBinding
import com.detrapay.ui.order_details.OrderDetailsActivity
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.registration.discount_dialog.DiscountDialogFragment
import com.detrapay.ui.registration.resume.RegistrationResumeRecyclerViewAdapter.OnItemClickListener
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

class RegistrationResumeFragment : Fragment() {

    private fun receiptTypeface(isBold: Boolean = false): Typeface {
        val baseTypeface = ResourcesCompat.getFont(requireContext(), R.font.font) ?: Typeface.SANS_SERIF
        return if (isBold) Typeface.create(baseTypeface, Typeface.BOLD) else baseTypeface
    }

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationOrderResumeBinding
    private lateinit var adapter: RegistrationResumeRecyclerViewAdapter

    companion object {
        const val REQUEST_EXTERNAL_STORAGE = 1
        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationOrderResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            registrationViewModel.navigateBack()
            findNavController().popBackStack()
        }

        binding.btnClose.setOnClickListener {
            (activity as? RegistrationActivity)?.showExitConfirmation()
        }

        binding.registrationOrderResumeNextBtn.setOnClickListener {
            registrationViewModel.createOrderWithoutPayments()
        }
        binding.printResumeButton.setOnClickListener {
            printResume()
        }

        binding.btnMore.setOnClickListener {
            showOverflowMenu(it)
        }

        binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
        refreshWhatsAppShareSection()

        adapter = RegistrationResumeRecyclerViewAdapter(
            registrationViewModel.simulationItems(),
            object : OnItemClickListener {
                override fun onRemoveDiscount(item: SimulationItem, itemPosition: Int) {
                    val position = registrationViewModel.removeDiscount(item)
                    updateItem(position)
                }
            })

        binding.rvOrderDetailed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrderDetailed.adapter = adapter
        binding.rvOrderDetailed.isNestedScrollingEnabled = false
    }

    private fun showOverflowMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 1, 0, getString(R.string.registration_resume_add_discount))
        popup.menu.add(0, 2, 1, getString(R.string.registration_resume_print_menu))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> showDiscountDialog()
                2 -> printResume()
            }
            true
        }
        popup.show()
    }

    private fun showDiscountDialog() {
        if (!registrationViewModel.canAddDiscount()) {
            Toast.makeText(requireContext(), "Nenhum item disponivel para desconto.", Toast.LENGTH_SHORT).show()
            return
        }
        val discountDialogFragment = DiscountDialogFragment(listener = object : DiscountDialogFragment.OnUpdateListener {
            override fun onUpdate(itemPosition: Int?) {
                updateItem(itemPosition)
            }
        })
        discountDialogFragment.show(parentFragmentManager, "DiscountDialogFragment")
    }

    private fun printResume() {
        verifyStoragePermissions(requireActivity())
        val dir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DCIM)

        val printView = createPrintView()
        val path = captureViewForPrint(dir, printView)
        if (path != null) {
            registrationViewModel.printOrderResume(path)
        } else {
            Toast.makeText(requireContext(), "Falha ao realizar impressao", Toast.LENGTH_LONG).show()
        }
    }

    private fun createPrintView(): View {
        val printWidth = resolvePrintWidth()
        val horizontalPadding = 24
        val logoWidth = ((printWidth - (horizontalPadding * 2)) * 0.96f).toInt()

        val container = android.widget.LinearLayout(requireContext())
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.layoutParams = ViewGroup.LayoutParams(printWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        container.setBackgroundColor(android.graphics.Color.WHITE)
        container.setPadding(horizontalPadding, 24, horizontalPadding, 24)

        val logo = ImageView(requireContext())
        logo.setImageResource(R.drawable.logotipo)
        logo.adjustViewBounds = true
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        logo.setColorFilter(android.graphics.Color.BLACK)
        logo.layoutParams = android.widget.LinearLayout.LayoutParams(
            logoWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 5
        }
        container.addView(logo)

        val brandTitle = TextView(requireContext())
        brandTitle.text = getString(R.string.home_brand_name).lowercase(Locale.getDefault())
        brandTitle.setTextColor(android.graphics.Color.BLACK)
        brandTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f)
        brandTitle.typeface = receiptTypeface(isBold = true)
        brandTitle.gravity = Gravity.CENTER_HORIZONTAL
        brandTitle.layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 2
        }
        container.addView(brandTitle)

        val slogan = TextView(requireContext())
        slogan.text = getString(R.string.print_slogan)
        slogan.setTextColor(android.graphics.Color.BLACK)
        slogan.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        slogan.typeface = receiptTypeface()
        slogan.gravity = Gravity.CENTER_HORIZONTAL
        slogan.layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 10
        }
        container.addView(slogan)

        fun addTextView(
            text: String,
            isBold: Boolean = false,
            sizeSp: Float = 20f,
            bottomMargin: Int = 6
        ) {
            val tv = TextView(requireContext())
            tv.text = text
            tv.setTextColor(android.graphics.Color.BLACK)
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
            tv.typeface = if (isBold) {
                receiptTypeface(isBold = true)
            } else {
                receiptTypeface()
            }
            tv.letterSpacing = 0.01f
            tv.layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.bottomMargin = bottomMargin
            }
            container.addView(tv)
        }

        fun addDivider(topMargin: Int = 14, bottomMargin: Int = 14) {
            val divider = View(requireContext())
            divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                3
            ).apply {
                setMargins(0, topMargin, 0, bottomMargin)
            }
            divider.setBackgroundColor(android.graphics.Color.BLACK)
            container.addView(divider)
        }

        addTextView("CONCESSIONARIA: ${registrationViewModel.getDealershipName()}", isBold = true)
        addTextView("VENDEDOR: ${registrationViewModel.getSalesmanName()}")

        registrationViewModel.simulationSimulation()?.let { sim ->
            addTextView("TIPO: ${registrationViewModel.getVehicleTypeName(sim.vehicleTypeId)}")
            addTextView("VALOR DO VEICULO: R$ ${sim.vehiclePrice}")
            addTextView("DATA DE AQUISICAO: ${sim.billingDate}")
        }

        addDivider()

        registrationViewModel.simulationItems().forEach { item ->
            val price = "%,.2f".format(java.util.Locale("pt", "BR"), item.price)
            addTextView("${item.name}: R$ $price", sizeSp = 19f)
            if (item.discount != null && item.discount > 0) {
                val disc = "%,.2f".format(java.util.Locale("pt", "BR"), item.discount)
                addTextView("DESCONTO: - R$ $disc", isBold = true, sizeSp = 18f)
            }
        }

        addDivider()
        addTextView(
            "VALOR TOTAL: ${registrationViewModel.simulationTotalAmount()}",
            isBold = true,
            sizeSp = 24f,
            bottomMargin = 0
        )

        val footer = TextView(requireContext())
        footer.text = "Resumo do pedido"
        footer.setTextColor(android.graphics.Color.BLACK)
        footer.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        footer.typeface = receiptTypeface(isBold = true)
        footer.gravity = Gravity.CENTER_HORIZONTAL
        footer.layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 14
        }
        container.addView(footer)

        return container
    }

    private fun resolvePrintWidth(): Int {
        val contentWidth = binding.root.width - binding.root.paddingStart - binding.root.paddingEnd
        return if (contentWidth > 0) {
            contentWidth
        } else {
            resources.displayMetrics.widthPixels
        }
    }

    private fun captureViewForPrint(dir: File?, view: View): String? {
        return try {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(view.layoutParams.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            view.draw(canvas)

            val now = Date()
            val imageFile = File(dir, "print_${now.time}.png")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            imageFile.toString()
        } catch (e: Throwable) {
            Logger.d("Capture Error: ${e.message}")
            null
        }
    }

    private fun setupObservers() {
        registrationViewModel.paymentSelectionCreateOrderState.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UIState.Loading -> {
                    binding.registrationOrderResumeNextBtn.isEnabled = false
                    binding.registrationOrderResumeNextBtn.text = getString(R.string.registration_creating_order_button)
                }
                is UIState.Success -> {
                    binding.registrationOrderResumeNextBtn.isEnabled = false
                    binding.registrationOrderResumeNextBtn.text = getString(R.string.registration_create_order_button)
                    status.data?.order?.let(::openDetailsScreen)
                }
                is UIState.Error -> {
                    binding.registrationOrderResumeNextBtn.isEnabled = true
                    binding.registrationOrderResumeNextBtn.text = getString(R.string.registration_create_order_button)
                    Toast.makeText(
                        requireContext(),
                        status.message ?: getString(R.string.registration_default_error_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is UIState.Idle -> {
                    binding.registrationOrderResumeNextBtn.isEnabled = true
                    binding.registrationOrderResumeNextBtn.text = getString(R.string.registration_create_order_button)
                }
            }
        }

        registrationViewModel.orderResumePrintState.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UIState.Success -> {
                    binding.printProgressView.visibility = View.GONE
                }
                is UIState.Error -> {
                    binding.printProgressView.visibility = View.GONE
                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                }
                is UIState.Loading -> {
                    binding.printProgressView.visibility = View.VISIBLE
                }
                is UIState.Idle -> {}
            }
        }
    }

    private fun verifyStoragePermissions(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
        }
    }

    private fun openDetailsScreen(order: Order) {
        val orderDetailsActivityIntent = android.content.Intent(
            requireContext(),
            OrderDetailsActivity::class.java
        )
        orderDetailsActivityIntent.putExtra("order", order)
        orderDetailsActivityIntent.putExtra("orderId", order.id)
        orderDetailsActivityIntent.putExtra("isSuccess", false)
        startActivity(orderDetailsActivityIntent)
        requireActivity().finish()
    }

    private fun updateItem(itemPosition: Int?) {
        if (itemPosition != null) {
            adapter.updateItem(registrationViewModel.simulationItems(), itemPosition)
            binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
            refreshWhatsAppShareSection()
        }
    }

    private fun refreshWhatsAppShareSection() {
        val payload = registrationViewModel.buildWhatsAppSharePayload()
        if (payload == null) {
            binding.whatsappShareCard.visibility = View.GONE
            return
        }

        binding.whatsappShareCard.visibility = View.VISIBLE
        binding.whatsappTargetText.text = getString(
            R.string.registration_resume_whatsapp_target,
            payload.formattedPhone
        )

        val qrBitmap = generateQrBitmap(payload.waMeLink)
        if (qrBitmap != null) {
            binding.whatsappQrImage.setImageBitmap(qrBitmap)
            binding.whatsappQrImage.visibility = View.VISIBLE
        } else {
            binding.whatsappQrImage.visibility = View.GONE
        }
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
}
