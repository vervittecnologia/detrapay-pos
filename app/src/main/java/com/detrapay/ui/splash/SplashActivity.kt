package com.detrapay.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.detrapay.R
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()
    private val splashStartTime: Long = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        animateLogo()

        viewModel.authResult.observe(this@SplashActivity, Observer {
            val loginResult = it ?: return@Observer
            navigateWithMinimumDuration(loginResult)
        })
        viewModel.getLoggedUser()
    }

    private fun animateLogo() {
        val logo = findViewById<ImageView>(R.id.ivSplashLogo)
        logo.scaleX = 0.88f
        logo.scaleY = 0.88f
        logo.translationY = 24f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(520)
            .start()
    }

    private fun navigateWithMinimumDuration(result: SplashAuthResult) {
        val minimumDurationMs = 900L
        val elapsed = SystemClock.elapsedRealtime() - splashStartTime
        val delayMs = (minimumDurationMs - elapsed).coerceAtLeast(0L)
        window.decorView.postDelayed({
            handleAuthResult(result)
        }, delayMs)
    }

    private fun handleAuthResult(result: SplashAuthResult) {
        if (result.authenticated) {
            val intent = Intent(
                this,
                HomeActivity::class.java
            )
            this.startActivity(intent)
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
