package com.detrapay.ui.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ComposePilotFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(com.detrapay.R.layout.fragment_compose_pilot, container, false)
        val composeView = root.findViewById<ComposeView>(com.detrapay.R.id.compose_pilot_view)
        composeView.setContent {
            PilotScreen(onContinue = {
                // example navigation: go to paymentMethodFragment if exists
                try {
                    findNavController().navigate(com.detrapay.R.id.paymentMethodFragment)
                } catch (e: Exception) {
                    findNavController().popBackStack()
                }
            })
        }
        return root
    }
}

@Composable
private fun PilotScreen(onContinue: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Compose Pilot Screen", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onContinue) {
                Text("Continuar")
            }
        }
    }
}
