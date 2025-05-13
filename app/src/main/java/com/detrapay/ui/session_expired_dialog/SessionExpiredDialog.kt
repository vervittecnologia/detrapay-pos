package com.detrapay.ui.session_expired_dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.detrapay.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionExpiredDialog : DialogFragment() {

    private val viewModel: SessionExpiredViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Sessão expirada")
            builder.setMessage("Sua sessão expirou, por favor faça login novamente.")
                .setPositiveButton("ok") { _, _ ->
                    viewModel.logout()
                    val intent = Intent(it, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    it.finish()
                }
            builder.setCancelable(false)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
