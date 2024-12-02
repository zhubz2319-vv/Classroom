package com.example.iems5725_Classroom
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import coil3.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iems5725_Classroom.network.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen()
        }
    }
    /*
    @Composable
    fun ProfileScreen() {
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val username = sharedPref.getString("username", "")!!

        var nickname by remember { mutableStateOf("Loading...") }
        var role by remember { mutableStateOf("Loading...") }
        var showLogoutConfirmation by remember { mutableStateOf(false) }

        LaunchedEffect(username) {
            val response = doInfoRequest(username)
            nickname = response.nickname
            role = response.role
        }

        // 展示个人信息
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Text("Profile", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Account Icon",
                    modifier = Modifier.size(60.dp),
                    tint = Color.Gray
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column{
                    Text("User: $username",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Name: $nickname",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(context, EditProfileActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Account")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showLogoutConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout", color = Color.White)
            }
        }

        if (showLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showLogoutConfirmation = false
                },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmation = false
                            sharedPref.edit().clear().apply()
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    */

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileScreen() {
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val username = sharedPref.getString("username", "") ?: "Unknown"

        var nickname by remember { mutableStateOf("Loading...") }
        var role by remember { mutableStateOf("Loading...") }

        var note by remember { mutableStateOf(sharedPref.getString("user_note", "") ?: "") }
        var isEditingNote by remember { mutableStateOf(false) }

        LaunchedEffect(username) {
            val response = doInfoRequest(username)
            nickname = response.nickname
            role = response.role
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("More", style = MaterialTheme.typography.headlineSmall)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            startActivity(Intent(context, MainActivity::class.java))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            content = { paddingValues ->
                ProfileContent(
                    modifier = Modifier.padding(paddingValues),
                    username = username,
                    nickname = nickname,
                    role = role,
                    context = context,
                    sharedPref = sharedPref,
                    note = note,
                    isEditingNote = isEditingNote,
                    onNoteChange = { newNote -> note = newNote },
                    onNoteEditToggle = { isEditingNote = !isEditingNote }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    isEditingNote = !isEditingNote
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Notes")
                }
            }
        )
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun ProfileContent(
        modifier: Modifier,
        username: String,
        nickname: String,
        role: String,
        context: Context,
        sharedPref: SharedPreferences,
        note: String,
        isEditingNote: Boolean,
        onNoteChange: (String) -> Unit,
        onNoteEditToggle: () -> Unit
    ) {

        var showLogoutConfirmation by remember { mutableStateOf(false) }
        var showInputDialog by remember { mutableStateOf(false) }
        var profilePicUrl by remember { mutableStateOf(sharedPref.getString("profile_pic_url", "") ?: "") }
        var newImageUrl by remember { mutableStateOf(TextFieldValue(profilePicUrl)) }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var isInputUrlSelected by remember { mutableStateOf(false) }

        val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = uri
                sharedPref.edit().putString("profile_pic_url", uri.toString()).apply()
                profilePicUrl = uri.toString()
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { showInputDialog = true }
                ) {
                    if (profilePicUrl.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    } else {
                        GlideImage(
                            model = profilePicUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column{
                    Text("User: $username",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Name: $nickname",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditingNote) {
                TextField(
                    value = note,
                    onValueChange = { newNote -> onNoteChange(newNote) },
                    label = { Text("Your Note") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    singleLine = false
                )
            } else {
                Text(
                    text = note.ifEmpty { "Nothing here yet" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    startActivity(Intent(context, EditProfileActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Account")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout button with confirmation
            Button(
                onClick = {
                    showLogoutConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout", color = Color.White)
            }
        }

        if (showLogoutConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    showLogoutConfirmation = false
                },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmation = false
                            sharedPref.edit().clear().apply()
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showInputDialog) {
            AlertDialog(
                onDismissRequest = { showInputDialog = false },
                title = { Text("Change Profile Photo") },
                text = {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { isInputUrlSelected = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isInputUrlSelected) Color.Gray else Color.LightGray
                                )
                            ) {
                                Text("Local Image")
                            }

                            Button(
                                onClick = { isInputUrlSelected = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isInputUrlSelected) Color.Gray else Color.LightGray
                                )
                            ) {
                                Text("Image URL")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isInputUrlSelected) {
                            TextField(
                                value = newImageUrl,
                                onValueChange = { newImageUrl = it },
                                label = { Text("Image URL") },
                                modifier = Modifier.fillMaxWidth()
                            ) }
                        else {
                            Text("Confirm to select images")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (isInputUrlSelected) {
                            with(sharedPref.edit()) {
                                putString("profile_pic_url", newImageUrl.text)
                                apply()
                            }
                            profilePicUrl = newImageUrl.text
                        } else {
                            getImage.launch("image/*")
                        }
                        showInputDialog = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInputDialog = false }) {
                        Text("Cancel")
                    }
                },

            )
        }
    }


    private suspend fun doInfoRequest(username: String): UserInfoResponse {
        val api = RetrofitClient.apiService
        return api.getInfo(username)
    }
}

