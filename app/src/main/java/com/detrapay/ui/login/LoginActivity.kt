package com.detrapay.ui.login

import android.app.ActivityOptions
import android.content.Intent
import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import com.detrapay.databinding.ActivityLoginBinding
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

            loading.visibility = View.GONE
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

        cnpj.afterTextChanged {
            loginViewModel.loginDataChanged(
                cnpj.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    cnpj.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            cnpj.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(cnpj.text.toString(), password.text.toString())
            }
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}
