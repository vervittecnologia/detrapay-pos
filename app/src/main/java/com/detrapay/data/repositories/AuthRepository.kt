package com.detrapay.data.repositories

import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Salesman
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private var userLocalDataSource: UsersDao,
) {
    private var user: LoggedInUser? = null

    suspend fun logout() {
        this.user = null
        userLocalDataSource.deleteAll()
    }

    suspend fun getLoggedUser(forceRefresh: Boolean = false): LoggedInUser? {
        if (this.user != null && !forceRefresh) {
            return this.user!!
        }

        val users = userLocalDataSource.getUsers()
        if (users.isNotEmpty()) {
            val user = users.first()

            val gson = Gson()
            val companyType = object : TypeToken<List<Company>>() {}.type
            val dispatcherType = object : TypeToken<List<Dispatcher>>() {}.type
            val salesmenType = object : TypeToken<List<Salesman>>() {}.type

            val companies: List<Company> = if (user.companies != null) {
                gson.fromJson(user.companies, companyType)
            } else {
                emptyList()
            }

            val dispatchers: List<Dispatcher> = if (user.dispatchers != null) {
                gson.fromJson(user.dispatchers, dispatcherType)
            } else {
                emptyList()
            }

            val salesmen: List<Salesman> = if (user.salesmen != null) {
                gson.fromJson(user.salesmen, salesmenType)
            } else {
                emptyList()
            }

            val loggedInUser = LoggedInUser(
                user.id,
                user.token,
                user.name,
                user.email,
                user.username,
                companies,
                dispatchers,
                salesmen
            )
            this.user = loggedInUser
            return loggedInUser
        }
        return null
    }
}