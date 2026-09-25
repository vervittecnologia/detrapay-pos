package com.detrapay.ui.registration

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.Order
import com.detrapay.data.model.SimulationItem
import com.detrapay.ui.registration.order_data.RegistrationOrderInitialState
import com.detrapay.ui.state.UIState
import com.detrapay.ui.theme.DetrapayTheme
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.isValidCpnj
import com.detrapay.ui.util.isValidCpf
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class RegistrationActivity : ComponentActivity() {

    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initialize(orderFromIntent())
        setContent {
            DetrapayTheme {
                RegistrationRoute(
                    viewModel = viewModel,
                    onClose = ::finish,
                    onCreated = ::finishWithCreatedOrder,
                    onPrint = ::printRegistrationReceipt,
                )
            }
        }
    }

    private fun orderFromIntent(): Order? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra("order", Order::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getSerializableExtra("order") as? Order
    }

    private fun finishWithCreatedOrder(order: Order) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_CREATED_ORDER, order))
        finish()
    }

    private fun printRegistrationReceipt() {
        val path = createReceiptImage()
        if (path == null) {
            Toast.makeText(this, "Falha ao gerar o comprovante.", Toast.LENGTH_LONG).show()
        } else {
            viewModel.printOrderResume(path)
        }
    }

    private fun createReceiptImage(): String? = runCatching {
        // PlugPag's documented image width; text is sized for the printed roll.
        val width = 1155
        val padding = 48f
        data class ReceiptLine(val text: String, val size: Float = 64f, val bold: Boolean = false)
        val lines = buildList {
            add(ReceiptLine("RESUMO DO PEDIDO", 72f, true))
            add(ReceiptLine(""))
            viewModel.simulationCustomer()?.let { customer ->
                add(ReceiptLine("CLIENTE", 44f, true))
                add(ReceiptLine(customer.name))
                add(ReceiptLine("CPF/CNPJ", 44f, true))
                add(ReceiptLine(customer.cpfCnpj))
                add(ReceiptLine("WHATSAPP", 44f, true))
                add(ReceiptLine(customer.whatsapp))
            }
            add(ReceiptLine("CONCESSIONÁRIA", 44f, true))
            add(ReceiptLine(viewModel.getDealershipName()))
            add(ReceiptLine("VENDEDOR", 44f, true))
            add(ReceiptLine(viewModel.getSalesmanName()))
            add(ReceiptLine(""))
            add(ReceiptLine("SERVIÇOS", 52f, true))
            viewModel.simulationItems().forEach { item ->
                add(ReceiptLine(item.name, bold = true))
                add(ReceiptLine(formatCurrency(item.price)))
                item.discount?.takeIf { it > 0 }?.let {
                    add(ReceiptLine("Desconto: -" + formatCurrency(it), 56f))
                }
                add(ReceiptLine(""))
            }
            add(ReceiptLine("TOTAL", 52f, true))
            add(ReceiptLine(viewModel.simulationTotalAmount(), 88f, true))
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        fun style(line: ReceiptLine) {
            paint.textSize = line.size
            paint.typeface = android.graphics.Typeface.create("sans-serif", if (line.bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        val wrapped = lines.flatMap { line ->
            style(line)
            wrapReceiptLine(line.text, paint, width - padding * 2).map { line.copy(text = it) }
        }
        val logo = requireNotNull(androidx.appcompat.content.res.AppCompatResources.getDrawable(this, com.detrapay.R.drawable.receipt_logo)).mutate()
        val logoWidth = 690
        val logoHeight = (logoWidth.toFloat() * logo.intrinsicHeight / logo.intrinsicWidth).toInt()
        val textTop = padding + logoHeight + 52f
        val height = (textTop + wrapped.sumOf { line ->
            style(line)
            (if (line.text.isBlank()) 28f else paint.fontMetrics.descent - paint.fontMetrics.ascent + 22f).toDouble()
        } + 64f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val logoLeft = (width - logoWidth) / 2
            logo.setBounds(logoLeft, padding.toInt(), logoLeft + logoWidth, padding.toInt() + logoHeight)
            logo.draw(this)
            var y = textTop
            wrapped.forEach { line ->
                style(line)
                if (line.text.isBlank()) {
                    y += 28f
                } else {
                    val metrics = paint.fontMetrics
                    drawText(line.text, padding, y - metrics.ascent, paint)
                    y += metrics.descent - metrics.ascent + 22f
                }
            }
        }
        val directory = getExternalFilesDir(Environment.DIRECTORY_DCIM) ?: error("Diretorio indisponivel")
        File(directory, "registration_${System.currentTimeMillis()}.png").also { file ->
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.absolutePath
    }.getOrNull()

    private fun wrapReceiptLine(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
            if (count < remaining.length) {
                val space = remaining.lastIndexOf(' ', count)
                if (space > 0) count = space
                if (count < remaining.length && Character.isLowSurrogate(remaining[count])) count--
            }
            result += remaining.take(count)
            remaining = remaining.drop(count).trimStart()
        }
        return result
    }

    companion object {
        const val EXTRA_CREATED_ORDER = "createdOrder"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationRoute(
    viewModel: RegistrationViewModel,
    onClose: () -> Unit,
    onCreated: (Order) -> Unit,
    onPrint: () -> Unit,
) {
    val context = LocalContext.current
    val registration by viewModel.registrationState.observeAsState(RegistrationState(1))
    val initialState by viewModel.orderInitialState.observeAsState(UIState.Idle())
    val orderState by viewModel.orderDataState.observeAsState(UIState.Idle())
    val customerState by viewModel.orderDataClientSearchState.observeAsState(UIState.Idle())
    val createState by viewModel.paymentSelectionCreateOrderState.observeAsState(UIState.Idle())
    val printState by viewModel.orderResumePrintState.observeAsState(UIState.Idle())
    val formStateHolder = rememberSaveableStateHolder()
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscountDialog by rememberSaveable { mutableStateOf(false) }
    var unauthorized by remember { mutableStateOf<UnauthorizedException?>(null) }

    LaunchedEffect(initialState, orderState, customerState, createState) {
        listOf(initialState, orderState, customerState, createState)
            .mapNotNull { it.exception as? UnauthorizedException }
            .firstOrNull()
            ?.let { unauthorized = it }
    }
    LaunchedEffect(createState) {
        if (createState is UIState.Success) createState.data?.order?.let(onCreated)
        if (createState is UIState.Error) createState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(printState) {
        when (val state = printState) {
            is UIState.Success -> Toast.makeText(context, state.data, Toast.LENGTH_SHORT).show()
            is UIState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    BackHandler {
        if (registration.currentScreen > 1) viewModel.navigateBack() else showExitDialog = true
    }

    Box(Modifier.fillMaxSize()) {
        when (registration.currentScreen) {
            1 -> formStateHolder.SaveableStateProvider("order-form") { RegistrationDataScreen(
                viewModel = viewModel,
                initialState = initialState,
                orderState = orderState,
                customerState = customerState,
                onClose = { showExitDialog = true },
            ) }
            else -> RegistrationSummaryScreen(
                viewModel = viewModel,
                isCreating = createState is UIState.Loading,
                isPrinting = printState is UIState.Loading,
                onBack = viewModel::navigateBack,
                onClose = { showExitDialog = true },
                onAddDiscount = { showDiscountDialog = true },
                onPrint = onPrint,
                onCreateOrder = viewModel::createOrderWithoutPayments,
            )
        }

        if (createState is UIState.Loading || printState is UIState.Loading) {
            Box(
                Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 6.dp) {
                    Row(
                        Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Text(if (createState is UIState.Loading) "Criando pedido..." else "Imprimindo...")
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Sair do cadastro?") },
            text = { Text("Os dados ainda nao enviados serao descartados.") },
            confirmButton = { TextButton(onClick = onClose) { Text("Sair") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continuar") } },
        )
    }
    if (showDiscountDialog) {
        DiscountDialog(
            items = viewModel.simulationItemsWhoSupportDiscount(),
            onDismiss = { showDiscountDialog = false },
            onConfirm = { item, amount ->
                viewModel.addDiscountToSimulationItem(item, amount)
                showDiscountDialog = false
            },
        )
    }
    unauthorized?.let { error ->
        AlertDialog(
            onDismissRequest = { unauthorized = null },
            title = { Text("Sessao invalida ou expirada") },
            text = {
                Text(buildString {
                    append("Sua sessao foi rejeitada pelo backend. O login local foi mantido, mas esta tela nao consegue continuar.")
                    if (error.endpoint.isNotBlank()) append("\n\nEndpoint: ${error.endpoint}")
                    if (!error.backendMessage.isNullOrBlank()) append("\nDetalhe: ${error.backendMessage}")
                })
            },
            confirmButton = { TextButton(onClick = { unauthorized = null }) { Text("OK") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationDataScreen(
    viewModel: RegistrationViewModel,
    initialState: UIState<RegistrationOrderInitialState>,
    orderState: UIState<*>,
    customerState: UIState<*>,
    onClose: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("registration_preferences", Context.MODE_PRIVATE) }
    var cpfCnpj by rememberSaveable { mutableStateOf(preferences.getString("last_order_cpf_cnpj", "").orEmpty()) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var vehicleValue by rememberSaveable { mutableStateOf("") }
    var specialPlate by rememberSaveable { mutableStateOf(false) }
    var financed by rememberSaveable { mutableStateOf(false) }
    var selectedVehicleId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedSalesmanId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showVehicleTypes by rememberSaveable { mutableStateOf(false) }
    var choosingSalesman by rememberSaveable { mutableStateOf(true) }
    BackHandler(enabled = !choosingSalesman) { focusManager.clearFocus(); choosingSalesman = true }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var hydrated by rememberSaveable { mutableStateOf(false) }
    val data = if (initialState is UIState.Success) initialState.data else null

    LaunchedEffect(data) {
        val initial = data?.orderData ?: return@LaunchedEffect
        if (!hydrated) {
            cpfCnpj = formatDocument(initial.cpfCnpj)
            name = initial.name
            phone = formatPhone(initial.phone)
            date = backendDateToDisplay(initial.invoiceDate)
            vehicleValue = formatMoneyInput(initial.vehiclePrice)
            specialPlate = initial.specialPlate
            financed = initial.disposalVehicle
            selectedVehicleId = initial.vehicleType.id
            hydrated = true
        }
    }
    LaunchedEffect(customerState) {
        (customerState as? UIState.Success<CustomerSearchData>)?.data?.let { customer ->
            name = customer.name
            phone = formatPhone(customer.whatsapp)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            Column {
                LinearProgressIndicator(progress = { if (choosingSalesman) 1f / 3f else 2f / 3f }, modifier = Modifier.fillMaxWidth())
                TopAppBar(
                    navigationIcon = { if (!choosingSalesman) IconButton(onClick = { focusManager.clearFocus(); choosingSalesman = true }) { Icon(Icons.Default.ArrowBack, "Voltar aos vendedores") } },
                    title = {
                        Column {
                            Text(if (choosingSalesman) "Etapa 1 de 3" else "Etapa 2 de 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(if (choosingSalesman) "Escolha o vendedor" else "Dados do pedido", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    actions = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fechar") } },
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Button(
                    onClick = next@{
                        focusManager.clearFocus()
                        if (choosingSalesman) {
                            if (data?.salesmen.orEmpty().any { it.id != null && it.id == selectedSalesmanId }) {
                                choosingSalesman = false
                                submitError = null
                            }
                            return@next
                        }
                        val cleanDocument = Mask.replaceChars(cpfCnpj)
                        when {
                            !(isValidCpf(cpfCnpj) || isValidCpnj(cpfCnpj)) -> submitError = "Informe um CPF/CNPJ valido."
                            phone.filter(Char::isDigit).length < 10 -> submitError = "Informe um WhatsApp valido."
                            !validDisplayDate(date) -> submitError = "Informe uma data valida."
                            Mask.toSafeDouble(vehicleValue) <= 0 -> submitError = "Informe o valor do veiculo."
                            selectedVehicleId == null -> submitError = "Selecione o tipo de veiculo."
                            selectedSalesmanId == null -> submitError = "Selecione o vendedor."
                            else -> {
                                submitError = null
                                preferences.edit().putString("last_order_cpf_cnpj", cpfCnpj).apply()
                                viewModel.onOrderNext(
                                    cleanDocument,
                                    name,
                                    phone,
                                    date,
                                    vehicleValue,
                                    selectedVehicleId!!,
                                    financed,
                                    specialPlate,
                                    selectedSalesmanId,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    enabled = orderState !is UIState.Loading && initialState is UIState.Success &&
                        (!choosingSalesman || data?.salesmen.orEmpty().any { it.id != null && it.id == selectedSalesmanId }),
                ) {
                    if (orderState is UIState.Loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (choosingSalesman) "Continuar" else "Revisar pedido")
                }
            }
        },
    ) { padding ->
        when (initialState) {
            is UIState.Loading, is UIState.Idle -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UIState.Error -> ErrorState(
                message = initialState.message ?: "Nao foi possivel carregar o cadastro.",
                onRetry = viewModel::loadOrderScreenContent,
                modifier = Modifier.padding(padding),
            )
            is UIState.Success -> if (choosingSalesman) {
                SalesmanCards(
                    salesmen = data?.salesmen.orEmpty(),
                    selectedId = selectedSalesmanId,
                    onSelect = { selectedSalesmanId = it },
                    modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                )
            } else Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RegistrationInputField(
                    value = cpfCnpj,
                    onValueChange = { cpfCnpj = formatDocument(it); submitError = null },
                    label = "CPF/CNPJ",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (customerState is UIState.Loading) CircularProgressIndicator(Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                        else IconButton(onClick = { viewModel.searchClient(Mask.replaceChars(cpfCnpj)) }) { Icon(Icons.Default.Search, "Buscar cliente") }
                    },
                )
                if (customerState is UIState.Error) Text("Cliente nao encontrado.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                RegistrationInputField(
                    value = name,
                    onValueChange = { name = it; submitError = null },
                    label = "Nome do cliente",
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                RegistrationInputField(
                    value = phone,
                    onValueChange = { phone = formatPhone(it); submitError = null },
                    label = "WhatsApp",
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                RegistrationInputField(
                    value = vehicleValue,
                    onValueChange = { vehicleValue = formatMoneyInput(it); submitError = null },
                    label = "Valor do veículo",
                    prefix = { Text("R$ ", fontSize = 20.sp) },
                    imeAction = ImeAction.Done,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                DisabledPickerField(
                    value = date,
                    label = "Data de aquisição",
                    icon = { Icon(Icons.Default.CalendarMonth, null) },
                    onClick = { focusManager.clearFocus(); showDatePicker = true },
                )
                DisabledPickerField(
                    value = data?.vehicleTypes.orEmpty().firstOrNull { it.id == selectedVehicleId }?.name.orEmpty(),
                    label = "Tipo de veículo",
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary) },
                    icon = { Icon(Icons.Default.ExpandMore, null) },
                    onClick = { focusManager.clearFocus(); showVehicleTypes = true },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(financed, { financed = it })
                    Text("Veículo com alienação", Modifier.clickable { financed = !financed }, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(specialPlate, { specialPlate = it })
                    Text("Placa especial", Modifier.clickable { specialPlate = !specialPlate }, style = MaterialTheme.typography.bodyMedium)
                }
                (submitError ?: if (orderState is UIState.Error) orderState.message else null)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showVehicleTypes) {
        SelectionDialog(
            title = "Selecione o tipo de veículo",
            items = data?.vehicleTypes.orEmpty().sortedBy { it.id },
            selectedId = selectedVehicleId,
            itemId = { it.id },
            itemLabel = { it.name },
            onSelect = { selectedVehicleId = it.id; showVehicleTypes = false; submitError = null },
            onDismiss = { showVehicleTypes = false },
        )
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun SalesmanCards(
    salesmen: List<Salesman>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (salesmen.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Nenhum vendedor disponível.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    BoxWithConstraints(modifier) {
        val spacing = 4.dp
        val cardHeight = minOf(56.dp, (maxHeight - spacing * (salesmen.size - 1)) / salesmen.size)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            salesmen.forEach { salesman ->
                val selected = salesman.id != null && salesman.id == selectedId
                Surface(
                    modifier = Modifier.fillMaxWidth().height(cardHeight)
                        .selectable(selected = selected, enabled = salesman.id != null, onClick = { salesman.id?.let(onSelect) }),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.Badge, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            salesman.name.firstTwoNames().uppercase(Locale("pt", "BR")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun String.firstTwoNames(): String = trim().split(Regex("\\s+")).take(2).joinToString(" ")

@Composable
private fun RegistrationInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    imeAction: ImeAction = ImeAction.Next,
    prefix: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = label },
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
        singleLine = true,
        keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() },
        ),
        leadingIcon = leadingIcon,
        prefix = prefix,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisabledPickerField(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().semantics { contentDescription = label }.clickable(onClick = onClick)) {
        TextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled = false,
            label = { Text(label) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            singleLine = true,
            trailingIcon = icon,
            leadingIcon = leadingIcon,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledIndicatorColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.background,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationSummaryScreen(
    viewModel: RegistrationViewModel,
    isCreating: Boolean,
    isPrinting: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAddDiscount: () -> Unit,
    onPrint: () -> Unit,
    onCreateOrder: () -> Unit,
) {
    var revision by remember { mutableIntStateOf(0) }
    val payload = remember(revision) { viewModel.buildWhatsAppSharePayload() }
    val qrLink = payload?.waMeLink
    var qr by remember(qrLink) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(qrLink) {
        qr = withContext(Dispatchers.Default) { qrLink?.let(::qrBitmap) }
    }
    val items = remember(revision) { viewModel.simulationItems() }
    Scaffold(
        topBar = {
            Column {
                LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                TopAppBar(
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } },
                    title = {
                        Column {
                            Text("Etapa 3 de 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Revise o pedido")
                        }
                    },
                    actions = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fechar") } },
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrint, modifier = Modifier.fillMaxWidth(), enabled = !isPrinting && !isCreating) {
                        Icon(Icons.Default.Print, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Imprimir resumo")
                    }
                    Button(onClick = onCreateOrder, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !isCreating && !isPrinting) {
                        Text("Criar pedido")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Servicos", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAddDiscount, enabled = viewModel.canAddDiscount()) { Text("Adicionar desconto") }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    items.forEachIndexed { index, item ->
                        SummaryItem(
                            item = item,
                            onRemoveDiscount = {
                                viewModel.removeDiscount(item)
                                revision++
                            },
                        )
                        if (index < items.lastIndex) HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Text("Valor total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(viewModel.simulationTotalAmount(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (payload != null) {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Compartilhe pelo WhatsApp", style = MaterialTheme.typography.titleMedium)
                        Text("Aponte a camera para abrir o resumo do pedido.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        qr?.let { Image(it.asImageBitmap(), "QR Code do WhatsApp", Modifier.padding(top = 14.dp).size(220.dp)) }
                        Text("Destino: ${payload.formattedPhone}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryItem(item: SimulationItem, onRemoveDiscount: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            item.discount?.takeIf { it > 0 }?.let {
                Text("Desconto: -${formatCurrency(it)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatCurrency(item.price - (item.discount ?: 0.0)), style = MaterialTheme.typography.titleMedium)
            if ((item.discount ?: 0.0) > 0) IconButton(onClick = onRemoveDiscount) { Icon(Icons.Default.DeleteOutline, "Remover desconto") }
        }
    }
}

@Composable
private fun DiscountDialog(
    items: List<SimulationItem>,
    onDismiss: () -> Unit,
    onConfirm: (SimulationItem, Double) -> Unit,
) {
    var selected by remember(items) { mutableStateOf(items.firstOrNull()) }
    var amount by rememberSaveable { mutableStateOf("") }
    val value = Mask.toSafeDouble(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar desconto") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (items.isEmpty()) Text("Nenhum item disponivel para desconto.")
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected == item) { selected = item },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == item, onClick = { selected = item })
                        Column {
                            Text(item.name)
                            Text("Maximo: ${formatCurrency(item.price)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = formatMoneyInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Valor do desconto", fontSize = 16.sp) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                    prefix = { Text("R$ ", fontSize = 22.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = value > (selected?.price ?: 0.0),
                    supportingText = if (value > (selected?.price ?: 0.0)) ({ Text("O desconto nao pode superar o valor do item.") }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { selected?.let { onConfirm(it, value) } }, enabled = selected != null && value > 0 && value <= (selected?.price ?: 0.0)) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    items: List<T>,
    selectedId: Int?,
    itemId: (T) -> Int?,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().selectable(itemId(item) == selectedId) { onSelect(item) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(itemId(item) == selectedId, onClick = { onSelect(item) })
                        Text(itemLabel(item), Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Tentar novamente") }
    }
}

private fun formatDocument(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(14)
    return if (digits.length <= 11) {
        buildString {
            digits.forEachIndexed { index, char ->
                append(char)
                if ((index == 2 || index == 5) && index < digits.lastIndex) append('.')
                if (index == 8 && index < digits.lastIndex) append('-')
            }
        }
    } else {
        buildString {
            digits.forEachIndexed { index, char ->
                append(char)
                if ((index == 1 || index == 4) && index < digits.lastIndex) append('.')
                if (index == 7 && index < digits.lastIndex) append('/')
                if (index == 11 && index < digits.lastIndex) append('-')
            }
        }
    }
}

private fun formatPhone(raw: String): String {
    val digits = raw.filter(Char::isDigit).takeLast(11)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 0) append('(')
            append(char)
            if (index == 1) append(") ")
            if (index == 6 && index < digits.lastIndex) append('-')
        }
    }
}

private fun formatMoneyInput(raw: String): String {
    val normalized = raw.trim()
    if (normalized.matches(Regex("\\d{1,3}(\\.\\d{3})*,\\d{2}"))) return normalized
    val digits = raw.filter(Char::isDigit)
    if (digits.isBlank()) return ""
    val cents = digits.toLongOrNull() ?: 0L
    return NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(cents / 100.0)
}

private fun backendDateToDisplay(value: String): String = runCatching {
    if (value.length >= 10 && value[4] == '-') "${value.substring(8, 10)}/${value.substring(5, 7)}/${value.substring(0, 4)}" else value
}.getOrDefault(value)

private fun validDisplayDate(value: String): Boolean = runCatching {
    java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/uuuu")).year >= 2025
}.getOrDefault(false)

private fun formatCurrency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun qrBitmap(content: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 640, 640)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) Color.BLACK else Color.WHITE
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}.getOrNull()
