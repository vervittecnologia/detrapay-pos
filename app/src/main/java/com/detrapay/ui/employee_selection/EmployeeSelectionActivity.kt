package com.detrapay.ui.employee_selection

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Employee
import com.detrapay.databinding.ActivityEmployeeSelectionBinding
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmployeeSelectionActivity : AppCompatActivity(), EmployeeRecyclerViewAdapter.OnItemClickListener {

    private lateinit var binding: ActivityEmployeeSelectionBinding
    private val viewModel: EmployeeSelectionViewModel by viewModels()
    private lateinit var employeeRecyclerViewAdapter: EmployeeRecyclerViewAdapter
    private var fromHome: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeSelectionBinding.inflate(layoutInflater)
        supportActionBar?.hide()
        setContentView(binding.root)
        initRecyclerView()
        observeViewModel()
        setupErrorBtn()
        addOnBackPressedCallback()

        viewModel.loadScreenContent()
    }

    private fun addOnBackPressedCallback() {
        val extras = intent.extras
        this.fromHome = extras?.getBoolean("FROM_HOME") ?: false

        onBackPressedDispatcher.addCallback(this) {
            if (fromHome) openHome()
        }
    }

    private fun initRecyclerView() {
        employeeRecyclerViewAdapter = EmployeeRecyclerViewAdapter(emptyList(), this)
        val recyclerView: RecyclerView = binding.rvEmployees
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = employeeRecyclerViewAdapter
    }

    override fun onItemClick(item: Employee) {
        viewModel.updatePreferredEmployee(item)
        openHome()
    }

    private fun openHome() {
        val homeActivityIntent = Intent(
            this,
            HomeActivity::class.java
        )
        this.startActivity(homeActivityIntent)
        finish()
    }

    private fun observeViewModel() {
        viewModel.employeeSelectionState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    binding.contentView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }

                is UIState.Success -> {
                    status.data?.let {
                        employeeRecyclerViewAdapter.swapData(it.employees)
                        binding.subtitle.text =
                            getString(R.string.employee_selection_subtitle, it.clientName)
                        binding.loadingView.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
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

    private fun setupErrorBtn() {
        binding.reloadEmployees.setOnClickListener {
            viewModel.loadScreenContent()
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) SessionExpiredDialog().show(
            supportFragmentManager,
            "SessionExpiredDialog"
        )
    }
}