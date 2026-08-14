package com.example

import com.example.core.datastore.UserSession
import com.example.core.network.NetworkResult
import com.example.data.repository.AuthRepository
import com.example.domain.model.User
import com.example.domain.usecase.auth.CheckVerificationStatusUseCase
import com.example.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.domain.usecase.auth.ConfirmVerificationUseCase
import com.example.domain.usecase.auth.LoginUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.auth.RegisterUseCase
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.domain.usecase.auth.RequestVerificationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableStateFlow(UserSession())
    override val currentSession: Flow<UserSession> = sessionFlow
    override val currentUser: Flow<User?> = MutableStateFlow(null)

    var loginResult: NetworkResult<User> = NetworkResult.Success(
        User(id = "user1", username = "testuser", email = "test@example.com", isVerified = true)
    )
    var registerResult: NetworkResult<User> = NetworkResult.Success(
        User(id = "user2", username = "newuser", email = "new@example.com", isVerified = false)
    )
    var requestVerificationResult: NetworkResult<Boolean> = NetworkResult.Success(true)
    var confirmVerificationResult: NetworkResult<Boolean> = NetworkResult.Success(true)
    var checkVerificationResult: NetworkResult<Boolean> = NetworkResult.Success(true)
    var requestPasswordResetResult: NetworkResult<Boolean> = NetworkResult.Success(true)
    var confirmPasswordResetResult: NetworkResult<Boolean> = NetworkResult.Success(true)

    var loggedOutCalled = false

    override suspend fun login(identity: String, password: String): NetworkResult<User> = loginResult

    override suspend fun register(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String,
        name: String
    ): NetworkResult<User> = registerResult

    override suspend fun requestVerification(email: String): NetworkResult<Boolean> = requestVerificationResult

    override suspend fun confirmVerification(token: String): NetworkResult<Boolean> = confirmVerificationResult

    override suspend fun checkVerificationStatus(email: String?): NetworkResult<Boolean> = checkVerificationResult

    override suspend fun requestPasswordReset(email: String): NetworkResult<Boolean> = requestPasswordResetResult

    override suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): NetworkResult<Boolean> = confirmPasswordResetResult

    override suspend fun refreshAuth(): NetworkResult<User> = loginResult

    override suspend fun logout() {
        loggedOutCalled = true
        sessionFlow.value = UserSession()
    }
}

class AuthUseCasesTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var requestVerificationUseCase: RequestVerificationUseCase
    private lateinit var confirmVerificationUseCase: ConfirmVerificationUseCase
    private lateinit var checkVerificationStatusUseCase: CheckVerificationStatusUseCase
    private lateinit var requestPasswordResetUseCase: RequestPasswordResetUseCase
    private lateinit var confirmPasswordResetUseCase: ConfirmPasswordResetUseCase

    @Before
    fun setup() {
        fakeRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeRepository)
        registerUseCase = RegisterUseCase(fakeRepository)
        logoutUseCase = LogoutUseCase(fakeRepository)
        requestVerificationUseCase = RequestVerificationUseCase(fakeRepository)
        confirmVerificationUseCase = ConfirmVerificationUseCase(fakeRepository)
        checkVerificationStatusUseCase = CheckVerificationStatusUseCase(fakeRepository)
        requestPasswordResetUseCase = RequestPasswordResetUseCase(fakeRepository)
        confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(fakeRepository)
    }

    @Test
    fun `LoginUseCase returns error when identity or password empty`() = runTest {
        val resultBlankIdentity = loginUseCase("", "password123")
        assertTrue(resultBlankIdentity is NetworkResult.Error)

        val resultBlankPassword = loginUseCase("user@test.com", "")
        assertTrue(resultBlankPassword is NetworkResult.Error)
    }

    @Test
    fun `LoginUseCase returns success with valid credentials`() = runTest {
        val result = loginUseCase("user@test.com", "validPassword123")
        assertTrue(result is NetworkResult.Success)
        assertEquals("testuser", (result as NetworkResult.Success).data.username)
    }

    @Test
    fun `RegisterUseCase validates fields before dispatching`() = runTest {
        val invalidEmailResult = registerUseCase("invalid-email", "validuser", "pass12345", "pass12345", "Name")
        assertTrue(invalidEmailResult is NetworkResult.Error)

        val shortUsernameResult = registerUseCase("user@test.com", "ab", "pass12345", "pass12345", "Name")
        assertTrue(shortUsernameResult is NetworkResult.Error)

        val mismatchPassResult = registerUseCase("user@test.com", "validuser", "pass12345", "different", "Name")
        assertTrue(mismatchPassResult is NetworkResult.Error)

        val validResult = registerUseCase("user@test.com", "validuser", "pass12345", "pass12345", "Name")
        assertTrue(validResult is NetworkResult.Success)
    }

    @Test
    fun `LogoutUseCase triggers logout on repository`() = runTest {
        logoutUseCase()
        assertTrue(fakeRepository.loggedOutCalled)
    }

    @Test
    fun `Verification use cases handle tokens and status verification`() = runTest {
        val requestResult = requestVerificationUseCase("user@test.com")
        assertTrue(requestResult is NetworkResult.Success)

        val confirmResult = confirmVerificationUseCase("TOKEN_ABC")
        assertTrue(confirmResult is NetworkResult.Success)

        val statusResult = checkVerificationStatusUseCase("user@test.com")
        assertTrue(statusResult is NetworkResult.Success)
        assertTrue((statusResult as NetworkResult.Success).data)
    }

    @Test
    fun `Password reset use cases validate inputs and return success`() = runTest {
        val resetReqResult = requestPasswordResetUseCase("user@test.com")
        assertTrue(resetReqResult is NetworkResult.Success)

        val confirmResetResult = confirmPasswordResetUseCase("TOKEN_XYZ", "NewSecurePass123", "NewSecurePass123")
        assertTrue(confirmResetResult is NetworkResult.Success)
    }
}
