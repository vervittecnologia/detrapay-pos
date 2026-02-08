package com.detrapay.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val authRepository: AuthRepository) :
    ViewModel() {

    private var firstInitialization = true
    private val _homeState = MutableLiveData<UIState<HomeState>>()
    val homeState: LiveData<UIState<HomeState>> = _homeState

    fun loadScreenContent() {
        _homeState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.getLoggedUser(firstInitialization)
            if (result != null) {
                _homeState.postValue(
                    UIState.Success(
                        HomeState(
                            companyName = result.displayName
                        )
                    )
                )
            } else {
                _homeState.postValue(UIState.Error("Ops! Algo deu errado, tente novamente."))
            }
            firstInitialization = false
        }
    }


    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
        }
    }
}