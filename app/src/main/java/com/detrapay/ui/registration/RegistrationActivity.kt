package com.detrapay.ui.registration

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.detrapay.R
import com.detrapay.databinding.ActivityRegistrationBinding
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationActivity : AppCompatActivity() {

    private val viewModel: RegistrationViewModel by viewModels()
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var progressBar: LinearProgressIndicator

    private var currentScreen: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        progressBar = binding.linearProgressIndicator
        progressBar.progress = 25
        setupObservers()
        setupNavigation()
        setupToolbar()
        setContentView(binding.root)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.registrationToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.registrationToolbar.setNavigationOnClickListener {
            if (currentScreen == 1) {
                finish()
            } else {
                navController.navigateUp() || super.onSupportNavigateUp()
            }
            viewModel.navigateBack()
        }
    }

    private fun setupObservers() {
        viewModel.registrationState.observe(this, Observer { state ->
            updateToolbar(state.currentScreen)
        })
    }

    private fun updateToolbar(currentScreen: Int) {
        this.currentScreen = currentScreen
        progressBar.progress = currentScreen * 25
        val toolbarTitle = when (currentScreen) {
            1 -> {
                "Dados do pedido"
            }

            2 -> {
                "Detalhamento do pagamento"
            }

            else -> {
                "Forma de pagamento"
            }
        }
        binding.registrationToolbar.title = toolbarTitle
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.registration_nav_host) as NavHostFragment
        navController = navHostFragment.navController
    }
}