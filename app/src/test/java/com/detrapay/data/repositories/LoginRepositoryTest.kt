package com.detrapay.data.repositories

import android.content.Context
import android.content.SharedPreferences
import com.detrapay.data.Result
import com.detrapay.data.datasources.local.UsersDao
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.remote.AuthResponse
import com.detrapay.data.model.remote.DispatcherResponse
import com.detrapay.data.model.remote.LoginCompanyResponse
import com.detrapay.data.model.remote.RoleResponse
import com.detrapay.data.model.remote.UserResponse
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginRepositoryTest {

    private val usersDao = mockk<UsersDao>()
    private val remoteDataSource = mockk<DetrapayRemoteDataSource>()
    private val authRepository = mockk<AuthRepository>()
    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()

    private lateinit var repository: LoginRepository

    @Before
    fun setUp() {
        every { context.getSharedPreferences("login_preferences", Context.MODE_PRIVATE) } returns preferences
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = LoginRepository(
            userLocalDataSource = usersDao,
            detrapayRemoteDataSource = remoteDataSource,
            authRepository = authRepository,
            context = context,
        )
    }

    @Test
    fun `login ignores legacy simplified profile mode when dealership uses complete mode`() = runTest {
        coEvery { remoteDataSource.login("04685620000162", "crasa04685620") } returns Result.Success(
            authResponse(appMode = "simplified")
        )
        coEvery { usersDao.insertUser(any()) } returns 1L
        coEvery { authRepository.saveLoginSession(any(), any()) } just Runs

        val result = repository.login("04685620000162", "crasa04685620")

        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("complete", user.appMode)
        assertTrue(!user.isSimplifiedMode)
    }

    @Test
    fun `login uses simplified mode from company configuration`() = runTest {
        coEvery { remoteDataSource.login("04685620000162", "crasa04685620") } returns Result.Success(
            authResponse(appMode = "complete", companySellerAppMode = "simplified")
        )
        coEvery { usersDao.insertUser(any()) } returns 1L
        coEvery { authRepository.saveLoginSession(any(), any()) } just Runs

        val result = repository.login("04685620000162", "crasa04685620")

        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("simplified", user.appMode)
        assertTrue(user.isSimplifiedMode)
    }

    @Test
    fun `login uses direct checkout mode from company configuration`() = runTest {
        coEvery { remoteDataSource.login("04685620000162", "crasa04685620") } returns Result.Success(
            authResponse(appMode = "complete", companySellerAppMode = "direct_checkout")
        )
        coEvery { usersDao.insertUser(any()) } returns 1L
        coEvery { authRepository.saveLoginSession(any(), any()) } just Runs

        val result = repository.login("04685620000162", "crasa04685620")

        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("direct_checkout", user.appMode)
        assertTrue(user.isSimplifiedMode)
    }

    private fun authResponse(appMode: String, companySellerAppMode: String? = null) = AuthResponse(
        token = "legacy-token",
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600,
        expiresAt = 999999,
        tokenType = "Bearer",
        appMode = appMode,
        user = UserResponse(
            id = "user-1",
            documentId = "doc-1",
            username = "terminal",
            name = "Terminal",
            email = "terminal@example.com",
            phoneNumber = null,
            cpf_cnpj = "04685620000162",
            blocked = false,
            role = RoleResponse(
                id = 1,
                name = "pos_terminal",
                type = "pos_terminal",
            ),
        ),
        companies = listOf(
            LoginCompanyResponse(
                id = 37,
                name = "CRASA",
                sellerAppMode = companySellerAppMode,
            ),
        ),
        dispatchers = listOf(
            DispatcherResponse(
                id = 35,
                name = "Despachante",
            ),
        ),
        salesmen = emptyList(),
    )
}
