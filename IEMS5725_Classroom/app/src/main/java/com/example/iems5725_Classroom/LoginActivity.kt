package com.example.iems5725_Classroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.iems5725_Classroom.network.*
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (NetworkUtil.isNetworkAvailable(this)) {
            setContent {
                ContrastAwareReplyTheme {  // 使用主题设置
                    val context = applicationContext
                    var username by remember { mutableStateOf("") } // 存储输入的用户名
                    var password by remember { mutableStateOf("") } // 存储输入的密码
                    var errorMessage by remember { mutableStateOf("") } // 错误信息
                    var rememberMe by remember { mutableStateOf(false) }
                    var autoLogin by remember { mutableStateOf(false) }
                    var fcmToken = ""
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.d("FCM", "Fetching FCM registration token failed", task.exception)
                            return@addOnCompleteListener
                        }

                        fcmToken = task.result
                        Log.d("FCM", "FCM registration token: $fcmToken")
                    }

                    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                    LaunchedEffect(Unit) {
                        rememberMe = sharedPref.getBoolean("rememberMe", false)
                        autoLogin = sharedPref.getBoolean("autoLogin", false)
                        if (autoLogin) {
                            val token = sharedPref.getString("token", "")!!
                            if (token.isNotEmpty()) {
                                lifecycleScope.launch {
                                    val response = doAuth(token)
                                    if (response.status == "success") {
                                        val newToken = doRefresh(token).token
                                        with(sharedPref.edit()) {
                                            putString("token", newToken)
                                            apply()
                                        }
                                        Toast.makeText(
                                            context,
                                            "Auto Login Successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startActivity(Intent(context, MainActivity::class.java))
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to auto login.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        if (rememberMe) {
                            val savedUsername = sharedPref.getString("username", "")
                            val savedPassword = sharedPref.getString("password", "")
                            if (!savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                                username = savedUsername
                                password = savedPassword
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Login", style = MaterialTheme.typography.headlineLarge)

                        Spacer(modifier = Modifier.height(16.dp))

                        // 用户名输入框
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = errorMessage.isNotEmpty()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {

                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = errorMessage.isNotEmpty(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            Text(
                                text = "Forgot?",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .clickable {
                                        if (username.isEmpty()) {
                                            errorMessage = "Please fill in username first."
                                        }
                                        else {
                                            with(sharedPref.edit()) {
                                                putString("username", username)
                                                apply()
                                            }
                                            startActivity(
                                                Intent(
                                                    context,
                                                    EditProfileActivity::class.java
                                                )
                                            )
                                        }
                                    },
                                color = Color.Black,
                                style = TextStyle(fontSize = 16.sp)
                            )

                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remember me?")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoLogin,
                                onCheckedChange = { autoLogin = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto Login?")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 错误提示文本
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 登录按钮
                        Button(
                            onClick = {
                                if (username.isEmpty() || password.isEmpty()) {
                                    errorMessage = "Please fill in all fields."
                                } else {
                                    lifecycleScope.launch {
                                        val response = doLogin(username, password)
                                        if (response.status == "success") {
                                            with(sharedPref.edit()) {
                                                putString("username", username)
                                                putString("password", password)
                                                putString("token", response.token)
                                                putBoolean("rememberMe", rememberMe)
                                                putBoolean("autoLogin", autoLogin)
                                                apply()
                                            }
                                            val fcm = doSubmitToken(username, fcmToken)
                                            if (!fcm) {
                                                Log.d("FCM", "Submit FCM Error")
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            errorMessage = response.message
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Login")
                        }

                        /*
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    val token = sharedPref.getString("token", null)
                                    if (token != null) {
                                        val response = doAuth(token)
                                        if (response.status == "success") {
                                            val new_token = doRefresh(token).token
                                            with (sharedPref.edit()) {
                                                putString("username", username)
                                                putString("token", new_token)
                                                apply()
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        else {
                                            errorMessage = response.message
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use saved token to login")
                        }
                        */

                        Spacer(modifier = Modifier.height(16.dp))

                        // 跳转到注册页面的按钮
                        TextButton(
                            onClick = {
                                val intent = Intent(context, RegisterActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        ) {
                            Text("Don't have an account? Register")
                        }
                    }
                }
            }
        } else {
            // 网络不可用，提示用户
            Toast.makeText(this, "网络连接不可用，请检查网络设置", Toast.LENGTH_LONG).show()
            finish() // 结束当前活动
        }


    }

    private suspend fun doLogin(username: String, password: String): LoginResponse {
        val api = RetrofitClient.apiService
        return api.login(LoginRequest(username, password))
    }

    private suspend fun doAuth(token: String): AuthResponse {
        val api = RetrofitClient.apiService
        return api.auth("Bearer $token")
    }

    private suspend fun doRefresh(token: String): RefreshResponse {
        val api = RetrofitClient.apiService
        return api.refreshToken("Bearer $token")
    }

    private suspend fun doSubmitToken(username: String, token: String): Boolean {
        val api = RetrofitClient.apiService
        return api.submitFCMToken(FCMSubmitRequest(username, token)).status == "success"
    }
}
