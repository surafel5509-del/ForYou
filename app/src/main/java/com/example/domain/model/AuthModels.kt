package com.example.domain.model

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ValidationResult(isValid = true, errorMessage = null)
        fun error(message: String) = ValidationResult(isValid = false, errorMessage = message)
    }
}

data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasDigitOrSpecial: Boolean = false,
    val hasUppercaseOrLowercase: Boolean = false,
    val matchesConfirm: Boolean = false
) {
    val isValid: Boolean get() = hasMinLength && matchesConfirm
}

data class UserVerificationStatus(
    val email: String = "",
    val isVerified: Boolean = false,
    val message: String = ""
)
