package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreparationMode
import com.example.ui.components.InputField
import com.example.ui.components.ModeSelectionCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (PreparationMode) -> Unit,
    onNavigateRegister: (PreparationMode) -> Unit,
    onPerformLogin: suspend (String, String, PreparationMode, Boolean) -> Result<Unit>
) {
    var selectedMode by remember { mutableStateOf(PreparationMode.ACADEMIC) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AppTheme(mode = selectedMode) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_login"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Welcome Back!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Login to continue your learning journey",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Select Preparation Mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeSelectionCard(
                        mode = PreparationMode.ACADEMIC,
                        isSelected = selectedMode == PreparationMode.ACADEMIC,
                        onSelect = { selectedMode = PreparationMode.ACADEMIC },
                        modifier = Modifier.weight(1f)
                    )

                    ModeSelectionCard(
                        mode = PreparationMode.PROFESSIONAL_GATE,
                        isSelected = selectedMode == PreparationMode.PROFESSIONAL_GATE,
                        onSelect = { selectedMode = PreparationMode.PROFESSIONAL_GATE },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                InputField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Email Address",
                    placeholder = "enter your email",
                    leadingIcon = Icons.Default.Email,
                    testTag = "input_email"
                )

                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = "Password",
                    placeholder = "enter your password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    testTag = "input_password"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            modifier = Modifier.testTag("chk_remember_me")
                        )
                        Text(text = "Remember Me", fontSize = 13.sp, color = TextPrimary)
                    }

                    Text(
                        text = "Forgot Password?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { /* Reset info */ }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    PrimaryButton(
                        text = "Login",
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                                return@PrimaryButton
                            }
                            isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val result = onPerformLogin(email, password, selectedMode, rememberMe)
                                        isLoading = false
                                        if (result.isSuccess) {
                                            onLoginSuccess(selectedMode)
                                        } else {
                                            val msg = result.exceptionOrNull()?.message ?: "Login failed"
                                            errorMessage = if (msg.startsWith("MODE_MISMATCH|")) {
                                                val modeStr = msg.substringAfter("|")
                                                val friendlyMode = if (modeStr == "PROFESSIONAL_GATE") "Professional (GATE)" else "Academic"
                                                "This account is registered for $friendlyMode mode. Please select $friendlyMode mode."
                                            } else if (msg.contains("password", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) {
                                                "Incorrect email or password."
                                            } else if (msg.contains("no user", ignoreCase = true)) {
                                                "No account found with this email."
                                            } else if (msg.contains("network", ignoreCase = true)) {
                                                "Unable to connect. Please check your internet connection."
                                            } else {
                                                msg
                                            }
                                        }
                                    } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = e.message ?: "An error occurred"
                                }
                            }
                        },
                        testTag = "btn_login"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryButton(
                        text = "Create New Account",
                        onClick = { onNavigateRegister(selectedMode) },
                        testTag = "btn_create_account"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
