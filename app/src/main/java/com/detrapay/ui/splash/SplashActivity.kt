package com.detrapay.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.authResult.observe(this, ::handleAuthResult)
        viewModel.getLoggedUser()
    }

    private fun handleAuthResult(result: SplashAuthResult) {
        startActivity(
            Intent(
                this,
                if (result.authenticated) HomeActivity::class.java else LoginActivity::class.java,
            ),
        )
        finish()
    }
}
