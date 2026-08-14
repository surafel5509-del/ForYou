package com.example.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ForyouButton
import com.example.core.designsystem.ForyouTextField
import com.example.ui.theme.ForyouEmerald

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onPasswordResetSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.forgotPasswordEmail.collectAsState()
    val resetToken by viewModel.resetPasswordToken.collectAsState()
    val newPassword by viewModel.resetNewPassword.collectAsState()
    val newPasswordConfirm by viewModel.resetNewPasswordConfirm.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header Top Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("forgot_password_back_button")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reset Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Enter your account email to receive reset instructions, or enter your token below.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step 1: Request Reset
            Text(
                text = "1. Request Reset Link",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            ForyouTextField(
                value = email,
                onValueChange = { viewModel.onForgotPasswordEmailChanged(it) },
                label = "Account Email",
                placeholder = "name@example.com",
                leadingIcon = Icons.Default.Email,
                isError = uiState.fieldErrors.containsKey("forgotPasswordEmail"),
                errorMessage = uiState.fieldErrors["forgotPasswordEmail"],
                testTag = "forgot_password_email_input",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            ForyouButton(
                text = "Send Reset Email",
                onClick = { viewModel.requestPasswordReset() },
                isLoading = uiState.isLoading && !uiState.isPasswordResetEmailSent,
                testTag = "send_reset_email_button",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step 2: Enter Token & New Password
            Text(
                text = "2. Set New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            ForyouTextField(
                value = resetToken,
                onValueChange = { viewModel.onResetPasswordTokenChanged(it) },
                label = "Reset Token",
                placeholder = "Paste reset token from email",
                leadingIcon = Icons.Default.Key,
                isError = uiState.fieldErrors.containsKey("resetPasswordToken"),
                errorMessage = uiState.fieldErrors["resetPasswordToken"],
                testTag = "reset_password_token_input",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            ForyouTextField(
                value = newPassword,
                onValueChange = { viewModel.onResetNewPasswordChanged(it) },
                label = "New Password (min 8 chars)",
                placeholder = "Enter new password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isError = uiState.fieldErrors.containsKey("resetNewPassword"),
                errorMessage = uiState.fieldErrors["resetNewPassword"],
                testTag = "reset_new_password_input",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            ForyouTextField(
                value = newPasswordConfirm,
                onValueChange = { viewModel.onResetNewPasswordConfirmChanged(it) },
                label = "Confirm New Password",
                placeholder = "Re-enter new password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                isError = uiState.fieldErrors.containsKey("resetNewPasswordConfirm"),
                errorMessage = uiState.fieldErrors["resetNewPasswordConfirm"],
                testTag = "reset_new_password_confirm_input",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            // Password Requirements Indicator
            if (newPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResetPasswordRequirementPill(
                        text = "8+ characters",
                        satisfied = uiState.resetPasswordValidation.hasMinLength,
                        modifier = Modifier.weight(1f)
                    )
                    ResetPasswordRequirementPill(
                        text = "Passwords match",
                        satisfied = uiState.resetPasswordValidation.matchesConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Success feedback
            if (uiState.successMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ForyouEmerald.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ForyouEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.successMessage ?: "",
                            color = ForyouEmerald,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Error feedback
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ForyouButton(
                text = "Confirm & Save Password",
                onClick = {
                    viewModel.confirmPasswordReset(onSuccess = onPasswordResetSuccess)
                },
                isLoading = uiState.isLoading && uiState.isPasswordResetEmailSent,
                testTag = "confirm_reset_password_button",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResetPasswordRequirementPill(
    text: String,
    satisfied: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (satisfied) ForyouEmerald else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val bgColor = if (satisfied) ForyouEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
