package com.detrapay.data.repositories

import android.content.Context
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Salesman
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private var userLocalDataSource: UsersDao,
    @ApplicationContext private val context: Context,
) {
    private var user: LoggedInUser? = null
    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    suspend fun logout() {
        user?.cpfCnpj?.takeIf { it.isNotBlank() }?.let { saveLastLoggedCnpj(it) }
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
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(user.salesmen, salesmenType) as List<Salesman>
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val loggedInUser = LoggedInUser(
                user.id,
                user.token,
                user.name,
                user.username,
                user.cpfCnpj,
                user.email,
                companies,
                dispatchers,
                salesmen
            )
            this.user = loggedInUser
            return loggedInUser
        }
        return null
    }

    private fun saveLastLoggedCnpj(cnpj: String) {
        preferences.edit().putString(KEY_LAST_LOGGED_CNPJ, cnpj).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "login_preferences"
        private const val KEY_LAST_LOGGED_CNPJ = "last_logged_cnpj"
    }
}
