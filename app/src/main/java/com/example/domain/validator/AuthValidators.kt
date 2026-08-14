package com.example.domain.validator

import com.example.domain.model.PasswordValidationState
import com.example.domain.model.ValidationResult

object AuthValidators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9_.]+$")

    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.error("Email address cannot be empty")
        }
        if (!EMAIL_REGEX.matches(trimmed)) {
            return ValidationResult.error("Please enter a valid email address (e.g. name@example.com)")
        }
        return ValidationResult.success()
    }

    fun validateUsername(username: String): ValidationResult {
        val trimmed = username.trim().lowercase()
        if (trimmed.isBlank()) {
            return ValidationResult.error("Username cannot be empty")
        }
        if (trimmed.length < 3) {
            return ValidationResult.error("Username must be at least 3 characters long")
        }
        if (trimmed.length > 30) {
            return ValidationResult.error("Username cannot exceed 30 characters")
        }
        if (!USERNAME_REGEX.matches(trimmed)) {
            return ValidationResult.error("Username can only contain lowercase letters, numbers, underscores and dots")
        }
        if (trimmed.startsWith(".") || trimmed.endsWith(".")) {
            return ValidationResult.error("Username cannot start or end with a dot")
        }
        if (trimmed.contains("..")) {
            return ValidationResult.error("Username cannot contain consecutive dots")
        }
        return ValidationResult.success()
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult.error("Password cannot be empty")
        }
        if (password.length < 8) {
            return ValidationResult.error("Password must be at least 8 characters long")
        }
        return ValidationResult.success()
    }

    fun validatePasswordConfirm(password: String, passwordConfirm: String): ValidationResult {
        val passwordResult = validatePassword(password)
        if (!passwordResult.isValid) {
            return passwordResult
        }
        if (passwordConfirm.isBlank()) {
            return ValidationResult.error("Please confirm your password")
        }
        if (password != passwordConfirm) {
            return ValidationResult.error("Passwords do not match")
        }
        return ValidationResult.success()
    }

    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.length > 50) {
            return ValidationResult.error("Name cannot exceed 50 characters")
        }
        return ValidationResult.success()
    }

    fun validateToken(token: String): ValidationResult {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.error("Token / verification code cannot be empty")
        }
        if (trimmed.length < 4) {
            return ValidationResult.error("Please enter a valid token")
        }
        return ValidationResult.success()
    }

    fun evaluatePasswordStrength(password: String, confirm: String = ""): PasswordValidationState {
        val hasMinLength = password.length >= 8
        val hasDigitOrSpecial = password.any { it.isDigit() || !it.isLetterOrDigit() }
        val hasLetter = password.any { it.isLetter() }
        val matchesConfirm = password.isNotEmpty() && password == confirm

        return PasswordValidationState(
            hasMinLength = hasMinLength,
            hasDigitOrSpecial = hasDigitOrSpecial,
            hasUppercaseOrLowercase = hasLetter,
            matchesConfirm = matchesConfirm
        )
    }
}
