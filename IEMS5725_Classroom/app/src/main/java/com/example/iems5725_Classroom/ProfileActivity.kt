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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iems5725_Classroom.network.*
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContrastAwareReplyTheme {
                ProfileScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileScreen() {
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val username = sharedPref.getString("username", "") ?: "Unknown"

        var nickname by remember { mutableStateOf("Loading...") }
        var role by remember { mutableStateOf("Loading...") }

        var note by remember { mutableStateOf(sharedPref.getString("user_note", "")!!) }
        var isEditingNote by remember { mutableStateOf(false) }

        LaunchedEffect(username) {
            val response = doInfoRequest(username)
            if (response.status == "success") {
                nickname = response.nickname
                role = response.role
            }
            else {
                Toast.makeText(
                    context,
                    "Something wrong. Please come back later.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Profile", style = MaterialTheme.typography.headlineMedium)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            finish()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
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
                    onNoteChange = { newNote -> note = newNote }
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
        onNoteChange: (String) -> Unit
    ) {

        val clipboardManager = LocalClipboardManager.current
        var showLogoutConfirmation by remember { mutableStateOf(false) }
        var showInputDialog by remember { mutableStateOf(false) }
        var profilePicUrl by remember { mutableStateOf(sharedPref.getString("profile_pic_url", "") ?: "") }
        var newImageUrl by remember { mutableStateOf(TextFieldValue(profilePicUrl)) }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var isInputUrlSelected by remember { mutableStateOf(false) }
        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        var isUploading by remember { mutableStateOf(false) }

        val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = uri
                sharedPref.edit().putString("profile_pic_url", uri.toString()).apply()
                profilePicUrl = uri.toString()
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val getFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = uri
            }
        }

        fun getFileNameFromUri(): String? {
            var fileName: String? = null
            val cursor = context.contentResolver.query(selectedFileUri!!, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    fileName = it.getString(nameIndex)
                }
            }
            return fileName
        }

        fun getFileFromUri(): File? {
            val fileName = getFileNameFromUri() ?: "Default_Name"
            val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
            val tempFile = File(context.cacheDir, fileName)

            inputStream?.let { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return if (tempFile.exists()) tempFile else null
        }

        fun createMultipartBodyPart(file: File, parameterName: String): MultipartBody.Part {
            val requestBody: RequestBody = file
                .asRequestBody("application/octet-stream".toMediaTypeOrNull())

            return MultipartBody.Part.createFormData(parameterName, file.name, requestBody)
        }

        fun prepareFileForUpload(): MultipartBody.Part? {
            val file = getFileFromUri()
            return file?.let {
                createMultipartBodyPart(it, "file")
            }
        }

        fun doUploadFile() {
            val multipartBodyPart = prepareFileForUpload()

            if (multipartBodyPart != null) {
                lifecycleScope.launch {
                    isUploading = true
                    val api = RetrofitClient.apiService
                    val response = api.uploadFile(multipartBodyPart)
                    if (response.status == "success") {
                        println("HERE IS THE FILE_ID ${response.file_id}")
                        selectedFileUri = null
                        clipboardManager.setText(AnnotatedString("https://chat.lamitt.com/download_file?file_id=${response.file_id}"))
                        Toast.makeText(context, "File URL copied to clipboard.", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        println("FILE UPLOAD UNSUCCESSFULLY!")
                    }
                    isUploading = false
                }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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
                    Text(
                        nickname,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    /*
                    Text(
                        username,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    */
                    Text(
                        role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
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
                    text = note.ifEmpty { "You can note something here using the button below." },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    getFile.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload File")
            }

            if (selectedFileUri != null && !isUploading) {
                doUploadFile()
            }

            if (isUploading) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    startActivity(Intent(context, EditProfileActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Account")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    with(sharedPref.edit()) {
                        putBoolean("autoLogin", false)
                        apply()
                    }
                    Toast.makeText(
                        context,
                        "Disable Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disable Auto Login", color = Color.White)
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
                            with(sharedPref.edit()) {
                                putString("password", "")
                                putString("token", "")
                                putBoolean("rememberMe", false)
                                putBoolean("autoLogin", false)
                                apply()
                            }
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

