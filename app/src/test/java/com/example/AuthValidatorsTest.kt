package com.example

import com.example.domain.validator.AuthValidators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorsTest {

    @Test
    fun `validateEmail returns success for valid emails`() {
        val validEmails = listOf(
            "user@example.com",
            "john.doe@company.org",
            "jane_dev+test@foryou.social",
            "contact123@domain.co.uk"
        )
        for (email in validEmails) {
            val result = AuthValidators.validateEmail(email)
            assertTrue("Expected valid for $email", result.isValid)
        }
    }

    @Test
    fun `validateEmail returns error for invalid or empty emails`() {
        val invalidEmails = listOf(
            "",
            "   ",
            "notanemail",
            "user@",
            "@domain.com",
            "user@domain",
            "user with spaces@domain.com"
        )
        for (email in invalidEmails) {
            val result = AuthValidators.validateEmail(email)
            assertFalse("Expected invalid for $email", result.isValid)
        }
    }

    @Test
    fun `validateUsername returns success for valid usernames`() {
        val validUsernames = listOf(
            "john_doe",
            "jane.doe",
            "foryou_user99",
            "abc"
        )
        for (username in validUsernames) {
            val result = AuthValidators.validateUsername(username)
            assertTrue("Expected valid for $username", result.isValid)
        }
    }

    @Test
    fun `validateUsername returns error for invalid usernames`() {
        val invalidUsernames = listOf(
            "",
            "ab", // too short
            "a".repeat(31), // too long
            "user!name", // special char
            ".leading_dot",
            "trailing_dot.",
            "consecutive..dots"
        )
        for (username in invalidUsernames) {
            val result = AuthValidators.validateUsername(username)
            assertFalse("Expected invalid for '$username'", result.isValid)
        }
    }

    @Test
    fun `validatePassword returns success for 8+ character passwords`() {
        val result = AuthValidators.validatePassword("P@ssword123")
        assertTrue(result.isValid)
    }

    @Test
    fun `validatePassword returns error for short or blank passwords`() {
        val shortResult = AuthValidators.validatePassword("1234567")
        assertFalse(shortResult.isValid)

        val emptyResult = AuthValidators.validatePassword("")
        assertFalse(emptyResult.isValid)
    }

    @Test
    fun `validatePasswordConfirm checks match and minimum length`() {
        val matching = AuthValidators.validatePasswordConfirm("StrongPassword1!", "StrongPassword1!")
        assertTrue(matching.isValid)

        val mismatched = AuthValidators.validatePasswordConfirm("StrongPassword1!", "DifferentPassword2@")
        assertFalse(mismatched.isValid)
        assertEquals("Passwords do not match", mismatched.errorMessage)
    }

    @Test
    fun `validateToken checks token non-blank and minimum length`() {
        assertTrue(AuthValidators.validateToken("TOKEN_12345").isValid)
        assertFalse(AuthValidators.validateToken("").isValid)
        assertFalse(AuthValidators.validateToken("abc").isValid)
    }

    @Test
    fun `evaluatePasswordStrength computes flags correctly`() {
        val state = AuthValidators.evaluatePasswordStrength("Secret123!", "Secret123!")
        assertTrue(state.hasMinLength)
        assertTrue(state.hasDigitOrSpecial)
        assertTrue(state.hasUppercaseOrLowercase)
        assertTrue(state.matchesConfirm)
        assertTrue(state.isValid)
    }
}
