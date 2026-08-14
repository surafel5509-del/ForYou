package com.example.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatRepository
import com.example.data.repository.SearchRepository
import com.example.domain.model.Conversation
import com.example.domain.model.Message
import com.example.domain.model.Post
import com.example.domain.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ChatUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val activeConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val isOtherUserTyping: Boolean = false,
    val userSearchQuery: String = "",
    val suggestedUsers: List<User> = emptyList(),
    val isUserPickerOpen: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversations = chatRepository.conversationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var typingJob: Job? = null
    private var activeConversationJob: Job? = null
    private var typingObserveJob: Job? = null

    init {
        loadConversations()
        loadSuggestedUsers()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.fetchConversations()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadSuggestedUsers() {
        viewModelScope.launch {
            val users = searchRepository.getSuggestedUsers()
            _uiState.value = _uiState.value.copy(suggestedUsers = users)
        }
    }

    fun openConversation(conversation: Conversation) {
        _uiState.value = _uiState.value.copy(
            activeConversation = conversation,
            messages = emptyList(),
            isOtherUserTyping = false
        )

        // Mark as read
        viewModelScope.launch {
            chatRepository.markConversationRead(conversation.id)
        }

        // Fetch remote messages and collect Room cached messages
        activeConversationJob?.cancel()
        activeConversationJob = viewModelScope.launch {
            // First trigger remote fetch
            chatRepository.fetchMessages(conversation.id)
            // Then listen to Room DB cache for instant reactive updates
            chatRepository.getCachedMessages(conversation.id).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }

        // Listen for typing events from the other participant
        typingObserveJob?.cancel()
        typingObserveJob = viewModelScope.launch {
            chatRepository.observeTyping(conversation.id).collect { isTyping ->
                _uiState.value = _uiState.value.copy(isOtherUserTyping = isTyping)
            }
        }
    }

    fun closeActiveConversation() {
        activeConversationJob?.cancel()
        typingObserveJob?.cancel()
        _uiState.value = _uiState.value.copy(activeConversation = null, messages = emptyList())
        loadConversations()
    }

    fun sendMessage(
        content: String,
        mediaFile: File? = null,
        mediaType: String = "text",
        sharedPost: Post? = null
    ) {
        val conv = _uiState.value.activeConversation ?: return
        if (content.isBlank() && mediaFile == null && sharedPost == null) return

        viewModelScope.launch {
            // Stop typing immediately when sending
            chatRepository.sendTypingIndicator(conv.id, false)
            chatRepository.sendMessage(
                conversationId = conv.id,
                receiverId = conv.otherUser.id,
                content = content,
                mediaFile = mediaFile,
                mediaType = mediaType,
                sharedPost = sharedPost
            )
        }
    }

    fun onUserTyping(text: String) {
        val conv = _uiState.value.activeConversation ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            chatRepository.sendTypingIndicator(conv.id, text.isNotBlank())
            delay(3000)
            chatRepository.sendTypingIndicator(conv.id, false)
        }
    }

    fun retryFailedMessage(message: Message) {
        viewModelScope.launch {
            chatRepository.retryFailedMessage(message)
        }
    }

    fun deleteMessage(messageId: String) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId, conv.id)
        }
    }

    fun openUserPicker(open: Boolean) {
        _uiState.value = _uiState.value.copy(isUserPickerOpen = open, userSearchQuery = "")
    }

    fun onUserSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(userSearchQuery = query)
        viewModelScope.launch {
            val res = searchRepository.search(query, com.example.data.repository.SearchCategory.ACCOUNTS)
            if (res is com.example.core.network.NetworkResult.Success) {
                _uiState.value = _uiState.value.copy(suggestedUsers = res.data.users)
            }
        }
    }

    fun startDirectMessageWithUser(user: User, onComplete: (Conversation) -> Unit) {
        viewModelScope.launch {
            val res = chatRepository.getOrCreateConversation(user)
            if (res is com.example.core.network.NetworkResult.Success) {
                openConversation(res.data)
                _uiState.value = _uiState.value.copy(isUserPickerOpen = false)
                onComplete(res.data)
            }
        }
    }
}
