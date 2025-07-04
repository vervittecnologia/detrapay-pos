package com.detrapay.ui.registration.payment_method

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.RegistrationPaymentMethodListItemBinding
import com.detrapay.ui.util.Mask
import java.util.Locale

interface OnItemClickListener {
    fun onAdd(item: SimulationPayment)
    fun onDelete(item: SimulationPayment)
    fun onItemUpdated(newItem: SimulationPayment)
}

class RegistrationPaymentMethodRecyclerViewAdapter(
    private var values: MutableList<SimulationPayment>,
    private val paymentMethods: List<PaymentMethod>,
    private val listener: OnItemClickListener,
) : RecyclerView.Adapter<RegistrationPaymentMethodViewHolder>() {

    override fun getItemId(position: Int): Long {
        return values[position].id
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RegistrationPaymentMethodViewHolder {
        val itemBinding = RegistrationPaymentMethodListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RegistrationPaymentMethodViewHolder(
            parent.context,
            itemBinding,
            paymentMethods
        )
    }

    override fun onBindViewHolder(holder: RegistrationPaymentMethodViewHolder, position: Int) {
        val item: SimulationPayment = values[position]
        holder.bind(
            item = item,
            itemPosition = position,
            onAdd = {
                val paymentMethod = paymentMethods.first()
                val simulationPayment = SimulationPayment(
                    id = values.size.toLong() + 1,
                    paymentMethod = paymentMethod,
                    amountOriginal = "",
                    amountFinal = "",
                )
                values.add(simulationPayment)
                notifyItemChanged(values.size)
                listener.onAdd(simulationPayment)
            },
            onUpdate = { newItem ->
                try {
                    val itemPosition = values.indexOfFirst { it.id == newItem.id }
                    values[itemPosition] = newItem
                    listener.onItemUpdated(newItem)
                } catch (e: Exception) {
                    Log.d("UEHARINHA", e.message ?: "")
                }
            },
            onDelete = {
                try {
                    val itemPosition = values.indexOfFirst { it.id == item.id }
                    values.removeAt(itemPosition)
                    listener.onDelete(item)
                    notifyItemRemoved(itemPosition)
                } catch (e: Exception) {
                    Log.d("UEHARINHA", e.message ?: "")
                }
            }
        )
    }

    override fun getItemCount(): Int = values.size
}


class RegistrationPaymentMethodViewHolder(
    val context: Context,
    val binding: RegistrationPaymentMethodListItemBinding,
    val paymentMethods: List<PaymentMethod>
) : RecyclerView.ViewHolder(
    binding.root
) {
    private val locale = Locale("pt", "BR")
    private val amountInputText: EditText = binding.amountInput
    private val actionButton: ImageView = binding.actionButton
    private val paymentMethodSpinner: Spinner = binding.paymentMethodSpinner
    private val installmentsSelectorLayout: LinearLayout = binding.installmentSelectorLayout
    private val installmentsSelectorSpinner: Spinner = binding.installmentsSelectorSpinner
    private val paymentMethodAdapter = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        paymentMethods.map { it.name }
    )

    fun bind(
        item: SimulationPayment,
        itemPosition: Int,
        onAdd: () -> Unit,
        onUpdate: (item: SimulationPayment) -> Unit,
        onDelete: () -> Unit
    ) {

        amountInputText.setText(item.amountOriginal)
        val textWatcher = Mask.moneyMask(amountInputText, { value ->
            val stringValue = amountInputText.text.toString()
            Log.d("UEHARINHA - adapter", stringValue)
            val newPaymentMethod = paymentMethods[paymentMethodSpinner.selectedItemPosition]
            val newItem = item.copy(amountOriginal = stringValue, amountFinal = stringValue, paymentMethod = newPaymentMethod)
            onUpdate(newItem)
            updateInstallmentView(newItem)
        })
        amountInputText.addTextChangedListener(textWatcher)

        val buttonImage =
            if (itemPosition == 0) R.drawable.ic_add
            else R.drawable.ic_delete

        actionButton.setImageResource(buttonImage)
        actionButton.setOnClickListener {
            if (itemPosition == 0) {
                onAdd()
            } else {
                amountInputText.removeTextChangedListener(textWatcher)
                onDelete()
            }
        }

        paymentMethodSpinner.setAdapter(paymentMethodAdapter)
        paymentMethodSpinner.setSelection(paymentMethods.indexOf(item.paymentMethod))

        paymentMethodSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?,
                    position: Int, id: Long
                ) {
                    val newPaymentMethod = paymentMethods[position]
                    val newItem = item.copy(
                        paymentMethod = newPaymentMethod,
                        amountOriginal = amountInputText.text.toString(),
                        amountFinal = amountInputText.text.toString()
                    )
                    onUpdate(newItem)
                    updateInstallmentView(newItem)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        updateInstallmentView(item)

        installmentsSelectorSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?,
                    position: Int,
                    id: Long
                ) {
                    try {
                        val allowedInstallments: List<Int> =
                            (1..item.paymentMethod.maxInstallments).toList()
                        val newInstallment = allowedInstallments[position]

                        val paymentAmountValue = amountInputText.text.toString()
                            .replace("R$", "")
                            .replace(" ", "")
                            .replace(".", "")
                            .replace(",", ".")
                            .replace("\\s".toRegex(), "").toDouble()
                        val interestRate = item.paymentMethod.interestRate ?: 0.0
                        val amountFinal = (paymentAmountValue + (paymentAmountValue * interestRate))
                        val amountFinalValue = "%,.2f".format(locale, amountFinal)

                        onUpdate(
                            item.copy(
                                installment = newInstallment,
                                amountOriginal = amountInputText.text.toString(),
                                amountFinal = amountFinalValue
                            ),
                        )
                    } catch (_: Exception) {
                        // TODO LOG ERROR
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun updateInstallmentView(item: SimulationPayment) {
        if (item.paymentMethod.maxInstallments > 0) {
            installmentsSelectorLayout.visibility = View.VISIBLE
            val allowedInstallments: List<Int> = (1..item.paymentMethod.maxInstallments).toList()
            val installmentsAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                allowedInstallments.map {
                    installmentsDescription(
                        it,
                        item.paymentMethod.interestRate,
                        amountInputText.text.toString()
                    )
                }
            )
            installmentsSelectorSpinner.setAdapter(installmentsAdapter)
        } else {
            installmentsSelectorLayout.visibility = View.GONE
        }
    }

    private fun installmentsDescription(
        installment: Int,
        interestRate: Double?,
        paymentAmount: String
    ): String {
        return try {
            val paymentAmountValue = paymentAmount.replace("R$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".")
                .replace("\\s".toRegex(), "").toDouble()

            if (interestRate != null && interestRate > 0.0) {
                val paymentAmountValueWithInterestRate = paymentAmountValue + (paymentAmountValue * interestRate)
                val paymentAmountValueWithInterestRateFormattedValue = "%,.2f".format(locale, paymentAmountValueWithInterestRate)

                val installmentAmount = paymentAmountValueWithInterestRate / installment
                val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)
                "Em ${installment}x de R$${installmentFormattedValue} (R\$${paymentAmountValueWithInterestRateFormattedValue})"
            } else {
                val installmentAmount = paymentAmountValue / installment
                val installmentFormattedValue = "%,.2f".format(locale, installmentAmount)
                "Em ${installment}x de R$${installmentFormattedValue} sem juros"
            }
        } catch (e: Exception) {
            "Em ${installment}x"
        }

    }
}
