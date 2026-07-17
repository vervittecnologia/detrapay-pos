package com.detrapay.ui.login

import android.app.ActivityOptions
import android.content.Intent
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.databinding.ActivityLoginBinding
import com.detrapay.debug.DebugOrderDefaults
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.afterTextChanged
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cnpj = binding.cnpj
        val cnpjTextInputLayout = binding.cnpjTextInputLayout
        val passwordTextInputLayout = binding.passwordTextInputLayout
        val password = binding.password
        val login = binding.login
        val loading = binding.loading

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer
            binding.errorTextView.visibility = View.GONE
            login.isEnabled = loginState.isDataValid
            if (loginState.cnpjError != null) {
                cnpjTextInputLayout.error = getString(loginState.cnpjError)
                login.isEnabled = false
            } else {
                cnpjTextInputLayout.error = null
            }

            if (loginState.passwordError != null) {
                passwordTextInputLayout.error = getString(loginState.passwordError)
                login.isEnabled = false
            } else {
                passwordTextInputLayout.error = null
            }

            if (loginState.passwordError == null && loginState.cnpjError == null ){
                login.isEnabled = true
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            setLoadingState(false)
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                val homeIntent = Intent(this, HomeActivity::class.java)
                this.startActivity(homeIntent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                finish()
            }
        })

        cnpj.addTextChangedListener(Mask.mask("##.###.###/####-##", cnpj))
        restoreLastLoggedCnpj()

        if (BuildConfig.DEBUG) {
            cnpj.setText(DebugOrderDefaults.loginCnpjMasked())
            password.setText(DebugOrderDefaults.loginPassword())
            loginViewModel.loginDataChanged(
                cnpj.text.toString(),
                password.text.toString()
            )
            setLoadingState(true)
            loginViewModel.login(
                cnpj.text.toString(),
                password.text.toString()
            )
        }

        cnpj.afterTextChanged {
            binding.errorTextView.visibility = View.GONE
            loginViewModel.loginDataChanged(
                cnpj.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                binding.errorTextView.visibility = View.GONE
                loginViewModel.loginDataChanged(
                    cnpj.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        setLoadingState(true)
                        loginViewModel.login(
                            cnpj.text.toString(),
                            password.text.toString()
                        )
                    }
                }
                false
            }

            login.setOnClickListener {
                binding.errorTextView.visibility = View.GONE
                setLoadingState(true)
                loginViewModel.login(cnpj.text.toString(), password.text.toString())
            }
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        binding.errorTextView.text = getString(errorString)
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.login.text = if (isLoading) getString(R.string.login_loading) else getString(R.string.action_login)
        if (isLoading) {
            binding.login.isEnabled = false
        } else {
            loginViewModel.loginDataChanged(
                binding.cnpj.text.toString(),
                binding.password.text.toString()
            )
        }
    }

    private fun restoreLastLoggedCnpj() {
        val lastLoggedCnpj = loginViewModel.getLastLoggedCnpj().orEmpty()
        if (lastLoggedCnpj.isBlank()) {
            return
        }

        binding.cnpj.setText(formatCnpj(lastLoggedCnpj))
        binding.cnpj.setSelection(binding.cnpj.text?.length ?: 0)
    }

    private fun formatCnpj(cnpj: String): String {
        val digits = cnpj.filter(Char::isDigit)
        return if (digits.length == 14) {
            "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
        } else {
            cnpj
        }
    }
}
