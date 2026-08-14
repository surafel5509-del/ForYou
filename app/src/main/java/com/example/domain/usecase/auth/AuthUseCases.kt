package com.example.domain.usecase.auth

import com.example.core.network.NetworkResult
import com.example.data.repository.AuthRepository
import com.example.domain.model.User
import com.example.domain.validator.AuthValidators

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(identity: String, password: String): NetworkResult<User> {
        val trimmed = identity.trim()
        if (trimmed.isBlank()) return NetworkResult.Error("Please enter your email or username")
        if (password.isBlank()) return NetworkResult.Error("Please enter your password")
        return authRepository.login(trimmed, password)
    }
}

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String,
        name: String
    ): NetworkResult<User> {
        val emailValidation = AuthValidators.validateEmail(email)
        if (!emailValidation.isValid) {
            return NetworkResult.Error(emailValidation.errorMessage ?: "Invalid email")
        }

        val usernameValidation = AuthValidators.validateUsername(username)
        if (!usernameValidation.isValid) {
            return NetworkResult.Error(usernameValidation.errorMessage ?: "Invalid username")
        }

        val passwordValidation = AuthValidators.validatePassword(password)
        if (!passwordValidation.isValid) {
            return NetworkResult.Error(passwordValidation.errorMessage ?: "Invalid password")
        }

        val confirmValidation = AuthValidators.validatePasswordConfirm(password, passwordConfirm)
        if (!confirmValidation.isValid) {
            return NetworkResult.Error(confirmValidation.errorMessage ?: "Passwords do not match")
        }

        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim().lowercase()
        val trimmedName = name.trim().ifBlank { trimmedUsername }

        return authRepository.register(
            email = trimmedEmail,
            username = trimmedUsername,
            password = password,
            passwordConfirm = passwordConfirm,
            name = trimmedName
        )
    }
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() {
        authRepository.logout()
    }
}

class RequestVerificationUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): NetworkResult<Boolean> {
        val emailValidation = AuthValidators.validateEmail(email)
        if (!emailValidation.isValid) {
            return NetworkResult.Error(emailValidation.errorMessage ?: "Invalid email")
        }
        return authRepository.requestVerification(email.trim())
    }
}

class ConfirmVerificationUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String): NetworkResult<Boolean> {
        val tokenValidation = AuthValidators.validateToken(token)
        if (!tokenValidation.isValid) {
            return NetworkResult.Error(tokenValidation.errorMessage ?: "Invalid token")
        }
        return authRepository.confirmVerification(token.trim())
    }
}

class CheckVerificationStatusUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String? = null): NetworkResult<Boolean> {
        return authRepository.checkVerificationStatus(email?.trim())
    }
}

class RequestPasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): NetworkResult<Boolean> {
        val emailValidation = AuthValidators.validateEmail(email)
        if (!emailValidation.isValid) {
            return NetworkResult.Error(emailValidation.errorMessage ?: "Invalid email")
        }
        return authRepository.requestPasswordReset(email.trim())
    }
}

class ConfirmPasswordResetUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        token: String,
        password: String,
        passwordConfirm: String
    ): NetworkResult<Boolean> {
        val tokenValidation = AuthValidators.validateToken(token)
        if (!tokenValidation.isValid) {
            return NetworkResult.Error(tokenValidation.errorMessage ?: "Invalid token")
        }

        val passwordValidation = AuthValidators.validatePassword(password)
        if (!passwordValidation.isValid) {
            return NetworkResult.Error(passwordValidation.errorMessage ?: "Invalid password")
        }

        val confirmValidation = AuthValidators.validatePasswordConfirm(password, passwordConfirm)
        if (!confirmValidation.isValid) {
            return NetworkResult.Error(confirmValidation.errorMessage ?: "Passwords do not match")
        }

        return authRepository.confirmPasswordReset(token.trim(), password, passwordConfirm)
    }
}
