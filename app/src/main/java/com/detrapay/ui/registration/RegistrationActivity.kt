package com.detrapay.ui.registration

import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.core.content.ContextCompat
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.databinding.ActivityRegistrationBinding
import com.detrapay.ui.registration.exit_cofirmation_dialog.ExitConfirmationDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegistrationActivity : AppCompatActivity() {

    private val viewModel: RegistrationViewModel by viewModels()
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment

    private var currentScreen: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen / Immersive mode
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)
        
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        initializeViewModelMode()
        setupObservers()
        setupNavigation()
        setupStepperActions()
        setContentView(binding.root)
    }

    private fun setupStepperActions() {
        val stepperBinding = com.detrapay.databinding.LayoutRegistrationStepperBinding.bind(binding.registrationStepper.root)
        
        stepperBinding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        stepperBinding.btnExit.setOnClickListener {
            ExitConfirmationDialog().show(supportFragmentManager, "ExitConfirmationDialog")
        }
    }

    override fun onBackPressed() {
        if (currentScreen == 1) {
            ExitConfirmationDialog().show(supportFragmentManager, "ExitConfirmationDialog")
        } else {
            val previousDestinationId = navController.currentDestination?.id
            val handled = navController.navigateUp()
            val currentDestinationId = navController.currentDestination?.id

            // Avoid navigating back in ViewModel if we just moved from payment detail to payment method
            // (both are part of Step 3)
            if (handled && previousDestinationId == R.id.paymentDetailFragment && currentDestinationId == R.id.paymentMethodFragment) {
                // Stayed in Step 3
            } else {
                viewModel.navigateBack()
            }
        }
    }

    private fun initializeViewModelMode(){
        val orderParam: Order? = intent.getSerializableExtra("order") as Order?
        viewModel.initialize(orderParam)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.registration_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_exit -> {
                ExitConfirmationDialog().show(supportFragmentManager, "ExitConfirmationDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers() {
        viewModel.registrationState.observe(this, Observer { state ->
            this.currentScreen = state.currentScreen
            updateStepper(state.currentScreen)
        })
    }

    private fun updateStepper(currentScreen: Int) {
        val stepperBinding = com.detrapay.databinding.LayoutRegistrationStepperBinding.bind(binding.registrationStepper.root)
        
        val progress = when (currentScreen) {
            1 -> 33
            2 -> 66
            3 -> 100
            else -> 0
        }
        
        stepperBinding.registrationProgress.setProgress(progress, true)
        stepperBinding.tvProgressLabel.text = when(currentScreen) {
            1 -> "DADOS DO CLIENTE"
            2 -> "RESUMO DO PEDIDO"
            3 -> "FORMA DE PAGAMENTO"
            else -> "Passo $currentScreen de 3"
        }
        
        stepperBinding.btnBack.visibility = if (currentScreen == 1) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.registration_nav_host) as NavHostFragment
        navController = navHostFragment.navController
    }
}