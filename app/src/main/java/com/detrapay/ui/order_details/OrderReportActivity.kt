package com.detrapay.ui.order_details

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.databinding.ActivityOrderReportBinding
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OrderReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderReportBinding
    private val locale = Locale("pt", "BR")
    private val viewModel: OrderReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val order = intent.getSerializableExtra("order") as? Order
        if (order == null) {
            finish()
            return
        }

        setupToolbar()
        setupObservers()
        bindReport(order)
        setupPrint(order)
    }

    private fun setupToolbar() {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_left)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun bindReport(order: Order) {
        binding.reportTitle.text = "Pedido #${order.id}"
        binding.reportSubtitle.visibility = View.GONE
        binding.clientName.text = order.customer.name
        binding.clientDocument.text = "CPF/CNPJ: ${formatCpfCnpj(order.customer.cpfCnpj)}"
        binding.clientPhone.text = "Telefone: ${order.customer.phoneNumber}"

        binding.registrationService.text = "Servico: ${order.serviceName}"
        binding.registrationVehicleType.text = "Tipo do veiculo: ${order.vehicleType.name}"
        binding.registrationFinanced.text = "Veiculo com alienacao: ${if (order.isVehicleFinanced) "Sim" else "Nao"}"
        binding.registrationSpecialPlate.text = "Placa especial: ${if (order.isVehicleSpecialPlate) "Sim" else "Nao"}"
        binding.registrationBillingDate.text = "Data de faturamento: ${order.billingDate}"

        val orderAmount = if (order.currentAmount > 0.0) {
            order.currentAmount
        } else {
            order.items.sumOf { (it.price ?: 0.0) - it.discount }
        }
        val totalFinalAmount = order.receivables.sumOf { it.amountFinal }

        binding.valueVehicle.text = "Valor do veiculo: R$ ${formatMoney(order.vehiclePrice)}"
        binding.valueBase.text = "Valor base do pedido: R$ ${formatMoney(orderAmount)}"
        binding.valueFinal.text = "Valor total com juros: R$ ${formatMoney(totalFinalAmount)}"

        binding.itemsContent.text = buildItemsBlock(order)
        binding.paymentsContent.text = buildPaymentsBlock(order)
    }

    private fun setupPrint(order: Order) {
        binding.btnPrintReport.setOnClickListener {
            printReport(order)
        }
    }

    private fun buildItemsBlock(order: Order): String {
        return if (order.items.isEmpty()) {
            "- Sem itens cadastrados"
        } else {
            order.items.joinToString("\n") {
                val value = "R$ ${formatMoney((it.price ?: 0.0) - it.discount)}"
                "- ${it.name ?: "Item"}: $value"
            }
        }
    }

    private fun buildPaymentsBlock(order: Order): String {
        return if (order.receivables.isEmpty()) {
            "- Sem pagamentos registrados"
        } else {
            order.receivables.joinToString("\n") { formatReceivable(it) }
        }
    }

    private fun printReport(order: Order) {
        try {
            val dir = getExternalFilesDir(Environment.DIRECTORY_DCIM)
            val printView = createPrintView(order)
            val path = captureViewForPrint(dir, printView)

            if (path == null) {
                Toast.makeText(this, "Falha ao iniciar impressao", Toast.LENGTH_LONG).show()
                return
            }

            viewModel.printReport(path)
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao iniciar impressao", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewModel.printState.observe(this) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.btnPrintReport.isEnabled = false
                    binding.btnPrintReport.alpha = 0.6f
                }
                is UIState.Success -> {
                    binding.btnPrintReport.isEnabled = true
                    binding.btnPrintReport.alpha = 1f
                }
                is UIState.Error -> {
                    binding.btnPrintReport.isEnabled = true
                    binding.btnPrintReport.alpha = 1f
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is UIState.Idle -> {
                    binding.btnPrintReport.isEnabled = true
                    binding.btnPrintReport.alpha = 1f
                }
            }
        }
    }

    private fun createPrintView(order: Order): View {
        val orderAmount = if (order.currentAmount > 0.0) {
            order.currentAmount
        } else {
            order.items.sumOf { (it.price ?: 0.0) - it.discount }
        }
        val totalFinalAmount = order.receivables.sumOf { it.amountFinal }
        val printWidth = resolvePrintWidth()
        val horizontalPadding = 24
        val logoWidth = ((printWidth - (horizontalPadding * 2)) * 0.96f).toInt()

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(printWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
            setPadding(horizontalPadding, 24, horizontalPadding, 24)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logotipo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(Color.BLACK)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                logoWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 8
            }
        }
        container.addView(logo)

        val brandTitle = TextView(this).apply {
            text = getString(R.string.home_brand_name).lowercase(Locale.getDefault())
            setTextColor(Color.BLACK)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4
            }
        }
        container.addView(brandTitle)

        val slogan = TextView(this).apply {
            text = getString(R.string.print_slogan)
            setTextColor(Color.BLACK)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.SANS_SERIF
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }
        container.addView(slogan)

        fun addTextView(
            text: String,
            isBold: Boolean = false,
            sizeSp: Float = 20f,
            gravity: Int = Gravity.NO_GRAVITY,
            bottomMargin: Int = 6
        ) {
            val tv = TextView(this)
            tv.text = text
            tv.setTextColor(Color.BLACK)
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp)
            tv.typeface = if (isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
            tv.letterSpacing = 0.01f
            tv.gravity = gravity
            tv.layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.bottomMargin = bottomMargin
            }
            container.addView(tv)
        }

        fun addDivider(topMargin: Int = 14, bottomMargin: Int = 14) {
            val divider = View(this)
            divider.layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                3
            ).apply {
                setMargins(0, topMargin, 0, bottomMargin)
            }
            divider.setBackgroundColor(Color.BLACK)
            container.addView(divider)
        }

        addTextView("PEDIDO #${order.id}", isBold = true, sizeSp = 24f, gravity = Gravity.CENTER_HORIZONTAL, bottomMargin = 12)
        addTextView("CLIENTE: ${order.customer.name}", isBold = true)
        addTextView("CPF/CNPJ: ${formatCpfCnpj(order.customer.cpfCnpj)}")
        addTextView("TELEFONE: ${order.customer.phoneNumber}")
        addTextView("SERVICO: ${order.serviceName}")
        addTextView("TIPO: ${order.vehicleType.name}")
        addTextView("ALIENACAO: ${if (order.isVehicleFinanced) "Sim" else "Nao"}")
        addTextView("PLACA ESPECIAL: ${if (order.isVehicleSpecialPlate) "Sim" else "Nao"}")
        addTextView("DATA DE FATURAMENTO: ${order.billingDate}")

        addDivider()

        addTextView("ITENS", isBold = true)
        if (order.items.isEmpty()) {
            addTextView("SEM ITENS CADASTRADOS", sizeSp = 19f)
        } else {
            order.items.forEach { item ->
                val value = formatMoney((item.price ?: 0.0) - item.discount)
                addTextView("${item.name ?: "Item"}: R$ $value", sizeSp = 19f)
            }
        }

        addDivider()

        addTextView("PAGAMENTOS", isBold = true)
        if (order.receivables.isEmpty()) {
            addTextView("SEM PAGAMENTOS REGISTRADOS", sizeSp = 19f)
        } else {
            order.receivables.forEach { item ->
                addTextView(formatReceivable(item), sizeSp = 19f)
            }
        }

        addDivider()

        addTextView("VALOR DO VEICULO: R$ ${formatMoney(order.vehiclePrice)}", isBold = true)
        addTextView("VALOR BASE DO PEDIDO: R$ ${formatMoney(orderAmount)}", isBold = true)
        addTextView("VALOR TOTAL COM JUROS: R$ ${formatMoney(totalFinalAmount)}", isBold = true, sizeSp = 24f, bottomMargin = 0)

        val footer = TextView(this).apply {
            text = "Pedido #${order.id}"
            setTextColor(Color.BLACK)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 14
            }
        }
        container.addView(footer)

        return container
    }

    private fun resolvePrintWidth(): Int {
        val contentWidth = binding.root.width - binding.root.paddingStart - binding.root.paddingEnd
        return if (contentWidth > 0) contentWidth else resources.displayMetrics.widthPixels
    }

    private fun captureViewForPrint(dir: File?, view: View): String? {
        return try {
            if (dir == null) {
                return null
            }

            val widthSpec = View.MeasureSpec.makeMeasureSpec(view.layoutParams.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            view.draw(canvas)

            val imageFile = File(dir, "order_report_${Date().time}.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            imageFile.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatReceivable(item: OrderReceivableItem): String {
        val type = item.paymentMethod.name
        val status = item.status.toString()
        val amount = "R$ ${formatMoney(item.amountFinal)}"
        return "${type}: $amount (${status})"
    }

    private fun formatMoney(value: Double): String = "%,.2f".format(locale, value)

    private fun formatCpfCnpj(cpfCnpj: String): String {
        return when (cpfCnpj.length) {
            11 -> "${cpfCnpj.substring(0, 3)}.${cpfCnpj.substring(3, 6)}.${cpfCnpj.substring(6, 9)}-${cpfCnpj.substring(9, 11)}"
            14 -> "${cpfCnpj.substring(0, 2)}.${cpfCnpj.substring(2, 5)}.${cpfCnpj.substring(5, 8)}/${cpfCnpj.substring(8, 12)}-${cpfCnpj.substring(12, 14)}"
            else -> cpfCnpj
        }
    }
}
