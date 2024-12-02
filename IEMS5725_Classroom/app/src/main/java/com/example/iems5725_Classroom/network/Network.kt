package com.example.iems5725_Classroom.network

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface ApiService {

    @GET("/")
    fun root(): Call<RootResponse>

    @POST("/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @GET("/auth")
    suspend fun auth(@Header("Authorization") token: String): AuthResponse

    @POST("/refresh")
    suspend fun refreshToken(@Header("Authorization") token: String): RefreshResponse

    @POST("/register")
    suspend fun register(@Body registerRequest: RegisterRequest): RegisterResponse

    @GET("/get_info")
    suspend fun getInfo(@Query("username") username: String): UserInfoResponse

    @POST("/change_info")
    suspend fun changeInfo(@Body request: ChangeInfoRequest): StandardResponse

    @GET("/get_courses")
    suspend fun getCourses(): AllCoursesResponse

    @GET("/get_courseinfo")
    suspend fun getCourseInfo(
        @Query("course_code") course_code: String,
        @Query("section") section: String
    ): CourseInfoResponse

    @POST("/add_courseinfo")
    suspend fun addCourseInfo(@Body infoRequest: InfoRequest): StandardResponse

    @POST("/select_course")
    suspend fun selectCourse(@Body addDropRequest: AddDropRequest): StandardResponse

    @POST("/send_message")
    suspend fun sendMessage(@Body messageRequest: MessageRequest): StandardResponse

    @POST("/submit_fcm_token")
    suspend fun submitFCMToken(@Body request: FCMSubmitRequest): StandardResponse

    @GET("/get_filename")
    suspend fun getFileName(@Query("file_id") fileID: String): FileNameResponse

    @GET("/download_file")
    suspend fun downloadFile(@Query("file_id") fileID: String): ResponseBody

    @Multipart
    @POST("/upload_file")
    suspend fun uploadFile(@Part file: MultipartBody.Part): uploadResponse
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val security_answer: String,
    val auth_code: String?
)

data class InfoRequest(
    val username: String,
    val course_code: String,
    val section: String,
    val title: String,
    val body: String,
    val file_id: String?
)

data class AddDropRequest(
    val username: String,
    val course_code: String,
    val action: String
)

data class MessageRequest(
    val room_code: String,
    val username: String,
    val message: String,
    val file_id: String?
)

data class FCMSubmitRequest(
    val username: String,
    val token: String
)

data class ChangeInfoRequest(
    val username: String,
    val old_password: String?,
    val new_password: String?,
    val security_answer: String?,
    val nickname: String?
)

data class RootResponse(
    val message: String
)

data class LoginResponse(
    val status: String,
    val message: String,
    val token: String
)

data class FileNameResponse(
    val status: String,
    val message: String,
    val file_name: String
)

data class AuthResponse(
    val status: String,
    val message: String,
    val username: String
)

data class RefreshResponse(
    val status: String,
    val message: String,
    val token: String
)

data class RegisterResponse(
    val status: String,
    val message: String,
    val token: String
)

data class UserInfoResponse(
    val status: String,
    val message: String,
    val nickname: String,
    val role: String
)

data class AllCoursesResponse(
    val status: String,
    val message: String,
    val courses: List<Course>
)

data class Course(
    val course_name: String,
    val course_code: String,
    val instructor: String,
    val students: List<String>
)

data class CourseInfoResponse(
    val status: String,
    val message: String,
    val infos: List<CourseInfo>
)

data class CourseInfo(
    val course_code: String,
    val section: String,
    val by: String,
    val time: String,
    val title: String,
    val body: String,
    val file_id: String?
)

data class StandardResponse(
    val status: String,
    val message: String
)

data class uploadResponse(
    val status: String,
    val message: String,
    val file_id: String
)

object RetrofitClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://chat.lamitt.com")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

}