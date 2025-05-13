package com.detrapay.ui.employee_selection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.model.Employee
import com.detrapay.data.repositories.EmployeeRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeSelectionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val employeeRepository: EmployeeRepository,
) : ViewModel() {

    private val _employeeSelectionState = MutableLiveData<UIState<EmployeeSelectionState>>()
    val employeeSelectionState: LiveData<UIState<EmployeeSelectionState>> = _employeeSelectionState

    fun loadScreenContent() {
        _employeeSelectionState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val loggedInUser = authRepository.getLoggedUser()
            val result = employeeRepository.getEmployees()

            if (result is Result.Success && loggedInUser != null) {
                val successState =
                    EmployeeSelectionState(
                        clientName = loggedInUser.displayName,
                        employees = result.data
                    )
                _employeeSelectionState.postValue(UIState.Success(successState))
            } else {
                val error = result as Result.Error
                _employeeSelectionState.postValue(
                    UIState.Error(
                        message = "Ops! Algo deu errado, tente novamente.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun updatePreferredEmployee(employee: Employee) {
        viewModelScope.launch(Dispatchers.IO) {
            val loggedInUser = authRepository.getLoggedUser()
            if (loggedInUser != null) {
                employeeRepository.updatePreferredCompany(employee, loggedInUser)
            }
        }
    }
}