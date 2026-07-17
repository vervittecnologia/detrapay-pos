package com.detrapay.data.repositories

import android.content.Context
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.SessionRefreshResponse
import com.detrapay.data.model.local.User
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
        clearSessionTokens()
        userLocalDataSource.deleteAll()
    }

    fun currentAccessToken(): String? {
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        if (!token.isNullOrBlank()) return token
        return user?.sessionToken?.takeIf { it.isNotBlank() }
    }

    fun currentRefreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun currentTokenType(): String = preferences.getString(KEY_TOKEN_TYPE, DEFAULT_TOKEN_TYPE) ?: DEFAULT_TOKEN_TYPE

    fun tokenExpiresAt(): Long? = preferences
        .getLong(KEY_EXPIRES_AT, NO_EXPIRATION)
        .takeIf { it != NO_EXPIRATION }

    suspend fun saveLoginSession(authResponse: AuthResponse, localUser: User) {
        persistSessionTokens(
            accessToken = authResponse.resolvedAccessToken(),
            refreshToken = authResponse.refreshToken,
            expiresIn = authResponse.expiresIn,
            expiresAt = authResponse.expiresAt,
            tokenType = authResponse.tokenType,
            appMode = authResponse.appMode,
        )
        updateCachedUser(localUser)
    }

    suspend fun updateSessionFromRefresh(response: SessionRefreshResponse): Boolean {
        val accessToken = response.resolvedAccessToken()
        if (accessToken.isBlank()) return false

        persistSessionTokens(
            accessToken = accessToken,
            refreshToken = response.resolvedRefreshToken(),
            expiresIn = response.resolvedExpiresIn(),
            expiresAt = response.resolvedExpiresAt(),
            tokenType = response.resolvedTokenType(),
            appMode = currentAppMode(),
        )
        updateStoredUserToken(accessToken)
        return true
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
                currentAccessToken().takeUnless { it.isNullOrBlank() } ?: user.token,
                user.name,
                user.username,
                user.cpfCnpj,
                user.email,
                companies,
                dispatchers,
                salesmen,
                currentAppMode()
            )
            this.user = loggedInUser
            return loggedInUser
        }
        return null
    }

    private suspend fun updateStoredUserToken(token: String) {
        val storedUsers = userLocalDataSource.getUsers()
        val storedUser = storedUsers.firstOrNull() ?: return
        val updatedUser = storedUser.copy(token = token)
        userLocalDataSource.updateUser(updatedUser)
        updateCachedUser(updatedUser)
    }

    private fun updateCachedUser(localUser: User) {
        val cachedUser = user
        if (cachedUser != null) {
            user = cachedUser.copy(sessionToken = localUser.token)
        }
    }

    private fun persistSessionTokens(
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long?,
        expiresAt: Long?,
        tokenType: String?,
        appMode: String?,
    ) {
        val computedExpiresAt = expiresAt ?: expiresIn?.let { (System.currentTimeMillis() / 1000L) + it }
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_TOKEN_TYPE, tokenType ?: DEFAULT_TOKEN_TYPE)
            .putLong(KEY_EXPIRES_AT, computedExpiresAt ?: NO_EXPIRATION)
            .putString(KEY_APP_MODE, appMode?.takeIf { it.isNotBlank() } ?: LoggedInUser.APP_MODE_COMPLETE)
            .apply()
    }

    private fun currentAppMode(): String {
        return preferences.getString(KEY_APP_MODE, LoggedInUser.APP_MODE_COMPLETE)
            ?.takeIf { it.isNotBlank() }
            ?: LoggedInUser.APP_MODE_COMPLETE
    }

    private fun saveLastLoggedCnpj(cnpj: String) {
        preferences.edit().putString(KEY_LAST_LOGGED_CNPJ, cnpj).apply()
    }

    private fun clearSessionTokens() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_APP_MODE)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "login_preferences"
        private const val KEY_LAST_LOGGED_CNPJ = "last_logged_cnpj"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_APP_MODE = "app_mode"
        private const val DEFAULT_TOKEN_TYPE = "Bearer"
        private const val NO_EXPIRATION = -1L
    }
}
