package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.databinding.ActivityOrderDetailsBinding
import com.detrapay.ui.employee_selection.EmployeeSelectionActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.refund.RefundPaymentDialogFragment
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class OrderDetailsActivity : AppCompatActivity(),
    OrderDetailsPaymentsRecyclerViewAdapter.OnItemClickListener {

    private lateinit var binding: ActivityOrderDetailsBinding
    private lateinit var orderParam: Order
    private val locale: Locale = Locale("pt", "BR")
    private var paymentsAdapter: OrderDetailsPaymentsRecyclerViewAdapter? = null
    private var orderListItemDetailsExpanded = false

    private val viewModel: OrderDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderParam = intent.getSerializableExtra("order") as Order
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        viewModel.loadScreenContent(orderParam.id)
        setupToolbar(orderParam)
        setupObservers()
        setupErrorBtn()
        setContentView(binding.root)
    }

    private fun setupObservers() {
        viewModel.orderState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    binding.contentView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }

                is UIState.Success -> {
                    status.data?.let {
                        binding.errorView.visibility = View.GONE
                        binding.loadingView.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        setupOrderResume(it)
                        setupPayments(it)
                        binding.contentView.visibility = View.VISIBLE

                    }
                }

                is UIState.Error -> {
                    validateErrorType(status.exception)
                    if (status.retryData != null) {
                        setupErrorBtnWithPaymentData(status.retryData)
                    } else {
                        setupErrorBtn()
                    }
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: getString(R.string.employees_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupErrorBtnWithPaymentData(retryData: Any) {
        binding.reloadOrderDetails.setOnClickListener {
            val retryDataModel = retryData as RetryDataModel
            viewModel.payOrder(retryDataModel.receivableItem, retryDataModel.paymentData)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupOrderResume(order: Order) {
        val vehiclePrice = "%,.2f".format(locale, order.vehiclePrice)

        val orderAmount = if (order.currentAmount > 0.0) {
            order.currentAmount
        } else {
            order.items.sumOf {
                if (it.price != null) {
                    it.price - it.discount
                } else {
                    0.0
                }
            }
        }

        val paidReceivables = order.receivables.filter {
            it.status == OrderReceivableItemStatus.PAID
        }

        if (paidReceivables.isEmpty()) {
            binding.editOrderTxtView.visibility = View.VISIBLE
        } else {
            binding.editOrderTxtView.visibility = View.GONE
        }

        binding.editOrderTxtView.setOnClickListener {
            val orderDetailsActivityIntent = Intent(
                this,
                RegistrationActivity::class.java
            )
            orderDetailsActivityIntent.putExtra("order", order)

            this.startActivity(orderDetailsActivityIntent)
            this.finish()
        }

        val totalAmount = "%,.2f".format(locale, orderAmount)

        val cpfCnpjFormatted = formatCpfCnpj(order.customer.cpfCnpj)
        binding.cpfCnpjValue.text = cpfCnpjFormatted
        binding.clientNameValue.text = order.customer.name
        binding.vehicleValueValue.text = "R$ $vehiclePrice"
        binding.totalAmountValueTxtView.text = "R$ $totalAmount"

        if (order.vehicleType.name.isEmpty()) {
            binding.vehicleTypeValue.visibility = View.GONE
            binding.vehicleTypeLabel.visibility = View.GONE
        } else {
            binding.vehicleTypeValue.text = order.vehicleType.name
        }

        binding.rvOrderListItem.layoutManager = LinearLayoutManager(this)
        binding.rvOrderListItem.adapter = OrderDetailsItemsRecyclerViewAdapter(order.items)

        binding.constraintLayout.setOnClickListener {
            if (orderListItemDetailsExpanded) {
                orderListItemDetailsExpanded = false
                binding.rvOrderListItem.visibility = View.GONE
                binding.expandOrderListItems.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.chevron_up
                    )
                )
            } else {
                orderListItemDetailsExpanded = true
                binding.rvOrderListItem.visibility = View.VISIBLE
                binding.expandOrderListItems.setImageDrawable(
                    AppCompatResources.getDrawable(
                        this,
                        R.drawable.chevron_down
                    )
                )
            }
        }

        binding.finishServiceBtn.setOnClickListener {
            val intent = Intent(this, EmployeeSelectionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun formatCpfCnpj(cpfCnpj: String): String {
        if (cpfCnpj.length == 11) {
            val first = cpfCnpj.substring(0, 3)
            val second = cpfCnpj.substring(3, 6)
            val third = cpfCnpj.substring(6, 9)
            val fourth = cpfCnpj.substring(9, 11)
            return "$first.$second.$third-$fourth"
        } else if (cpfCnpj.length == 14) {
            val first = cpfCnpj.substring(0, 2)
            val second = cpfCnpj.substring(2, 5)
            val third = cpfCnpj.substring(5, 8)
            val fourth = cpfCnpj.substring(8, 12)
            val fifth = cpfCnpj.substring(12, 14)
            return "$first.$second.$third/$fourth-$fifth"
        } else {
            return cpfCnpj
        }
    }

    private fun setupToolbar(order: Order) {
        binding.toolbar.title = "Pedido ${order.id}"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupPayments(order: Order) {
        paymentsAdapter = OrderDetailsPaymentsRecyclerViewAdapter(order.receivables, this)
        val recyclerView: RecyclerView = binding.receivablesRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = paymentsAdapter
    }

    private fun setupErrorBtn() {
        binding.reloadOrderDetails.setOnClickListener {
            viewModel.loadScreenContent(orderParam.id)
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) SessionExpiredDialog().show(
            supportFragmentManager,
            "SessionExpiredDialog"
        )
    }

    override fun onItemClick(receivable: OrderReceivableItem) {
        val paymentDialogFragment = PaymentDialogFragment(listener = object : PaymentDialogFragment.PaymentListener {
                override fun onResult(paymentData: PaymentData?) {
                    if (paymentData != null) {
                        viewModel.payOrder(receivable, paymentData)
                        Toast.makeText(this@OrderDetailsActivity, "Pagamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OrderDetailsActivity, "Falha ao realizar pagamento", Toast.LENGTH_SHORT).show()
                    }
                }
            }, orderParam.id, receivable)
        paymentDialogFragment.show(this.supportFragmentManager, "PaymentDialogFragment")
    }

    override fun onRefundClick(receivable: OrderReceivableItem) {
        val refundPaymentDialogFragment = RefundPaymentDialogFragment(listener = object : RefundPaymentDialogFragment.RefundPaymentListener {
            override fun onResult(refundPaymentData: RefundPaymentData?) {
                if (refundPaymentData != null) {
                    viewModel.refundItem(receivable, refundPaymentData)
                    Toast.makeText(this@OrderDetailsActivity, "Estorno de Pagamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OrderDetailsActivity, "Falha ao realizar estorno", Toast.LENGTH_SHORT).show()
                }
            }
        }, receivable)
        refundPaymentDialogFragment.show(this.supportFragmentManager, "RefundPaymentDialogFragment")

    }
}