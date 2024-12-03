package com.example.iems5725_Classroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.network.*
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.launch
import java.net.IDN

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContrastAwareReplyTheme {
                EditProfileUI()
            }
        }
    }
    /*
    @Composable
    fun EditProfileScreen() {
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val username = sharedPref.getString("username", "")!!
        var password by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var securityAnswer by remember { mutableStateOf("") }
        var nickName by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf("Select Option") }
        val options = listOf(
            "Change Password",
            "Reset Password",
            "Change Nickname",
            "Change Security Answer"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 当前密码输入框
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 新密码输入框
            TextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 确认新密码输入框
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = securityAnswer,
                onValueChange = { securityAnswer = it },
                label = { Text("What's your favorite food?") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = nickName,
                onValueChange = { nickName = it },
                label = { Text("Give you a new name") },
                modifier = Modifier.fillMaxWidth(),
            )

            // 错误提示文本
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    lifecycleScope.launch {
                        val response = doEdit(username, password, newPassword, securityAnswer, nickName)
                        if (response.status == "success") {
                            finish()
                        }
                        else {
                            errorMessage = response.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }

        }
    }
    */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditProfileUI(){
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Edit Profile", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                )
            },
            content = { paddingValues ->
                EditProfileScreen(
                    modifier = Modifier.padding(paddingValues),
                )
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditProfileScreen(modifier: Modifier) {
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)


        val username = sharedPref.getString("username", "")!!
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var securityAnswer by remember { mutableStateOf("") }
        var newSecurityAnswer by remember { mutableStateOf("") }
        var nickName by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }
        var showDialog by remember { mutableStateOf(false) }
        var dialogMessage by remember { mutableStateOf("") }

        // Dropdown menu state
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf("Select Option") }
        val options = listOf(
            "Change Password",
            "Reset Password with Security Answer",
            "Change Nickname",
            "Change Security Answer"
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {

                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            finish()
                        }
                    ) {
                        Text("OK")
                    }
                },
                title = {
                    Text("Notification")
                },
                text = {
                    Text(dialogMessage)
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {


            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "User: $username",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedOption,
                    onValueChange = { },
                    label = { Text("Choose an option") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedOption = option
                                expanded = false
                                // Reset all input fields when selection changes
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                securityAnswer = ""
                                newSecurityAnswer = ""
                                nickName = ""
                                errorMessage = ""
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conditional input fields based on selected option
            when (selectedOption) {
                "Change Password" -> {
                    // Current Password
                    TextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // New Password
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confirm New Password
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                "Reset Password with Security Answer" -> {
                    // Security Answer
                    TextField(
                        value = securityAnswer,
                        onValueChange = { securityAnswer = it },
                        label = { Text("What's your favorite food?") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // New Password
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confirm New Password
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }

                "Change Nickname" -> {
                    // New Nickname
                    TextField(
                        value = nickName,
                        onValueChange = { nickName = it },
                        label = { Text("New Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                "Change Security Answer" -> {

                    TextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // New Security Answer
                    TextField(
                        value = newSecurityAnswer,
                        onValueChange = { newSecurityAnswer = it },
                        label = { Text("New Security Answer") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    // Handle submission based on selected option
                    when (selectedOption) {
                        "Change Password" -> {
                            if (newPassword == confirmPassword) {
                                lifecycleScope.launch {
                                    val response = doEdit(username = username, oldPassword = currentPassword, newPassword = newPassword)
                                    if (response.status == "success") {
                                        showDialog = true
                                        dialogMessage = response.message
                                    } else {
                                        errorMessage = response.message
                                    }
                                }
                            } else {
                                errorMessage = "Passwords do not match."
                            }
                        }

                        "Reset Password with Security Answer" -> {
                            if (newPassword == confirmPassword) {
                                lifecycleScope.launch {
                                    val response = doEdit(username = username, newPassword = newPassword, securityAnswer = securityAnswer)
                                    if (response.status == "success") {
                                        finish()
                                    } else {
                                        errorMessage = response.message
                                    }
                                }
                            } else {
                                errorMessage = "Passwords do not match."
                            }
                        }

                        "Change Nickname" -> {
                            if (nickName.isNotEmpty()) {
                                lifecycleScope.launch {
                                    val response = doEdit(username = username, nickName = nickName)
                                    if (response.status == "success") {
                                        finish()
                                    } else {
                                        errorMessage = response.message
                                    }
                                }
                            } else {
                                errorMessage = "Nickname cannot be empty."
                            }
                        }

                        "Change Security Answer" -> {
                            if (securityAnswer.isNotEmpty() && newSecurityAnswer.isNotEmpty()) {
                                lifecycleScope.launch {
                                    val response = doEdit(username = username, oldPassword = currentPassword, securityAnswer = securityAnswer)
                                    if (response.status == "success") {
                                        finish()
                                    } else {
                                        errorMessage = response.message
                                    }
                                }
                            } else {
                                errorMessage = "Please fill in all fields."
                            }
                        }

                        else -> {
                            errorMessage = "Please select an option."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }

            Button(
                onClick = {
                    finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }

}

private suspend fun doEdit(username: String, oldPassword: String? = null, newPassword: String? = null, securityAnswer: String? = null, nickName: String? = null): StandardResponse {
    val api = RetrofitClient.apiService
    return api.changeInfo(ChangeInfoRequest(username, oldPassword, newPassword, securityAnswer, nickName))
}