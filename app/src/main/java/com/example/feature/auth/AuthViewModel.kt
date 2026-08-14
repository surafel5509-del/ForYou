package com.example.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.PasswordValidationState
import com.example.domain.usecase.auth.CheckVerificationStatusUseCase
import com.example.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.domain.usecase.auth.ConfirmVerificationUseCase
import com.example.domain.usecase.auth.LoginUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.auth.RegisterUseCase
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.domain.usecase.auth.RequestVerificationUseCase
import com.example.domain.validator.AuthValidators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingVerification: Boolean = false,
    val isResendingVerification: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val successMessage: String? = null,
    val registeredUserEmail: String? = null,
    val isVerificationSent: Boolean = false,
    val isVerificationSuccess: Boolean = false,
    val isPasswordResetEmailSent: Boolean = false,
    val isPasswordResetSuccess: Boolean = false,
    val isVerifiedUser: Boolean = false,
    val registerPasswordValidation: PasswordValidationState = PasswordValidationState(),
    val resetPasswordValidation: PasswordValidationState = PasswordValidationState()
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val requestVerificationUseCase: RequestVerificationUseCase,
    private val confirmVerificationUseCase: ConfirmVerificationUseCase,
    private val checkVerificationStatusUseCase: CheckVerificationStatusUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form inputs state
    val loginIdentity = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")

    val registerEmail = MutableStateFlow("")
    val registerUsername = MutableStateFlow("")
    val registerName = MutableStateFlow("")
    val registerPassword = MutableStateFlow("")
    val registerPasswordConfirm = MutableStateFlow("")

    val verificationToken = MutableStateFlow("")
    val forgotPasswordEmail = MutableStateFlow("")
    val resetPasswordToken = MutableStateFlow("")
    val resetNewPassword = MutableStateFlow("")
    val resetNewPasswordConfirm = MutableStateFlow("")

    fun onLoginIdentityChanged(value: String) {
        loginIdentity.value = value
        clearFieldError("loginIdentity")
    }

    fun onLoginPasswordChanged(value: String) {
        loginPassword.value = value
        clearFieldError("loginPassword")
    }

    fun onRegisterEmailChanged(value: String) {
        registerEmail.value = value
        clearFieldError("registerEmail")
    }

    fun onRegisterUsernameChanged(value: String) {
        val sanitized = value.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }
        registerUsername.value = sanitized
        clearFieldError("registerUsername")
    }

    fun onRegisterNameChanged(value: String) {
        registerName.value = value
        clearFieldError("registerName")
    }

    fun onRegisterPasswordChanged(value: String) {
        registerPassword.value = value
        _uiState.update {
            it.copy(
                registerPasswordValidation = AuthValidators.evaluatePasswordStrength(
                    password = value,
                    confirm = registerPasswordConfirm.value
                )
            )
        }
        clearFieldError("registerPassword")
    }

    fun onRegisterPasswordConfirmChanged(value: String) {
        registerPasswordConfirm.value = value
        _uiState.update {
            it.copy(
                registerPasswordValidation = AuthValidators.evaluatePasswordStrength(
                    password = registerPassword.value,
                    confirm = value
                )
            )
        }
        clearFieldError("registerPasswordConfirm")
    }

    fun onVerificationTokenChanged(value: String) {
        verificationToken.value = value
        clearFieldError("verificationToken")
    }

    fun onForgotPasswordEmailChanged(value: String) {
        forgotPasswordEmail.value = value
        clearFieldError("forgotPasswordEmail")
    }

    fun onResetPasswordTokenChanged(value: String) {
        resetPasswordToken.value = value
        clearFieldError("resetPasswordToken")
    }

    fun onResetNewPasswordChanged(value: String) {
        resetNewPassword.value = value
        _uiState.update {
            it.copy(
                resetPasswordValidation = AuthValidators.evaluatePasswordStrength(
                    password = value,
                    confirm = resetNewPasswordConfirm.value
                )
            )
        }
        clearFieldError("resetNewPassword")
    }

    fun onResetNewPasswordConfirmChanged(value: String) {
        resetNewPasswordConfirm.value = value
        _uiState.update {
            it.copy(
                resetPasswordValidation = AuthValidators.evaluatePasswordStrength(
                    password = resetNewPassword.value,
                    confirm = value
                )
            )
        }
        clearFieldError("resetNewPasswordConfirm")
    }

    fun login(onSuccess: () -> Unit) {
        val identity = loginIdentity.value.trim()
        val password = loginPassword.value

        val fieldErrors = mutableMapOf<String, String>()
        if (identity.isBlank()) fieldErrors["loginIdentity"] = "Email or username is required"
        if (password.isBlank()) fieldErrors["loginPassword"] = "Password is required"

        if (fieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = fieldErrors, error = "Please complete all required fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap(), successMessage = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(identity, password)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun register(onSuccess: (email: String) -> Unit) {
        val email = registerEmail.value.trim()
        val username = registerUsername.value.trim().lowercase()
        val name = registerName.value.trim()
        val password = registerPassword.value
        val passwordConfirm = registerPasswordConfirm.value

        val fieldErrors = mutableMapOf<String, String>()

        val emailVal = AuthValidators.validateEmail(email)
        if (!emailVal.isValid) fieldErrors["registerEmail"] = emailVal.errorMessage ?: "Invalid email"

        val userVal = AuthValidators.validateUsername(username)
        if (!userVal.isValid) fieldErrors["registerUsername"] = userVal.errorMessage ?: "Invalid username"

        val passVal = AuthValidators.validatePassword(password)
        if (!passVal.isValid) fieldErrors["registerPassword"] = passVal.errorMessage ?: "Invalid password"

        val confirmVal = AuthValidators.validatePasswordConfirm(password, passwordConfirm)
        if (!confirmVal.isValid) fieldErrors["registerPasswordConfirm"] = confirmVal.errorMessage ?: "Passwords do not match"

        if (fieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = fieldErrors, error = fieldErrors.values.first()) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap(), successMessage = null) }
        viewModelScope.launch {
            when (val result = registerUseCase(email, username, password, passwordConfirm, name)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            registeredUserEmail = email,
                            successMessage = "Account created! A verification email has been sent."
                        )
                    }
                    onSuccess(email)
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun requestVerification(email: String? = null) {
        val targetEmail = (email ?: registerEmail.value).trim()
        val emailVal = AuthValidators.validateEmail(targetEmail)
        if (!emailVal.isValid) {
            _uiState.update { it.copy(error = emailVal.errorMessage ?: "Please specify a valid email address") }
            return
        }

        _uiState.update { it.copy(isResendingVerification = true, error = null) }
        viewModelScope.launch {
            when (val result = requestVerificationUseCase(targetEmail)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isResendingVerification = false,
                            isVerificationSent = true,
                            successMessage = "Verification link has been sent to $targetEmail"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isResendingVerification = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmVerification(token: String? = null, onSuccess: () -> Unit) {
        val targetToken = (token ?: verificationToken.value).trim()
        val tokenVal = AuthValidators.validateToken(targetToken)
        if (!tokenVal.isValid) {
            _uiState.update { it.copy(error = tokenVal.errorMessage ?: "Please enter verification token", fieldErrors = mapOf("verificationToken" to (tokenVal.errorMessage ?: "Invalid token"))) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = confirmVerificationUseCase(targetToken)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isVerificationSuccess = true,
                            isVerifiedUser = true,
                            successMessage = "Email verified successfully! You can now log in."
                        )
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun checkVerificationStatus(email: String? = null, onStatusChecked: (isVerified: Boolean) -> Unit = {}) {
        val targetEmail = (email ?: registerEmail.value).trim()
        _uiState.update { it.copy(isCheckingVerification = true, error = null) }

        viewModelScope.launch {
            when (val result = checkVerificationStatusUseCase(targetEmail)) {
                is NetworkResult.Success -> {
                    val isVerified = result.data
                    _uiState.update {
                        it.copy(
                            isCheckingVerification = false,
                            isVerifiedUser = isVerified,
                            successMessage = if (isVerified) "Your email has been verified!" else "Email is not verified yet. Please check your inbox or tap resend."
                        )
                    }
                    onStatusChecked(isVerified)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingVerification = false,
                            error = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun requestPasswordReset() {
        val email = forgotPasswordEmail.value.trim()
        val emailVal = AuthValidators.validateEmail(email)
        if (!emailVal.isValid) {
            _uiState.update { it.copy(error = emailVal.errorMessage, fieldErrors = mapOf("forgotPasswordEmail" to (emailVal.errorMessage ?: "Invalid email"))) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = requestPasswordResetUseCase(email)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordResetEmailSent = true,
                            successMessage = "Password reset instructions sent to $email"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun confirmPasswordReset(onSuccess: () -> Unit) {
        val token = resetPasswordToken.value.trim()
        val password = resetNewPassword.value
        val confirm = resetNewPasswordConfirm.value

        val fieldErrors = mutableMapOf<String, String>()

        val tokenVal = AuthValidators.validateToken(token)
        if (!tokenVal.isValid) fieldErrors["resetPasswordToken"] = tokenVal.errorMessage ?: "Token required"

        val passVal = AuthValidators.validatePassword(password)
        if (!passVal.isValid) fieldErrors["resetNewPassword"] = passVal.errorMessage ?: "Password too short"

        val confirmVal = AuthValidators.validatePasswordConfirm(password, confirm)
        if (!confirmVal.isValid) fieldErrors["resetNewPasswordConfirm"] = confirmVal.errorMessage ?: "Passwords do not match"

        if (fieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = fieldErrors, error = fieldErrors.values.first()) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = confirmPasswordResetUseCase(token, password, confirm)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPasswordResetSuccess = true,
                            successMessage = "Password successfully reset! You can now log in."
                        )
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }

    private fun clearFieldError(fieldKey: String) {
        if (_uiState.value.fieldErrors.containsKey(fieldKey)) {
            _uiState.update { it.copy(fieldErrors = it.fieldErrors - fieldKey) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
