import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.NetworkRepository
import com.example.iems5725_Classroom.network.AllCoursesResponse
import com.example.iems5725_Classroom.network.CourseInfoResponse
import com.example.iems5725_Classroom.network.RetrofitClient
import com.example.iems5725_Classroom.network.UserChatsResponse
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class MainViewModel(private val networkRepository: NetworkRepository) : ViewModel() {

    private val api = RetrofitClient.apiService

    private val _chats = MutableLiveData<UserChatsResponse>()
    val chats: LiveData<UserChatsResponse> get() = _chats

    private val _content = MutableLiveData<AllCoursesResponse>()
    val content: LiveData<AllCoursesResponse> get() = _content

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _selectedTab = mutableIntStateOf(0)
    val selectedTab: State<Int> = _selectedTab

    private val _tabName = mutableStateOf("Courses")
    val tabName: State<String> = _tabName

    fun fetchCourseData(tabId: Int, tabName: String) {
        _tabName.value = tabName
        viewModelScope.launch {
            delay(2000)
            _isLoading.value = true
            val response = api.getCourses()
            _content.postValue(response)
            Log.d(TAG, "Fetched date: ${response}")
            _isLoading.value = false
            _selectedTab.intValue = tabId
            Log.d(TAG, "Table id: ${tabId}")
        }
    }
    fun fetchChatsData(tabId: Int, tabName: String, userName: String){
        _tabName.value = tabName
        viewModelScope.launch {
            _isLoading.value = true
            val response = api.getChats(userName)
            _chats.postValue(response)
            Log.d(TAG, "Fetched date: ${response}")
            _isLoading.value = false
            _selectedTab.intValue = tabId
            Log.d(TAG, "Table id: ${tabId}")
        }
    }

    private val _courseInfo = MutableLiveData<CourseInfoResponse>()
    val courseInfo: LiveData<CourseInfoResponse> get() = _courseInfo

    private val _isLoading2 = mutableStateOf(true)
    val isLoading2: State<Boolean> = _isLoading2

//    private val _tabName2 = mutableStateOf("Courses")
//    val tabName2: State<String> = _tabName2

    fun fetchTabCourseData(courseCode: String, section: String) {
        viewModelScope.launch {
            _isLoading2.value = true
            val response = api.getCourseInfo(courseCode, section)
            _courseInfo.postValue(response)
            Log.d(TAG, "Fetched date: ${response}")
            _isLoading2.value = false
        }
    }

    private val _isCreated = mutableStateOf(true)
    val isCreated: State<Boolean> = _isCreated

    private val _createChatResponse = mutableStateOf(buildJsonObject { })
    val createChatResponse: MutableState<JsonObject> = _createChatResponse

    fun postForCreateChat(request: Any){
        viewModelScope.launch {
            _isCreated.value = false
            val data = networkRepository.postForCreateChat(request)
            Log.d(TAG, "Response data: ${data}")
            _createChatResponse.value = data
            _isCreated.value = true
        }
    }


}