package com.example

import com.example.domain.usecase.auth.CheckVerificationStatusUseCase
import com.example.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.domain.usecase.auth.ConfirmVerificationUseCase
import com.example.domain.usecase.auth.LoginUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.auth.RegisterUseCase
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.domain.usecase.auth.RequestVerificationUseCase
import com.example.feature.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(
            loginUseCase = LoginUseCase(fakeRepository),
            registerUseCase = RegisterUseCase(fakeRepository),
            logoutUseCase = LogoutUseCase(fakeRepository),
            requestVerificationUseCase = RequestVerificationUseCase(fakeRepository),
            confirmVerificationUseCase = ConfirmVerificationUseCase(fakeRepository),
            checkVerificationStatusUseCase = CheckVerificationStatusUseCase(fakeRepository),
            requestPasswordResetUseCase = RequestPasswordResetUseCase(fakeRepository),
            confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields sets field errors and does not succeed`() = runTest {
        var successCalled = false
        viewModel.login { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertTrue(state.fieldErrors.containsKey("loginIdentity"))
        assertTrue(state.fieldErrors.containsKey("loginPassword"))
    }

    @Test
    fun `login with valid inputs executes successfully`() = runTest {
        var successCalled = false
        viewModel.onLoginIdentityChanged("validuser@example.com")
        viewModel.onLoginPasswordChanged("password123")

        viewModel.login { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `register updates password validation live`() = runTest {
        viewModel.onRegisterPasswordChanged("Short1")
        assertFalse(viewModel.uiState.value.registerPasswordValidation.hasMinLength)

        viewModel.onRegisterPasswordChanged("LongPassword123")
        assertTrue(viewModel.uiState.value.registerPasswordValidation.hasMinLength)

        viewModel.onRegisterPasswordConfirmChanged("LongPassword123")
        assertTrue(viewModel.uiState.value.registerPasswordValidation.matchesConfirm)
    }

    @Test
    fun `register with valid info calls onSuccess with registered email`() = runTest {
        var registeredEmail: String? = null
        viewModel.onRegisterEmailChanged("newuser@example.com")
        viewModel.onRegisterUsernameChanged("newuser")
        viewModel.onRegisterNameChanged("New User")
        viewModel.onRegisterPasswordChanged("ValidPass123")
        viewModel.onRegisterPasswordConfirmChanged("ValidPass123")

        viewModel.register { email -> registeredEmail = email }
        advanceUntilIdle()

        assertEquals("newuser@example.com", registeredEmail)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun `confirmVerification invokes success callback on valid token`() = runTest {
        var successCalled = false
        viewModel.onVerificationTokenChanged("VALID_TOKEN")
        viewModel.confirmVerification { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.uiState.value.isVerificationSuccess)
    }

    @Test
    fun `checkVerificationStatus updates verification state in uiState`() = runTest {
        var checkedVerified = false
        viewModel.checkVerificationStatus("user@example.com") { isVerified ->
            checkedVerified = isVerified
        }
        advanceUntilIdle()

        assertTrue(checkedVerified)
        assertTrue(viewModel.uiState.value.isVerifiedUser)
    }

    @Test
    fun `requestPasswordReset and confirmPasswordReset flow`() = runTest {
        viewModel.onForgotPasswordEmailChanged("reset@example.com")
        viewModel.requestPasswordReset()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPasswordResetEmailSent)

        var resetSuccess = false
        viewModel.onResetPasswordTokenChanged("TOKEN_RESET_123")
        viewModel.onResetNewPasswordChanged("BrandNewPass123")
        viewModel.onResetNewPasswordConfirmChanged("BrandNewPass123")
        viewModel.confirmPasswordReset { resetSuccess = true }
        advanceUntilIdle()

        assertTrue(resetSuccess)
        assertTrue(viewModel.uiState.value.isPasswordResetSuccess)
    }
}
