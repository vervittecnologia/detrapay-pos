package com.detrapay.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.loadScreenContent()
        addOnBackPressedCallback()
        setupNavigation()
        setupObservers()
        setupToolbar()
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

    private fun setupToolbar() {
        binding.homeToolbar.toolbarLogout.setOnClickListener {
            showLogoutDialog()
        }
//        binding.homeToolbar.toolbarNotifications.setOnClickListener {
//            findNavController().navigate(R.id.notificationsActivity)
//        }
    }

    private fun setupObservers() {
        viewModel.homeState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {}
                is UIState.Success -> {
                    status.data?.let {
                        binding.homeToolbar.companyName.text = it.companyName
                    }
                }

                is UIState.Error -> {}
            }
        })
    }

    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.home_activity_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = binding.bottomAppBar

        bottomNavView.setupWithNavController(navController)
    }

    private fun showLogoutDialog() {
        Logger.d("Loggout button pressed on HomeActivity")
        AlertDialog.Builder(this)
            .setTitle("Deseja sair?")
            .setMessage("Você deseja sair do aplicativo? \nvocê terá que efetuar login novamente.")
            .setPositiveButton(
                android.R.string.ok
            ) { dialog, which ->
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

