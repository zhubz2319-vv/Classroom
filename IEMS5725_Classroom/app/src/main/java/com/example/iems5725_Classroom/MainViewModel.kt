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


    fun fetchTabData(tabId: Int, tabName: String) {
        _tabName.value = tabName
        viewModelScope.launch {
            _isLoading.value = true
            val data = networkRepository.fetchDataForTab(tabId)
            Log.d(TAG, "Fetched date: ${data}")
            _content.value = data
            _isLoading.value = false
            _selectedTab.intValue = tabId
            Log.d(TAG, "Table id: ${tabId}")
        }
    }
}