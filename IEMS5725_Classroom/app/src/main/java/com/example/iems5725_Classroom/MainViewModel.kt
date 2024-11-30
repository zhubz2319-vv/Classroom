import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.MessageItem
import com.example.iems5725_Classroom.NetworkRepository
import com.example.iems5725_Classroom.WebSocketManager
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

    private val _isLoadingMessage = mutableStateOf(false)
    val isLoadingMessage: State<Boolean> = _isLoadingMessage

    private val _message = mutableStateOf(buildJsonObject { })
    val message: MutableState<JsonObject> = _message

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
    fun fetchMessage(roomCode: String){
        viewModelScope.launch {
            _isLoadingMessage.value = true
            val data = networkRepository.fetchMessageByRoomCode(roomCode)
            Log.d(TAG, "Fetched date: ${data}")
            _message.value = data
            _isLoadingMessage.value = false
        }
    }
    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val webSocketManager = WebSocketManager("room1")

    init {
        // 连接到 WebSocket
        webSocketManager.connect()

        // 设置 WebSocket 回调，接收到新消息时更新 LiveData
        webSocketManager.onMessageReceived = { message ->
            val currentMessages = _messages.value.orEmpty().toMutableList()
            currentMessages.add(message)  // 将新消息添加到现有的消息列表
            _messages.value = currentMessages  // 更新 LiveData
        }
    }

    fun sendMessage(sender: String, message: String) {
        webSocketManager.sendMessage(sender, message)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.close()
    }
}