import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.NetworkRepository
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

    fun fetchTabData(tabId: Int) {
        _selectedTab.intValue = tabId
        viewModelScope.launch {
            _isLoading.value = true
            val data = networkRepository.fetchDataForTab(tabId)
            _content.value = data
            _isLoading.value = false
        }
    }
}