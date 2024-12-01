package com.example.iems5725_Classroom
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class EditProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EditProfileScreen()
        }
    }
}

@Composable
fun EditProfileScreen() {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val editor = sharedPref.edit()

    var username by remember { mutableStateOf(sharedPref.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(sharedPref.getString("password", "") ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Edit Profile", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // 用户名输入框
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.isNotEmpty() && username.isBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 当前密码输入框
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Current Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage.isNotEmpty() && password.isBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 新密码输入框
        TextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage.isNotEmpty() && newPassword.isBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 确认新密码输入框
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage.isNotEmpty() && confirmPassword.isBlank()
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

        // 保存修改按钮
        Button(
            onClick = {
                // 验证输入
                if (username.isBlank()) {
                    errorMessage = "Username cannot be empty."
                } else if (password.isBlank()) {
                    errorMessage = "Current password cannot be empty."
                } else if (newPassword != confirmPassword) {
                    errorMessage = "New passwords do not match."
                } else {
                    // 更新用户信息
                    editor.putString("username", username)
                    if (newPassword.isNotBlank()) {
                        editor.putString("password", newPassword)
                    }
                    editor.apply()

                    // 成功后跳转回个人资料页
                    context.startActivity(Intent(context, ProfileActivity::class.java))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}