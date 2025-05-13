package com.detrapay.ui.session_expired_dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionExpiredViewModel @Inject constructor(private val authRepository: AuthRepository) :
    ViewModel() {

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
        }
    }
}