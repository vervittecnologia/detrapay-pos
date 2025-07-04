package com.detrapay.ui.registration.order_data

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.CustomerSearchData
import com.detrapay.data.model.VehicleType
import com.detrapay.databinding.FragmentRegistrationOrderDataBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.util.isValidCpf
import com.detrapay.ui.util.isValidCpnj
import java.util.Date

class RegistrationOrderDataFragment : Fragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private var canNavigate = false
    private var selectedVehicle: VehicleType? = null
    private var selectedVehicleType: Int? = null
    private lateinit var binding: FragmentRegistrationOrderDataBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationOrderDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        registrationViewModel.loadOrderScreenContent()

        binding.reloadRegistration.setOnClickListener {
            registrationViewModel.loadOrderScreenContent()
        }

        val dateMaskWatcher = Mask.mask(
            "##/##/####", binding.invoiceDateInput
        )

        binding.invoiceDateInput.addTextChangedListener(dateMaskWatcher)

        binding.whatsappInput.addTextChangedListener(
            Mask.mask(
                "(##)#####-####", binding.whatsappInput
            )
        )

        binding.vehicleValueInput.addTextChangedListener(
            Mask.moneyMask(binding.vehicleValueInput, {})
        )

        binding.cpfCnpj.addTextChangedListener(Mask.cpfCnpjMask(binding.cpfCnpj))

        binding.cpfCnpjTextInputLayout.setEndIconOnClickListener {
            val cpfCnpj = Mask.replaceChars(binding.cpfCnpj.text.toString())
            registrationViewModel.searchClient(cpfCnpj)
        }

        binding.invoiceDateTextInputLayout.setEndIconOnClickListener {
            binding.invoiceDateInput.removeTextChangedListener(dateMaskWatcher)
            val now = Date()
            val dialog = DatePickerDialog(requireContext(), { view, year, month, dayOfMonth ->
                val stringDay = if (dayOfMonth < 10) {
                    "0$dayOfMonth"
                } else {
                    dayOfMonth.toString()
                }

                val stringMonth = if (month < 10) {
                    "0$month"
                } else {
                    month.toString()
                }
                binding.invoiceDateInput.setText("$stringDay/$stringMonth/$year")
                binding.invoiceDateInput.addTextChangedListener(dateMaskWatcher)
            }, now.year + 1900, now.month, now.date)
            dialog.setOnCancelListener({
                binding.invoiceDateInput.addTextChangedListener(dateMaskWatcher)
            })
            dialog.show()
        }

        binding.registrationOrderDataNextBtn.setOnClickListener {
            var hasInvalidFields = false
            val cpfCnpj = binding.cpfCnpj.text.toString()
            val validCpfCnpj = isValidCpf(cpfCnpj) || isValidCpnj(cpfCnpj)
            if (cpfCnpj.length != 14 && cpfCnpj.length != 18) {
                hasInvalidFields = true
                binding.cpfCnpjTextInputLayout.error = "Campo obrigatório"
            } else if (!validCpfCnpj) {
                hasInvalidFields = true
                binding.cpfCnpjTextInputLayout.error = "Cpf/Cnpj inválido"
            } else {
                binding.cpfCnpjTextInputLayout.error = null
            }

            val clientName = binding.clientNameInput.text.toString()
//            if (clientName.isEmpty()) {
//                hasInvalidFields = true
//                binding.clientNameTextInputLayout.error = "Campo obrigatório"
//            } else {
//                binding.clientNameTextInputLayout.error = null
//            }

            val whatsapp = binding.whatsappInput.text.toString()
            if (whatsapp.isEmpty() || whatsapp.length < 13) {
                hasInvalidFields = true
                binding.whatsappTextInputLayout.error = "Campo obrigatório"
            } else {
                binding.whatsappTextInputLayout.error = null
            }

            val invoiceDate = binding.invoiceDateInput.text.toString()
            if (invoiceDate.isEmpty() || invoiceDate.length != 10 || inValidDateFormat(invoiceDate)) {
                hasInvalidFields = true
                if (invoiceDate.isEmpty()) {
                    binding.invoiceDateTextInputLayout.error = "Campo obrigatório"
                } else if (inValidDateFormat(invoiceDate)) {
                    binding.invoiceDateTextInputLayout.error = "Data inválida"
                } else {
                    binding.invoiceDateTextInputLayout.error = "Campo obrigatório"
                }
            } else {
                binding.invoiceDateTextInputLayout.error = null
            }

            val vehicleValue = binding.vehicleValueInput.text.toString()
            if (vehicleValue.isEmpty()) {
                hasInvalidFields = true
                binding.vehicleValueTextInputLayout.error = "Campo obrigatório"
            } else {
                binding.vehicleValueTextInputLayout.error = null
            }

            val vehicleTypeId = selectedVehicle?.id
            if (selectedVehicle?.id == null) {
                hasInvalidFields = true
                binding.vehicleTypeTextInputLayout.error = "Campo obrigatório"
            } else {
                binding.vehicleTypeTextInputLayout.error = null
            }

            if (!hasInvalidFields) {
                canNavigate = true
                registrationViewModel.onOrderNext(
                    cpfCnpj,
                    clientName,
                    whatsapp,
                    invoiceDate,
                    vehicleValue,
                    vehicleTypeId!!,
                    binding.disposalVehicleCheckbox.isChecked,
                    binding.specialPlateCheckBox.isChecked
                )
            }
        }
    }

    private fun inValidDateFormat(date: String): Boolean {
        try {
            val day = date.substring(0, 2).toInt()
            val month = date.substring(3, 5).toInt()
            val year = date.substring(6, 10).toInt()
            return day == 0 || day > 31 || month == 0 || month > 12 || year < 2025
        } catch (e: Exception) {
            return true
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
                        Log.d("UEHARA", "Recebeu dados - orderInitialState")
                        binding.loadingView.stopShimmer()
                        binding.loadingView.visibility = View.GONE
                        binding.errorView.visibility = View.GONE
                        binding.contentView.visibility = View.VISIBLE
                        setupVehiclesTypesAdapter(it.vehicleTypes)

                        it.orderData?.let { data ->
                            Log.d("UEHARA", data.toString())
                            Log.d("UEHARA", data.phone)
                            Log.d("UEHARA", data.invoiceDate)

                            binding.clientNameInput.setText(data.name)

                            if (binding.cpfCnpj.text?.isEmpty() == true){
                                binding.cpfCnpj.setText(data.cpfCnpj)
                            }

                            if (binding.whatsappInput.text?.isEmpty() == true){
                                binding.whatsappInput.setText(data.phone)
                            }

                            if (binding.invoiceDateInput.text?.isEmpty() == true){
                                val day = data.invoiceDate.substring(8, 10)
                                val month = data.invoiceDate.substring(5, 7)
                                val year = data.invoiceDate.substring(0, 4)
                                val invoiceDate = "$day/$month/$year"
                                binding.invoiceDateInput.setText(invoiceDate)
                            }

                            binding.specialPlateCheckBox.isChecked = data.specialPlate
                            binding.disposalVehicleCheckbox.isChecked = data.disposalVehicle
                            selectedVehicle = data.vehicleType
                            selectedVehicleType = data.vehicleType.id
                            binding.vehicleValueInput.setText(data.vehiclePrice)
                        }
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
            }
        })

        registrationViewModel.orderDataState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.registrationOrderDataNextBtnError.setText(status.message)
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
            }
        })

        registrationViewModel.orderDataClientSearchState.observe(
            viewLifecycleOwner, Observer { status ->
                when (status) {
                    is UIState.Error -> {
                        validateErrorType(status.exception)
                        binding.cpfCnpj.error = "Usuário não encontrado!"
                        binding.cpfCnpjLoading.visibility = View.GONE
                    }

                    is UIState.Loading -> {
                        binding.cpfCnpjLoading.visibility = View.VISIBLE
                    }

                    is UIState.Success<CustomerSearchData> -> {
                        binding.cpfCnpjLoading.visibility = View.GONE
                        binding.clientNameInput.setText(status.data?.name)
                        binding.whatsappInput.setText(status.data?.whatsapp)
                    }
                }
            })
    }

    private fun setupVehiclesTypesAdapter(vehicleTypes: List<VehicleType>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            vehicleTypes.map { it.name })

        binding.vehicleTypeSpinner.setAdapter(adapter)

        if (selectedVehicleType != null) {
            binding.vehicleTypeSpinner.setSelection(selectedVehicleType!!)
        }

        binding.vehicleTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedVehicle = vehicleTypes[position]
                    selectedVehicleType = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun navigateNextScreen() {
        if (canNavigate) {
            binding.root.findNavController()
                .navigate(R.id.action_orderDataFragment_to_resumeFragment)
            canNavigate = false
        }
    }

    private fun removeObservers() {
        registrationViewModel.orderDataState.removeObservers(viewLifecycleOwner)
        registrationViewModel.orderInitialState.removeObservers(viewLifecycleOwner)
        registrationViewModel.orderDataClientSearchState.removeObservers(viewLifecycleOwner)
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) SessionExpiredDialog().show(
            requireActivity().supportFragmentManager,
            "SessionExpiredDialog"
        )
    }
}