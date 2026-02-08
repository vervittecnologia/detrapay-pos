package com.detrapay.data.repositories

import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.model.LoggedInUser
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
            val loggedInUser = LoggedInUser(
                user.id,
                user.token,
                user.name,
                user.email,
                user.username
            )
            this.user = loggedInUser
            return loggedInUser
        }
        return null
    }
}