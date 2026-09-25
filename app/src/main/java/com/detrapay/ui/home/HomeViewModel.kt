package com.detrapay.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.data.repositories.SessionLifecycleCoordinator
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val salesmanRepository: SalesmanRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionLifecycleCoordinator: SessionLifecycleCoordinator,
) :
    ViewModel() {

    private val _homeState = MutableLiveData<UIState<HomeState>>()
    val homeState: LiveData<UIState<HomeState>> = _homeState

    fun loadScreenContent() {
        _homeState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.getLoggedUser(forceRefresh = false)
            if (result != null) {
                val company = result.companies.firstOrNull()
                val initial = HomeState(
                    companyName = company?.name.orEmpty(),
                    companyDocument = result.cpfCnpj,
                    dispatcherName = result.dispatchers.firstOrNull()?.name.orEmpty(),
                    companyLogoKey = company?.logoKey,
                    salesmen = result.salesmen,
                )
                _homeState.postValue(UIState.Success(initial))
                launch {
                    when (val salesmenResult = salesmanRepository.getSalesmen()) {
                        is Result.Success -> _homeState.postValue(
                            UIState.Success(initial.copy(salesmen = salesmenResult.data)),
                        )
                        is Result.Error -> Unit
                    }
                }
                launch { registrationRepository.loadVehicleTypes() }
            } else {
                _homeState.postValue(UIState.Error("Ops! Algo deu errado, tente novamente."))
            }
        }
    }


    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionLifecycleCoordinator.logout()
        }
    }
}
