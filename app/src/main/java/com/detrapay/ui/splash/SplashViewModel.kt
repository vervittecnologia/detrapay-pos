package com.detrapay.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(private val authRepository: AuthRepository) :
    ViewModel() {

    private val _authResult = MutableLiveData<SplashAuthResult>()
    val authResult: LiveData<SplashAuthResult> = _authResult

    fun getLoggedUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val loggedInUser = authRepository.getLoggedUser()
            delay(2000)
            if (loggedInUser != null) {
                val hasPreferredCompany =
                    loggedInUser.preferredEmployeeId != null && loggedInUser.preferredEmployeeName != null
                _authResult.postValue(
                    SplashAuthResult(
                        authenticated = true,
                        hasPreferredCompany = hasPreferredCompany
                    )
                )
            } else {
                _authResult.postValue(
                    SplashAuthResult(
                        authenticated = false,
                        hasPreferredCompany = false
                    )
                )
            }
        }
    }

}