package com.detrapay.ui.registration

import android.content.Intent
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
        setContentView(binding.root)
    }

    override fun onBackPressed() {
        showExitConfirmation()
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
                showExitConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showExitConfirmation() {
        if (supportFragmentManager.findFragmentByTag("ExitConfirmationDialog") == null) {
            ExitConfirmationDialog().show(supportFragmentManager, "ExitConfirmationDialog")
        }
    }

    fun finishWithCreatedOrder(order: Order) {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_CREATED_ORDER, order),
        )
        finish()
    }

    private fun setupObservers() {
        viewModel.registrationState.observe(this, Observer { state ->
            this.currentScreen = state.currentScreen
            updateStepper(state.currentScreen)
        })
    }

    private fun updateStepper(currentScreen: Int) {
        // No-op: the global stepper has been removed from the registration shell.
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.registration_nav_host) as NavHostFragment
        navController = navHostFragment.navController
    }

    companion object {
        const val EXTRA_CREATED_ORDER = "createdOrder"
    }
}
