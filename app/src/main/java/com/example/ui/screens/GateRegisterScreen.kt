package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GateDepartment
import com.example.model.PreparationMode
import com.example.ui.components.InputField
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit,
    onPerformRegister: suspend (String, String, String, Int, String, GateDepartment, Boolean) -> Result<Unit>
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var collegeName by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(GateDepartment.COMPUTER_SCIENCE) }
    var isDeptDropdownExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AppTheme(mode = PreparationMode.PROFESSIONAL_GATE) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_gate_register"),
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
                    text = "GATE Aspirant Registration",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Specialized preparation for GATE success",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                InputField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    label = "Full Name",
                    placeholder = "Alex Morgan",
                    leadingIcon = Icons.Default.Person,
                    testTag = "input_full_name"
                )

                Spacer(modifier = Modifier.height(14.dp))

                InputField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Email Address",
                    placeholder = "alex@example.com",
                    leadingIcon = Icons.Default.Email,
                    testTag = "input_email"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = "Password",
                        placeholder = "password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        modifier = Modifier.weight(1f),
                        testTag = "input_password"
                    )

                    InputField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = "Confirm",
                        placeholder = "confirm",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        modifier = Modifier.weight(1f),
                        testTag = "input_confirm_password"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { char -> char.isDigit() }; errorMessage = null },
                        label = "Age",
                        placeholder = "21",
                        leadingIcon = Icons.Default.Cake,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.4f),
                        testTag = "input_age"
                    )

                    InputField(
                        value = collegeName,
                        onValueChange = { collegeName = it; errorMessage = null },
                        label = "College Name",
                        placeholder = "IIT / NIT / University",
                        leadingIcon = Icons.Default.AccountBalance,
                        modifier = Modifier.weight(0.6f),
                        testTag = "input_college"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GATE Department Dropdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GATE Department",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = isDeptDropdownExpanded,
                        onExpandedChange = { isDeptDropdownExpanded = !isDeptDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDept.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeptDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("dropdown_gate_dept")
                        )

                        ExposedDropdownMenu(
                            expanded = isDeptDropdownExpanded,
                            onDismissRequest = { isDeptDropdownExpanded = false }
                        ) {
                            GateDepartment.values().forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.displayName, fontSize = 14.sp) },
                                    onClick = {
                                        selectedDept = dept
                                        isDeptDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    PrimaryButton(
                        text = "Register GATE Account",
                        onClick = {
                            val age = ageText.toIntOrNull()
                            when {
                                fullName.isBlank() -> errorMessage = "Full Name is required"
                                email.isBlank() || !email.contains("@") -> errorMessage = "Valid Email is required"
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                password != confirmPassword -> errorMessage = "Passwords do not match"
                                age == null || age < 15 || age > 99 -> errorMessage = "Valid numeric Age is required"
                                collegeName.isBlank() -> errorMessage = "College Name is required"
                                else -> {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val result = onPerformRegister(fullName, email, password, age, collegeName, selectedDept, true)
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
                        testTag = "btn_register_gate"
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
