package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Employee
import com.detrapay.data.model.local.User
import javax.inject.Inject
import javax.inject.Singleton
import com.detrapay.ui.util.Logger

@Singleton
class EmployeeRepository @Inject constructor(
    private val detrapayRemoteDataSource: DetrapayRemoteDataSource,
    private val userLocalDataSource: UsersDao
) {

    suspend fun getEmployees(): Result<List<Employee>> {
        when (val result = detrapayRemoteDataSource.getEmployees()) {
            is Result.Success -> {
                try {
                    val stores: List<Employee> = result.data.map {
                        Employee(it.id, it.email, it.name, it.username)
                    }
                    return Result.Success(stores)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO GET EMPLOYEES: ${e.message}")
                    return Result.Error(e)
                }
            }

            is Result.Error -> {
                return result
            }

            else -> {
                return Result.Error(Exception())
            }
        }
    }

    suspend fun updatePreferredCompany(employee: Employee, loggedInUser: LoggedInUser) {
        val newLoggedInUser = User(
            id = loggedInUser.id,
            token = loggedInUser.sessionToken,
            name = loggedInUser.displayName,
            email = loggedInUser.email,
            username = loggedInUser.username,
            preferredEmployeeName = employee.name,
            preferredEmployeeId = employee.id,
        )
        userLocalDataSource.updateUser(newLoggedInUser)
    }
}