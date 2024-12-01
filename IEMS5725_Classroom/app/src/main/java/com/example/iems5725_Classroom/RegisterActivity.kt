package com.example.iems5725_Classroom

import MainViewModel
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContrastAwareReplyTheme {  // 使用主题设置
                val context = applicationContext
                var username by remember { mutableStateOf("") } // 存储用户名
                var password by remember { mutableStateOf("") } // 存储密码
                var confirmPassword by remember { mutableStateOf("") } // 存储确认密码
                var errorMessage by remember { mutableStateOf("") } // 错误信息

                // 获取SharedPreferences
                val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                // 已注册的用户名列表
                val registeredUsernames = sharedPref.getStringSet("usernames", setOf()) ?: setOf()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), // 设置内边距
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Register", style = MaterialTheme.typography.headlineLarge)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 用户名输入框
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty() && !username.isBlank()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 密码输入框
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty() && !password.isBlank(),
                        visualTransformation = PasswordVisualTransformation() // 隐藏密码输入
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 确认密码输入框
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty() && !confirmPassword.isBlank(),
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

                    // 注册按钮
                    Button(
                        onClick = {
                            when {
                                username.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                                    errorMessage = "All fields must be filled"
                                }
                                password != confirmPassword -> {
                                    errorMessage = "Passwords do not match"
                                }
                                registeredUsernames.contains(username) -> {
                                    errorMessage = "Username already exists"
                                }
                                else -> {
                                    // 保存注册信息到SharedPreferences
                                    val editor = sharedPref.edit()
                                    val newUsernames = registeredUsernames.toMutableSet()
                                    newUsernames.add(username)  // 添加新用户名
                                    editor.putStringSet("usernames", newUsernames)
                                    editor.putString(username, password)  // 保存用户名对应的密码
                                    editor.apply()

                                    // 注册成功后跳转到登录界面
                                    val intent = Intent(context, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish() // 结束注册页面
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 跳转到登录页面的按钮
                    TextButton(
                        onClick = {
                            val intent = Intent(context, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    ) {
                        Text("Already have an account? Login")
                    }
                }
            }
        }
    }
}
