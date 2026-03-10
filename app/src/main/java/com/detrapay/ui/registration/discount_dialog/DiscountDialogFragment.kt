package com.detrapay.ui.registration.discount_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.FragmentDiscountDialogBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.Mask.Companion.locale
import java.text.NumberFormat

class DiscountDialogFragment(
    private val listener: OnUpdateListener
) : DialogFragment() {

    private lateinit var binding: FragmentDiscountDialogBinding
    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private var selectedSimulationItem: SimulationItem? = null

    interface OnUpdateListener {
        fun onUpdate(itemPosition: Int?)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiscountDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addDiscountButton.setOnClickListener {
            val discountAmount = Mask.doubleValue(binding.discountInput.text.toString())
            val itemPrice = selectedSimulationItem?.price ?: 0.0

            var canSave = true
            if (selectedSimulationItem == null) {
                binding.spinnerErrorText.visibility = View.VISIBLE
                canSave = false
            } else {
                binding.spinnerErrorText.visibility = View.GONE
            }

            if (discountAmount == 0.0 || discountAmount > itemPrice) {
                canSave = false
                if (discountAmount == 0.0) {
                    binding.discountTextInputLayout.error = "Valor inválido"
                } else if (selectedSimulationItem != null) {
                    val simulationItemPrice: String = NumberFormat.getCurrencyInstance(locale)
                        .format(selectedSimulationItem?.price)
                    binding.discountTextInputLayout.error =
                        "O valor máximo de desconto é de $simulationItemPrice"
                } else {
                    binding.discountTextInputLayout.error =
                        "O valor máximo de desconto não pode ser maior do que o valor do item"
                }
            } else {
                binding.discountTextInputLayout.error = null
            }

            if (canSave) {
                val itemPosition = registrationViewModel.addDiscountToSimulationItem(
                    selectedSimulationItem,
                    discountAmount
                )
                listener.onUpdate(itemPosition)
                this.dismiss()
            }
        }

        binding.cancelBtn.setOnClickListener {
            this.dismiss()
        }

        binding.closeDialog.setOnClickListener {
            this.dismiss()
        }

        val discountableItems = registrationViewModel.simulationItemsWhoSupportDiscount()
        selectedSimulationItem = discountableItems.firstOrNull()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            discountableItems.map { it.name })

        binding.itemSpinner.setAdapter(adapter)

        binding.discountInput.addTextChangedListener(Mask.moneyMask(binding.discountInput, {}))
        focusDiscountInput()

        binding.itemSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedSimulationItem = discountableItems[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


    }

    private fun focusDiscountInput() {
        binding.discountInput.post {
            binding.discountInput.requestFocus()
            binding.discountInput.setSelectAllOnFocus(true)
            binding.discountInput.text?.let { text ->
                if (text.isNotEmpty()) {
                    binding.discountInput.setSelection(0, text.length)
                }
            }
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.discountInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}
