package com.example.iems5725_Classroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import android.util.Log
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.iems5725_Classroom.network.*
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.http.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var FCMToken = ""
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            FCMToken = task.result
            Log.d("FCM", "FCM registration token: $FCMToken")
        }

        setContent {
            ContrastAwareReplyTheme {  // 使用主题设置
                val context = applicationContext
                var username by remember { mutableStateOf("") } // 存储输入的用户名
                var password by remember { mutableStateOf("") } // 存储输入的密码
                var errorMessage by remember { mutableStateOf("") } // 错误信息

                val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

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

                    // 密码输入框
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty(),
                        visualTransformation = PasswordVisualTransformation() // 隐藏密码输入
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

                    // 登录按钮
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                val response = doLogin(username, password)
                                if (response.status == "success") {
                                    with (sharedPref.edit()) {
                                        putString("username", username)
                                        putString("token", response.token)
                                        apply()
                                    }
                                    val fcm = doSubmitToken(username, FCMToken)
                                    if (!fcm) {
                                        Log.d("FCM", "Submit FCM Error")
                                    }
                                    val intent = Intent(context, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                else {
                                    errorMessage = response.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login")
                    }

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