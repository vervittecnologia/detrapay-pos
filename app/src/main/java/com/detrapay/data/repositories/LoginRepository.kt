package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.local.User
import com.detrapay.ui.util.Logger
import com.google.gson.Gson
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
                    val companies = result.data.companies.map { Company(it.id, it.name) }
                    val dispatchers = result.data.dispatchers.map { Dispatcher(it.id, it.name) }

                    val user = User(
                        token = result.data.token,
                        id = result.data.user.id,
                        name = result.data.user.name,
                        email = result.data.user.email,
                        username = result.data.user.username,
                        companies = Gson().toJson(companies),
                        dispatchers = Gson().toJson(dispatchers)
                    )

                    userLocalDataSource.insertUser(user)

                    val loggedInUser = LoggedInUser(
                        id = user.id,
                        sessionToken = user.token,
                        email = user.email,
                        displayName = user.name,
                        username = user.username,
                        companies = companies,
                        dispatchers = dispatchers
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