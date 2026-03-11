package com.detrapay.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.repositories.LoginRepository
import com.detrapay.data.Result
import com.detrapay.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginRepository: LoginRepository) :
    ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun getLastLoggedCnpj(): String? = loginRepository.getLastLoggedCnpj()

    fun login(cnpj: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cnpjNumbers = cnpj.replace(".","").replace("-", "").replace("/","")
            val result = loginRepository.login(cnpjNumbers, password)
            if (result is Result.Success) {
                _loginResult.postValue(LoginResult(success = result.data))
            } else {
                val errorRes = if (isCnpjValid(cnpj) && isPasswordValid(password)) {
                    R.string.login_error_credentials
                } else {
                    R.string.login_failed
                }
                _loginResult.postValue(LoginResult(error = errorRes))
            }
        }
    }

    fun loginDataChanged(cnpj: String, password: String) {
        val cnpjClean = cnpj.replace(".", "").replace("-", "").replace("/", "")
        if (cnpjClean.isNotEmpty() && !isCnpjValid(cnpj)) {
            _loginForm.value = LoginFormState(cnpjError = R.string.invalid_cnpj)
        } else if (password.isNotEmpty() && !isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = isCnpjValid(cnpj) && isPasswordValid(password))
        }
    }

    private fun isCnpjValid(cnpj: String): Boolean {
        return cnpj.isNotEmpty() && cnpj.length == 18
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}
