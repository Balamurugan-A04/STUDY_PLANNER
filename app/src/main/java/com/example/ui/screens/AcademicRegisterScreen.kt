package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AcademicRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    onPerformRegister: suspend (String, String, String, Boolean) -> Result<Unit>
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AppTheme(mode = PreparationMode.ACADEMIC) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_academic_register"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Academic Registration",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create your account for Academic Studies",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                InputField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = "Full Name",
                    placeholder = "John Doe",
                    leadingIcon = Icons.Default.Person,
                    testTag = "input_full_name"
                )

                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Email Address",
                    placeholder = "john@example.com",
                    leadingIcon = Icons.Default.Email,
                    testTag = "input_email"
                )

                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = "Password",
                    placeholder = "at least 6 characters",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    testTag = "input_password"
                )

                Spacer(modifier = Modifier.height(16.dp))

                InputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = "Confirm Password",
                    placeholder = "re-enter password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    testTag = "input_confirm_password"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    PrimaryButton(
                        text = "Register & Continue",
                        onClick = {
                            when {
                                fullName.isBlank() -> errorMessage = "Full Name is required"
                                email.isBlank() || !email.contains("@") -> errorMessage = "Valid Email is required"
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                password != confirmPassword -> errorMessage = "Passwords do not match"
                                else -> {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val result = onPerformRegister(fullName, email, password, true)
                                            isLoading = false
                                            if (result.isSuccess) {
                                                onRegisterSuccess()
                                            } else {
                                                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = e.message ?: "Error occurred"
                                        }
                                    }
                                }
                            }
                        },
                        testTag = "btn_register_academic"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryButton(
                        text = "Back to Login",
                        onClick = onNavigateLogin,
                        testTag = "btn_back_login"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
