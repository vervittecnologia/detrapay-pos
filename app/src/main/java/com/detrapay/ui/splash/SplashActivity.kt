package com.detrapay.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.detrapay.R
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.theme.DetrapayColors
import com.detrapay.ui.theme.DetrapayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DetrapayTheme {
                val authResult by viewModel.authResult.observeAsState()
                SplashScreen()
                LaunchedEffect(authResult) {
                    authResult?.let(::handleAuthResult)
                }
            }
        }
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

@Composable
private fun SplashScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 520))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetrapayColors.Primary),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.splash_image),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .alpha(progress.value)
                .graphicsLayer {
                    scaleX = 0.88f + (0.12f * progress.value)
                    scaleY = 0.88f + (0.12f * progress.value)
                    translationY = 24f * (1f - progress.value)
                },
        )
    }
}
