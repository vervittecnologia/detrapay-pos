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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.ui.state.UIState
import com.detrapay.ui.theme.DetrapayTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OrderReportActivity : ComponentActivity() {

    private fun receiptTypeface(isBold: Boolean = false): Typeface {
        val baseTypeface = ResourcesCompat.getFont(this, R.font.font) ?: Typeface.SANS_SERIF
        return if (isBold) Typeface.create(baseTypeface, Typeface.BOLD) else baseTypeface
    }

    private val locale = Locale("pt", "BR")
    private val viewModel: OrderReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val order = intent.getSerializableExtra("order") as? Order
        if (order == null) {
            finish()
            return
        }

        setContent {
            DetrapayTheme {
                val printState by viewModel.printState.observeAsState(UIState.Idle())
                LaunchedEffect(printState) {
                    (printState as? UIState.Error)?.message?.let {
                        Toast.makeText(this@OrderReportActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
                OrderReportScreen(
                    order = order,
                    isPrinting = printState is UIState.Loading,
                    onBack = ::finish,
                    onPrint = { printReport(order) },
                )
            }
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

    private fun createPrintView(order: Order): View {
        val orderAmount = if (order.currentAmount > 0.0) {
            order.currentAmount
        } else {
            order.items.sumOf { (it.price ?: 0.0) - it.discount }
        }
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
            typeface = receiptTypeface(isBold = true)
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
            typeface = receiptTypeface()
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
            tv.typeface = if (isBold) receiptTypeface(isBold = true) else receiptTypeface()
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
        addTextView("VALOR BASE DO PEDIDO: R$ ${formatMoney(orderAmount)}", isBold = true, bottomMargin = 0)

        val footer = TextView(this).apply {
            text = "Pedido #${order.id}"
            setTextColor(Color.BLACK)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = receiptTypeface(isBold = true)
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
        return resources.displayMetrics.widthPixels
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderReportScreen(
    order: Order,
    isPrinting: Boolean,
    onBack: () -> Unit,
    onPrint: () -> Unit,
) {
    val locale = Locale("pt", "BR")
    fun money(value: Double) = "%,.2f".format(locale, value)
    fun document(value: String) = when (value.length) {
        11 -> "${value.substring(0, 3)}.${value.substring(3, 6)}.${value.substring(6, 9)}-${value.substring(9)}"
        14 -> "${value.substring(0, 2)}.${value.substring(2, 5)}.${value.substring(5, 8)}/${value.substring(8, 12)}-${value.substring(12)}"
        else -> value
    }
    val orderAmount = if (order.currentAmount > 0.0) order.currentAmount
    else order.items.sumOf { (it.price ?: 0.0) - it.discount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedido #${order.id}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
            )
        },
        bottomBar = {
            Button(
                onClick = onPrint,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                enabled = !isPrinting,
            ) {
                if (isPrinting) CircularProgressIndicator(strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Imprimir relatorio")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReportCard("Cliente") {
                Text(order.customer.name, style = MaterialTheme.typography.titleMedium)
                Text("CPF/CNPJ: ${document(order.customer.cpfCnpj)}")
                Text("Telefone: ${order.customer.phoneNumber}")
            }
            ReportCard("Dados do pedido") {
                Text("Servico: ${order.serviceName}")
                Text("Tipo do veiculo: ${order.vehicleType.name}")
                Text("Veiculo com alienacao: ${if (order.isVehicleFinanced) "Sim" else "Nao"}")
                Text("Placa especial: ${if (order.isVehicleSpecialPlate) "Sim" else "Nao"}")
                Text("Data de faturamento: ${order.billingDate}")
            }
            ReportCard("Itens") {
                if (order.items.isEmpty()) Text("Sem itens cadastrados")
                order.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.name ?: "Item", Modifier.weight(1f))
                        Text("R$ ${money((item.price ?: 0.0) - item.discount)}", fontWeight = FontWeight.SemiBold)
                    }
                    if (index < order.items.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
            ReportCard("Pagamentos") {
                if (order.receivables.isEmpty()) Text("Sem pagamentos registrados")
                order.receivables.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.paymentMethod.name, fontWeight = FontWeight.SemiBold)
                            Text(item.status.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("R$ ${money(item.amountFinal)}")
                    }
                    if (index < order.receivables.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Valor do veiculo: R$ ${money(order.vehiclePrice)}")
                    Text("Valor base do pedido: R$ ${money(orderAmount)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
