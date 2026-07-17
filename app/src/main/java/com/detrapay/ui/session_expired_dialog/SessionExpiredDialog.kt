package com.detrapay.ui.session_expired_dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.detrapay.data.UnauthorizedException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionExpiredDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val endpoint = requireArguments().getString(ARG_ENDPOINT).orEmpty()
            val backendMessage = requireArguments().getString(ARG_BACKEND_MESSAGE).orEmpty()
            val message = buildString {
                append("Sua sessao foi rejeitada pelo backend. O login local foi mantido, mas esta tela nao consegue continuar.")
                if (endpoint.isNotBlank()) {
                    append("\n\nEndpoint: ")
                    append(endpoint)
                }
                if (backendMessage.isNotBlank()) {
                    append("\nDetalhe: ")
                    append(backendMessage)
                }
            }
            val builder = AlertDialog.Builder(it)
            builder
                .setTitle("Sessao invalida ou expirada")
                .setMessage(message)
                .setPositiveButton("OK") { _, _ -> dismiss() }
                .setCancelable(true)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val TAG = "SessionExpiredDialog"
        private const val ARG_ENDPOINT = "arg_endpoint"
        private const val ARG_BACKEND_MESSAGE = "arg_backend_message"

        fun showIfNeeded(fragmentManager: FragmentManager, error: UnauthorizedException) {
            if (fragmentManager.isStateSaved) return
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            SessionExpiredDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ENDPOINT, error.endpoint)
                    putString(ARG_BACKEND_MESSAGE, error.backendMessage)
                }
            }.show(fragmentManager, TAG)
        }
    }
}
