package com.detrapay.ui.registration.order_data

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.VehicleType
import com.detrapay.databinding.FragmentRegistrationOrderDataBinding
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.isValidCpnj
import com.detrapay.ui.util.isValidCpf
import java.util.Date

class RegistrationOrderDataFragment : Fragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private var canNavigate = false
    private var selectedVehicle: VehicleType? = null
    private var selectedSalesman: Salesman? = null
    private var selectedSalesmanId: Int? = null
    private lateinit var binding: FragmentRegistrationOrderDataBinding
    private val registrationPreferences: SharedPreferences by lazy {
        requireContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRegistrationOrderDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        registrationViewModel.loadLoggedUser()
        registrationViewModel.loadOrderScreenContent()
        configureDropdownInputs()
        configureFieldWatchers()
        configureDatePicker()

        binding.reloadRegistration.setOnClickListener {
            registrationViewModel.loadOrderScreenContent()
        }

        binding.closeRegistrationFlowButton.setOnClickListener {
            (activity as? RegistrationActivity)?.showExitConfirmation()
        }

        binding.whatsappInput.addTextChangedListener(
            Mask.mask("(##)#####-####", binding.whatsappInput),
        )

        binding.vehicleValueInput.addTextChangedListener(
            Mask.moneyMask(binding.vehicleValueInput, {}),
        )

        binding.cpfCnpj.addTextChangedListener(Mask.cpfCnpjMask(binding.cpfCnpj))
        restoreLastCpfCnpj()

        binding.cpfCnpjTextInputLayout.setEndIconOnClickListener {
            clearSubmitError()
            val cpfCnpj = Mask.replaceChars(binding.cpfCnpj.text.toString())
            registrationViewModel.searchClient(cpfCnpj)
        }

        binding.registrationOrderDataNextBtn.setOnClickListener {
            submitOrderData()
        }
    }

    private fun configureFieldWatchers() {
        binding.cpfCnpj.doAfterTextChanged {
            clearSubmitError()
            binding.cpfCnpjTextInputLayout.error = null
            saveLastCpfCnpj(it?.toString().orEmpty())
        }
        binding.clientNameInput.doAfterTextChanged {
            clearSubmitError()
            binding.clientNameTextInputLayout.error = null
        }
        binding.whatsappInput.doAfterTextChanged {
            clearSubmitError()
            binding.whatsappTextInputLayout.error = null
        }
        binding.invoiceDateInput.doAfterTextChanged {
            clearSubmitError()
            binding.invoiceDateTextInputLayout.error = null
        }
        binding.vehicleValueInput.doAfterTextChanged {
            clearSubmitError()
            binding.vehicleValueTextInputLayout.error = null
        }
    }

    private fun configureDatePicker() {
        val watcher = Mask.mask("##/##/####", binding.invoiceDateInput)
        binding.invoiceDateInput.addTextChangedListener(watcher)
        binding.invoiceDateInput.keyListener = null
        binding.invoiceDateInput.showSoftInputOnFocus = false
        binding.invoiceDateInput.isFocusable = false
        binding.invoiceDateInput.isClickable = true

        val openPicker = View.OnClickListener {
            openDatePicker(watcher)
        }

        binding.invoiceDateInput.setOnClickListener(openPicker)
        binding.invoiceDateTextInputLayout.setEndIconOnClickListener { openDatePicker(watcher) }
        binding.invoiceDateTextInputLayout.setOnClickListener(openPicker)
    }

    private fun openDatePicker(dateWatcher: TextWatcher) {
        clearSubmitError()
        binding.invoiceDateTextInputLayout.error = null
        binding.invoiceDateInput.removeTextChangedListener(dateWatcher)
        hideKeyboard(binding.invoiceDateInput)

        val now = Date()
        val dialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val stringDay = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
            val displayMonth = month + 1
            val stringMonth = if (displayMonth < 10) "0$displayMonth" else displayMonth.toString()
            binding.invoiceDateInput.setText("$stringDay/$stringMonth/$year")
            binding.invoiceDateInput.addTextChangedListener(dateWatcher)
        }, now.year + 1900, now.month, now.date)

        dialog.setOnCancelListener {
            binding.invoiceDateInput.addTextChangedListener(dateWatcher)
        }
        dialog.show()
    }

    private fun submitOrderData() {
        clearSubmitError()
        var hasInvalidFields = false

        if (selectedSalesmanId == null) {
            hasInvalidFields = true
            binding.salesmanTextInputLayout.error = "Campo obrigatorio"
        } else {
            binding.salesmanTextInputLayout.error = null
        }

        val cpfCnpj = binding.cpfCnpj.text.toString()
        val validCpfCnpj = isValidCpf(cpfCnpj) || isValidCpnj(cpfCnpj)
        if (cpfCnpj.length != 14 && cpfCnpj.length != 18) {
            hasInvalidFields = true
            binding.cpfCnpjTextInputLayout.error = "Campo obrigatorio"
        } else if (!validCpfCnpj) {
            hasInvalidFields = true
            binding.cpfCnpjTextInputLayout.error = "CPF/CNPJ invalido"
        } else {
            binding.cpfCnpjTextInputLayout.error = null
        }

        val clientName = binding.clientNameInput.text.toString()

        val whatsapp = binding.whatsappInput.text.toString()
        if (whatsapp.isEmpty() || whatsapp.length < 13) {
            hasInvalidFields = true
            binding.whatsappTextInputLayout.error = "Campo obrigatorio"
        } else {
            binding.whatsappTextInputLayout.error = null
        }

        val invoiceDate = binding.invoiceDateInput.text.toString()
        if (invoiceDate.isEmpty() || invoiceDate.length != 10 || inValidDateFormat(invoiceDate)) {
            hasInvalidFields = true
            binding.invoiceDateTextInputLayout.error = if (invoiceDate.isEmpty()) {
                "Campo obrigatorio"
            } else {
                "Data invalida"
            }
        } else {
            binding.invoiceDateTextInputLayout.error = null
        }

        val vehicleValue = binding.vehicleValueInput.text.toString()
        if (vehicleValue.isEmpty()) {
            hasInvalidFields = true
            binding.vehicleValueTextInputLayout.error = "Campo obrigatorio"
        } else {
            binding.vehicleValueTextInputLayout.error = null
        }

        val vehicleTypeId = selectedVehicle?.id
        if (vehicleTypeId == null) {
            hasInvalidFields = true
            binding.vehicleTypeDropdownLayout.error = "Campo obrigatorio"
        } else {
            binding.vehicleTypeDropdownLayout.error = null
        }

        if (hasInvalidFields) {
            binding.registrationOrderDataNextBtnError.text =
                getString(R.string.registration_order_error_summary)
            binding.registrationOrderDataNextBtnError.visibility = View.VISIBLE
            return
        }

        canNavigate = true
        registrationViewModel.onOrderNext(
            cpfCnpj,
            clientName,
            whatsapp,
            invoiceDate,
            vehicleValue,
            vehicleTypeId!!,
            binding.disposalVehicleCheckbox.isChecked,
            binding.specialPlateCheckBox.isChecked,
            selectedSalesmanId,
        )
    }

    private fun configureDropdownInputs() {
        configureDropdownInput(binding.vehicleTypeAutoComplete)
        configureDropdownInput(binding.salesmanAutoComplete)

        binding.vehicleTypeDropdownLayout.setEndIconOnClickListener {
            showDropdownWithoutKeyboard(binding.vehicleTypeAutoComplete)
        }

        binding.salesmanTextInputLayout.setEndIconOnClickListener {
            showDropdownWithoutKeyboard(binding.salesmanAutoComplete)
        }
    }

    private fun configureDropdownInput(view: AutoCompleteTextView) {
        view.apply {
            inputType = InputType.TYPE_NULL
            keyListener = null
            isCursorVisible = false
            isFocusable = false
            isFocusableInTouchMode = false
            showSoftInputOnFocus = false
            setOnClickListener { showDropdownWithoutKeyboard(this) }
            setOnTouchListener { _, _ ->
                showDropdownWithoutKeyboard(this)
                true
            }
        }
    }

    private fun showDropdownWithoutKeyboard(view: AutoCompleteTextView) {
        clearSubmitError()
        hideKeyboard(view)
        view.clearFocus()
        view.dismissDropDown()
        view.post { view.showDropDown() }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun inValidDateFormat(date: String): Boolean {
        return try {
            val day = date.substring(0, 2).toInt()
            val month = date.substring(3, 5).toInt()
            val year = date.substring(6, 10).toInt()
            day == 0 || day > 31 || month == 0 || month > 12 || year < 2025
        } catch (_: Exception) {
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeObservers()
    }

    private fun setupObservers() {
        registrationViewModel.orderInitialState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Success<RegistrationOrderInitialState> -> {
                    status.data?.let {
                        binding.loadingView.stopShimmer()
                        binding.loadingView.visibility = View.GONE
                        binding.errorView.visibility = View.GONE
                        binding.contentView.visibility = View.VISIBLE

                        it.orderData?.let { data ->
                            binding.clientNameInput.setText(data.name)

                            if (binding.cpfCnpj.text?.isEmpty() == true) {
                                binding.cpfCnpj.setText(data.cpfCnpj)
                            }

                            if (binding.whatsappInput.text?.isEmpty() == true) {
                                binding.whatsappInput.setText(data.phone)
                            }

                            if (binding.invoiceDateInput.text?.isEmpty() == true) {
                                val day = data.invoiceDate.substring(8, 10)
                                val month = data.invoiceDate.substring(5, 7)
                                val year = data.invoiceDate.substring(0, 4)
                                binding.invoiceDateInput.setText("$day/$month/$year")
                            }

                            binding.specialPlateCheckBox.isChecked = data.specialPlate
                            binding.disposalVehicleCheckbox.isChecked = data.disposalVehicle
                            selectedVehicle = data.vehicleType
                            binding.vehicleValueInput.setText(data.vehiclePrice)
                            selectedSalesmanId = data.salesmanId
                            selectedSalesman = it.salesmen.find { salesman -> salesman.id == data.salesmanId }
                        }

                        setupVehiclesTypesAdapter(it.vehicleTypes)
                        setupSalesmanAdapter(it.salesmen)
                    }
                }

                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.contentView.visibility = View.GONE
                    binding.loadingView.stopShimmer()
                    binding.loadingView.visibility = View.GONE
                    binding.errorView.visibility = View.VISIBLE
                }

                is UIState.Loading -> {
                    binding.contentView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }

                is UIState.Idle -> Unit
            }
        })

        registrationViewModel.orderDataState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.registrationOrderDataNextBtnError.text = status.message
                    binding.registrationOrderDataNextBtnError.visibility = View.VISIBLE
                    binding.registrationOrderDataNextBtn.isEnabled = true
                    binding.loading.visibility = View.GONE
                }

                is UIState.Loading -> {
                    binding.registrationOrderDataNextBtn.isEnabled = false
                    binding.registrationOrderDataNextBtnError.visibility = View.GONE
                    binding.loading.visibility = View.VISIBLE
                }

                is UIState.Success<RegistrationOrderState> -> {
                    binding.loading.visibility = View.GONE
                    binding.registrationOrderDataNextBtnError.visibility = View.GONE
                    binding.registrationOrderDataNextBtn.isEnabled = true
                    navigateNextScreen()
                }

                is UIState.Idle -> Unit
            }
        })

        registrationViewModel.orderDataClientSearchState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.cpfCnpjTextInputLayout.error = "Cliente nao encontrado."
                    binding.cpfCnpjLoading.visibility = View.GONE
                }

                is UIState.Loading -> {
                    binding.cpfCnpjLoading.visibility = View.VISIBLE
                }

                is UIState.Success<CustomerSearchData> -> {
                    binding.cpfCnpjLoading.visibility = View.GONE
                    binding.cpfCnpjTextInputLayout.error = null
                    binding.clientNameInput.setText(status.data?.name)
                    binding.whatsappInput.setText(status.data?.whatsapp)
                }

                is UIState.Idle -> Unit
            }
        })
    }

    private fun setupVehiclesTypesAdapter(vehicleTypes: List<VehicleType>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            vehicleTypes.map { it.name },
        )

        binding.vehicleTypeAutoComplete.setAdapter(adapter)

        if (selectedVehicle != null) {
            binding.vehicleTypeAutoComplete.setText(selectedVehicle?.name, false)
        } else if (BuildConfig.DEBUG && vehicleTypes.isNotEmpty()) {
            binding.vehicleTypeAutoComplete.setText(vehicleTypes[0].name, false)
            selectedVehicle = vehicleTypes[0]
        }

        binding.vehicleTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedVehicle = vehicleTypes[position]
            binding.vehicleTypeDropdownLayout.error = null
            clearSubmitError()
        }
    }

    private fun setupSalesmanAdapter(salesmen: List<Salesman>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            salesmen.map { it.name },
        )

        binding.salesmanAutoComplete.setAdapter(adapter)

        if (selectedSalesmanId != null) {
            salesmen.find { it.id == selectedSalesmanId }?.let {
                selectedSalesman = it
                binding.salesmanAutoComplete.setText(it.name, false)
            }
        } else if (BuildConfig.DEBUG && salesmen.isNotEmpty()) {
            binding.salesmanAutoComplete.setText(salesmen[0].name, false)
            selectedSalesman = salesmen[0]
            selectedSalesmanId = salesmen[0].id
        }

        binding.salesmanAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedSalesman = salesmen[position]
            selectedSalesmanId = salesmen[position].id
            binding.salesmanTextInputLayout.error = null
            clearSubmitError()
        }
    }

    private fun navigateNextScreen() {
        if (canNavigate) {
            binding.root.findNavController()
                .navigate(R.id.action_orderDataFragment_to_resumeFragment)
            canNavigate = false
        }
    }

    private fun clearSubmitError() {
        binding.registrationOrderDataNextBtnError.visibility = View.GONE
    }

    private fun removeObservers() {
        registrationViewModel.orderDataState.removeObservers(viewLifecycleOwner)
        registrationViewModel.orderInitialState.removeObservers(viewLifecycleOwner)
        registrationViewModel.orderDataClientSearchState.removeObservers(viewLifecycleOwner)
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager)
        }
    }

    private fun restoreLastCpfCnpj() {
        if (!binding.cpfCnpj.text.isNullOrBlank()) return
        val lastCpfCnpj = registrationPreferences.getString(KEY_LAST_ORDER_CPF_CNPJ, null).orEmpty()
        if (lastCpfCnpj.isBlank()) return
        binding.cpfCnpj.setText(lastCpfCnpj)
        binding.cpfCnpj.setSelection(binding.cpfCnpj.text?.length ?: 0)
    }

    private fun saveLastCpfCnpj(value: String) {
        val digitsOnly = Mask.replaceChars(value)
        if (digitsOnly.isBlank()) return
        registrationPreferences.edit().putString(KEY_LAST_ORDER_CPF_CNPJ, digitsOnly).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "registration_preferences"
        private const val KEY_LAST_ORDER_CPF_CNPJ = "last_order_cpf_cnpj"
    }
}
