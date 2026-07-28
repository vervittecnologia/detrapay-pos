package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.local.User
import com.detrapay.ui.util.Logger
import com.google.gson.Gson
import android.content.Context
import com.detrapay.ui.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private var userLocalDataSource: UsersDao,
    private var detrapayRemoteDataSource: DetrapayRemoteDataSource,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        return when (val result = detrapayRemoteDataSource.login(username, password)) {
            is Result.Success -> {
                try {
                    val companies = result.data.companies.map { companyResponse ->
                        val logoKey = companyResponse.logoUrl
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "company_${companyResponse.id}_logo" }

                        if (logoKey != null) {
                            when (val logoResult = detrapayRemoteDataSource.downloadFile(companyResponse.logoUrl!!)) {
                                is Result.Success -> {
                                    ImageUtils.saveImage(
                                        context = context,
                                        name = logoKey,
                                        bytes = logoResult.data.bytes()
                                    )
                                }

                                is Result.Error -> {
                                    Logger.d("UNABLE TO DOWNLOAD COMPANY LOGO: ${logoResult.exception.message}")
                                }
                            }
                        }

                        Company(
                            id = companyResponse.id,
                            name = companyResponse.name,
                            logoUrl = companyResponse.logoUrl,
                            logoKey = logoKey,
                        )
                    }
                    val dispatchers = result.data.dispatchers.map { Dispatcher(it.id, it.name) }
                    val salesmen = result.data.salesmen.map {
                        Salesman(
                            id = it.id,
                            name = it.name
                        )
                    }

                    val user = User(
                        token = result.data.resolvedAccessToken(),
                        id = result.data.user.id,
                        name = result.data.user.name,
                        email = result.data.user.email,
                        username = result.data.user.username,
                        cpfCnpj = result.data.user.cpf_cnpj,
                        companies = Gson().toJson(companies),
                        dispatchers = Gson().toJson(dispatchers),
                        salesmen = Gson().toJson(salesmen)
                    )
                    userLocalDataSource.insertUser(user)
                    authRepository.saveLoginSession(result.data, user)
                    saveLastLoggedCnpj(user.cpfCnpj)

                    val loggedInUser = LoggedInUser(
                        id = user.id,
                        sessionToken = user.token,
                        email = user.email,
                        displayName = user.name,
                        username = user.username,
                        cpfCnpj = user.cpfCnpj,
                        companies = companies,
                        dispatchers = dispatchers,
                        salesmen = salesmen,
                    )
                    Result.Success(loggedInUser)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO SAVE LOCAL SESSION: ${e.message}")
                    Result.Error(e)
                }
            }

            is Result.Error -> {
                result
            }
        }
    }

    fun getLastLoggedCnpj(): String? {
        return preferences.getString(KEY_LAST_LOGGED_CNPJ, null)
    }

    private fun saveLastLoggedCnpj(cnpj: String) {
        preferences.edit().putString(KEY_LAST_LOGGED_CNPJ, cnpj).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "login_preferences"
        private const val KEY_LAST_LOGGED_CNPJ = "last_logged_cnpj"
    }
}
