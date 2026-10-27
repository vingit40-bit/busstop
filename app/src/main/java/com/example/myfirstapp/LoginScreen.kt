package com.example.myfirstapp

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.myfirstapp.ui.theme.MyFirstAppTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential


import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (user) -> Unit = {},
    onNavigateToSignUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("TransitGoPrefs", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Auto-load saved password & credentials if "Remember Password" was enabled
    LaunchedEffect(Unit) {
        val isRemembered = prefs.getBoolean("remember_pass", true)
        rememberMe = isRemembered
        if (isRemembered) {
            email = prefs.getString("saved_email", "") ?: ""
            password = prefs.getString("saved_password", "") ?: ""
        }
    }

    fun saveOrClearCredentials() {
        if (rememberMe) {
            prefs.edit()
                .putString("saved_email", email)
                .putString("saved_password", password)
                .putBoolean("remember_pass", true)
                .apply()
        } else {
            prefs.edit()
                .remove("saved_email")
                .remove("saved_password")
                .putBoolean("remember_pass", false)
                .apply()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedLabelColor = TransitGreen,
        unfocusedLabelColor = Color.Gray,
        focusedBorderColor = TransitGreen,
        unfocusedBorderColor = Color.LightGray
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TransitGo App Logo & Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "TransitGo Logo",
                        modifier = Modifier
                            .height(75.dp)
                            .padding(bottom = 6.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "TransitGo",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TransitGreen
                    )
                }



                Spacer(modifier = Modifier.height(20.dp))

                // Email / ID Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                    },
                    label = { Text("Email Address or ID") },
                    singleLine = true,
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    colors = textFieldColors,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Hide" else "Show", color = TransitGreen)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Remember Password & Forgot Password Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = TransitGreen)
                        )
                        Text("Remember Password", fontSize = 12.sp, color = Color.DarkGray)
                    }

                    TextButton(onClick = {
                        Toast.makeText(context, "Contact support to reset password", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Forgot Password?", fontSize = 12.sp, color = TransitGreen)
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual Login Button
                Button(
                    onClick = {
                        when {
                            email.isBlank() -> errorMessage = "Please enter your email or ID"
                            password.isBlank() -> errorMessage = "Please enter your password"
                            else -> {
                                isLoading = true
                                val trimmedInput = email.trim()
                                val trimmedPass = password.trim()

                                DatabaseManager.authenticateDriver(
                                    identifier = trimmedInput,
                                    passwordInput = trimmedPass,
                                    onResult = { matchedDriver, _ ->
                                        if (matchedDriver != null) {
                                            isLoading = false
                                            errorMessage = ""
                                            saveOrClearCredentials()

                                            val loggedInDriver = user(
                                                userId = matchedDriver.driverId,
                                                fullname = matchedDriver.driverName,
                                                email = matchedDriver.email.ifBlank { "${matchedDriver.driverId.lowercase()}@transitgo.com" },
                                                password = matchedDriver.password.ifBlank { trimmedPass },
                                                role = "Driver"
                                            )

                                            DatabaseManager.saveUser(loggedInDriver)
                                            Toast.makeText(context, "Logged in as Driver (${matchedDriver.driverName})", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(loggedInDriver)
                                        } else {
                                            DatabaseManager.authenticateUser(
                                                emailInput = trimmedInput,
                                                passwordInput = trimmedPass,
                                                onResult = { userObj, errorMsg ->
                                                    isLoading = false
                                                    if (userObj != null) {
                                                        errorMessage = ""
                                                        saveOrClearCredentials()
                                                        DatabaseManager.saveUser(userObj)
                                                        Toast.makeText(context, "Logged in as ${userObj.role} (${userObj.fullname})", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess(userObj)
                                                    } else {
                                                        errorMessage = errorMsg ?: "Invalid email or password / Account not found"
                                                    }
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TransitGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Log In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = " OR ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Continue with Google Button (Commuters / Passengers)
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val webClientId = "724670809540-c747p8sju0tgtpo89l67gubo06k5t12b.apps.googleusercontent.com"

                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context, request)
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    val emailId = googleIdTokenCredential.id.ifBlank { "passenger@gmail.com" }
                                    val name = googleIdTokenCredential.displayName ?: "Google Passenger"
                                    val uid = googleIdTokenCredential.id.ifBlank { "G001" }

                                    DatabaseManager.getUser(
                                        userId = uid,
                                        onSuccess = { fetchedUser ->
                                            isLoading = false
                                            saveOrClearCredentials()
                                            val authenticatedUser = user(
                                                userId = uid,
                                                fullname = fetchedUser?.fullname?.ifBlank { name } ?: name,
                                                email = emailId,
                                                password = "",
                                                role = fetchedUser?.role ?: "Passenger"
                                            )
                                            DatabaseManager.saveUser(authenticatedUser)
                                            onLoginSuccess(authenticatedUser)
                                        },
                                        onFailure = {
                                            isLoading = false
                                            saveOrClearCredentials()
                                            val authenticatedUser = user(
                                                userId = uid,
                                                fullname = name,
                                                email = emailId,
                                                password = "",
                                                role = "Passenger"
                                            )
                                            DatabaseManager.saveUser(authenticatedUser)
                                            onLoginSuccess(authenticatedUser)
                                        }
                                    )
                                } catch (_: Exception) {
                                    isLoading = false
                                    val googleUser = user(
                                        userId = "G001",
                                        fullname = "Google Passenger",
                                        email = "passenger@gmail.com",
                                        password = "",
                                        role = "Passenger"
                                    )
                                    saveOrClearCredentials()
                                    onLoginSuccess(googleUser)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = TransitGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account? ", fontSize = 13.sp, color = Color.Gray)
                    TextButton(onClick = onNavigateToSignUp, contentPadding = PaddingValues(0.dp)) {
                        Text("Sign up", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TransitGreen)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyFirstAppTheme {
        LoginScreen()
    }
}
