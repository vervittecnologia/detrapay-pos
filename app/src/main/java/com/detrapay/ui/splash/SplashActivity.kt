package com.detrapay.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.authResult.observe(this@SplashActivity, Observer {
            val loginResult = it ?: return@Observer
            handleAuthResult(loginResult)
        })
        viewModel.getLoggedUser()
    }

    private fun handleAuthResult(result: SplashAuthResult) {
        if (result.authenticated) {
            val homeIntent = Intent(
                this,
                HomeActivity::class.java
            )
            this.startActivity(homeIntent)
        } else {
            val loginActivityIntent = Intent(
                this,
                LoginActivity::class.java
            )
            this.startActivity(loginActivityIntent)
        }
        this.finish()
    }
}