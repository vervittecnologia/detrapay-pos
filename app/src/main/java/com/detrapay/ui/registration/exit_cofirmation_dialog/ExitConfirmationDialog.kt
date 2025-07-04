package com.detrapay.ui.registration.exit_cofirmation_dialog

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.fragment.app.DialogFragment
import com.detrapay.databinding.FragmentExitRegistrationDialogBinding
class ExitConfirmationDialog : DialogFragment() {

    private lateinit var binding: FragmentExitRegistrationDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExitRegistrationDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myCustomizedString = SpannableStringBuilder()
            .append("Tem certeza que deseja sair sem salvar? ")
            .bold{ append("As informações serão perdidas.") }

        binding.message.text = myCustomizedString

        binding.exitBtn.setOnClickListener {
            activity?.finish()
        }

        binding.cancelBtn.setOnClickListener {
            this.dismiss()
        }

        binding.closeDialog.setOnClickListener {
            this.dismiss()
        }
    }
}
