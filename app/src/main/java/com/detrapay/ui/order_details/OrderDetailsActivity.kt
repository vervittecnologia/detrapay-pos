package com.detrapay.ui.order_details

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.detrapay.databinding.ActivityOrderDetailsBinding
import com.detrapay.ui.employee_selection.EmployeeSelectionActivity
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
        Log.d("UEHARINHA", orderParam.toString())
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
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: getString(R.string.employees_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
            }
        })
    }

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

        val totalAmount = "%,.2f".format(locale, orderAmount)

        binding.cpfCnpjValue.text = order.customer.cpfCnpj
        binding.clientNameValue.text = order.customer.name
        binding.vehicleValueValue.text = "R$ $vehiclePrice"
        binding.totalAmountValueTxtView.text = "R$ $totalAmount"

        if (order.vehicleType.name.isNullOrEmpty()) {
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


    override fun onItemClick(item: OrderReceivableItem) {
        //        TODO("Not yet implemented")
    }
}