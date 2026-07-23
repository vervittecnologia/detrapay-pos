package com.detrapay.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.detrapay.R
import com.detrapay.databinding.ActivityHomeBinding
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var simplifiedModeApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.loadScreenContent()
        addOnBackPressedCallback()
        setupNavigation()
        observeHomeMode()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun addOnBackPressedCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                showLogoutDialog()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                showLogoutDialog()
            }
        }
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.home_activity_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = binding.bottomAppBar

        bottomNavView.setupWithNavController(navController)
    }

    private fun observeHomeMode() {
        viewModel.homeState.observe(this) { state ->
            val homeState = (state as? UIState.Success)?.data ?: return@observe
            when {
                HomeModeRouter.shouldUseSimplifiedSurface(homeState.sellerAppMode) -> {
                    showSimplifiedMode()
                }
                HomeModeRouter.shouldUseDirectCheckoutSurface(homeState.sellerAppMode) -> {
                    showDirectCheckoutMode()
                }
            }
        }
    }

    private fun showSimplifiedMode() {
        if (simplifiedModeApplied) return

        simplifiedModeApplied = true
        binding.bottomAppBar.visibility = android.view.View.GONE
        navController.navigate(R.id.simplifiedReceivableListFragment)
    }

    private fun showDirectCheckoutMode() {
        if (simplifiedModeApplied) return

        simplifiedModeApplied = true
        binding.bottomAppBar.visibility = android.view.View.GONE
        navController.navigate(R.id.directCheckoutFragment)
    }

    private fun showLogoutDialog() {
        Logger.d("Loggout button pressed on HomeActivity")
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                viewModel.logout()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}
