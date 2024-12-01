import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.NetworkRepository
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class MainViewModel(private val networkRepository: NetworkRepository) : ViewModel() {

    private val _content = mutableStateOf(buildJsonObject { })
    val content: MutableState<JsonObject> = _content

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _selectedTab = mutableIntStateOf(0)
    val selectedTab: State<Int> = _selectedTab

    private val _tabName = mutableStateOf("Courses")
    val tabName: State<String> = _tabName

    fun fetchTabData(tabId: Int, tabName: String, userName: String) {
        _tabName.value = tabName
        viewModelScope.launch {
            _isLoading.value = true
            val data = networkRepository.fetchDataForTab(tabId, userName)
            Log.d(TAG, "Fetched date: ${data}")
            _content.value = data
            _isLoading.value = false
            _selectedTab.intValue = tabId
            Log.d(TAG, "Table id: ${tabId}")
        }
    }

    private val _courseData = mutableStateOf(buildJsonObject { })
    val courseData: MutableState<JsonObject> = _courseData

    private val _isLoading2 = mutableStateOf(false)
    val isLoading2: State<Boolean> = _isLoading2

//    private val _tabName2 = mutableStateOf("Courses")
//    val tabName2: State<String> = _tabName2

    fun fetchTabCourseData(cCode: String, sec: String) {
        viewModelScope.launch {
            _isLoading2.value = true
            val data = networkRepository.fetchDataForTab(cCode, sec)
            Log.d(TAG, "Fetched date: ${data}")
            _courseData.value = data
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