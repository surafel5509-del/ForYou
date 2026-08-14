package com.example.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ForyouButton
import com.example.core.designsystem.ForyouTextField
import com.example.ui.theme.ForyouBrandGradient
import com.example.ui.theme.ForyouEmerald

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.registerEmail.collectAsState()
    val username by viewModel.registerUsername.collectAsState()
    val name by viewModel.registerName.collectAsState()
    val password by viewModel.registerPassword.collectAsState()
    val passwordConfirm by viewModel.registerPasswordConfirm.collectAsState()

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Brand Emblem
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ForyouBrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "F",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Join Foryou community today",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields
                ForyouTextField(
                    value = email,
                    onValueChange = { viewModel.onRegisterEmailChanged(it) },
                    label = "Email",
                    placeholder = "name@example.com",
                    leadingIcon = Icons.Default.Email,
                    isError = uiState.fieldErrors.containsKey("registerEmail"),
                    errorMessage = uiState.fieldErrors["registerEmail"],
                    testTag = "register_email_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForyouTextField(
                    value = username,
                    onValueChange = { viewModel.onRegisterUsernameChanged(it) },
                    label = "Username",
                    placeholder = "Choose a unique username (min 3 chars)",
                    leadingIcon = Icons.Default.AlternateEmail,
                    isError = uiState.fieldErrors.containsKey("registerUsername"),
                    errorMessage = uiState.fieldErrors["registerUsername"],
                    testTag = "register_username_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForyouTextField(
                    value = name,
                    onValueChange = { viewModel.onRegisterNameChanged(it) },
                    label = "Full Name",
                    placeholder = "Your display name",
                    leadingIcon = Icons.Default.AccountCircle,
                    isError = uiState.fieldErrors.containsKey("registerName"),
                    errorMessage = uiState.fieldErrors["registerName"],
                    testTag = "register_name_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForyouTextField(
                    value = password,
                    onValueChange = { viewModel.onRegisterPasswordChanged(it) },
                    label = "Password (min 8 chars)",
                    placeholder = "Create a secure password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    isError = uiState.fieldErrors.containsKey("registerPassword"),
                    errorMessage = uiState.fieldErrors["registerPassword"],
                    testTag = "register_password_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                ForyouTextField(
                    value = passwordConfirm,
                    onValueChange = { viewModel.onRegisterPasswordConfirmChanged(it) },
                    label = "Confirm Password",
                    placeholder = "Re-enter your password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    isError = uiState.fieldErrors.containsKey("registerPasswordConfirm"),
                    errorMessage = uiState.fieldErrors["registerPasswordConfirm"],
                    testTag = "register_password_confirm_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                // Password Requirements Indicator
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PasswordRequirementPill(
                            text = "8+ characters",
                            satisfied = uiState.registerPasswordValidation.hasMinLength,
                            modifier = Modifier.weight(1f)
                        )
                        PasswordRequirementPill(
                            text = "Passwords match",
                            satisfied = uiState.registerPasswordValidation.matchesConfirm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Error Message banner
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

                // Sign Up Button
                ForyouButton(
                    text = "Sign Up",
                    onClick = {
                        viewModel.register { registeredEmail ->
                            onRegistrationSuccess(registeredEmail)
                        }
                    },
                    isLoading = uiState.isLoading,
                    testTag = "register_submit_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom Switch to Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Log In",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .testTag("go_to_login_button")
                        .clickable(onClick = onNavigateToLogin)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun PasswordRequirementPill(
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
