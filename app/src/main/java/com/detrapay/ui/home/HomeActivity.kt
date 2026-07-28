package com.detrapay.ui.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.detrapay.R
import com.detrapay.databinding.ActivityHomeBinding
import com.detrapay.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityHomeBinding.inflate(layoutInflater).root)
        viewModel.loadScreenContent()
        addOnBackPressedCallback()
    }

    private fun addOnBackPressedCallback() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { showLogoutDialog() }
        } else {
            onBackPressedDispatcher.addCallback(this) { showLogoutDialog() }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .show()
    }
}
