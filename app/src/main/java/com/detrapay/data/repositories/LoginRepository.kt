package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.local.User
import com.detrapay.ui.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private var userLocalDataSource: UsersDao,
    private var detrapayRemoteDataSource: DetrapayRemoteDataSource,
) {
    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        when (val result = detrapayRemoteDataSource.login(username, password)) {
            is Result.Success -> {
                try {
                    val user = User(
                        token = result.data.token,
                        id = result.data.user.id,
                        name = result.data.user.name,
                        email = result.data.user.email,
                        username = result.data.user.username
                    )

                    userLocalDataSource.insertUser(user)

                    val loggedInUser = LoggedInUser(
                        id = user.id,
                        sessionToken = user.token,
                        email = user.email,
                        displayName = user.name,
                        username = user.username
                    )
                    return Result.Success(loggedInUser)
                } catch (e: Exception) {
                    Logger.d("UNABLE TO SAVE LOCAL SESSION: ${e.message}")
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
}