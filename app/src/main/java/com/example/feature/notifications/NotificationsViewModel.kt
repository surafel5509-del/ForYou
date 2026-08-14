package com.example.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NotificationRepository
import com.example.domain.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NotificationFilter {
    ALL, LIKES, COMMENTS, MENTIONS, FOLLOWS
}

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    private val _isLoading = MutableStateFlow(false)

    val unreadCount: StateFlow<Int> = notificationRepository.unreadCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val uiState: StateFlow<NotificationsUiState> = combine(
        _isLoading,
        _selectedFilter,
        notificationRepository.notificationsFlow,
        notificationRepository.unreadCountFlow
    ) { isLoading, filter, allNotifications, unreadCount ->
        val filtered = when (filter) {
            NotificationFilter.ALL -> allNotifications
            NotificationFilter.LIKES -> allNotifications.filter { it.type == "like" || it.type == "story_reaction" }
            NotificationFilter.COMMENTS -> allNotifications.filter { it.type == "comment" || it.type == "reply" }
            NotificationFilter.MENTIONS -> allNotifications.filter { it.type == "mention" }
            NotificationFilter.FOLLOWS -> allNotifications.filter { it.type == "follow" || it.type == "follow_request" }
        }

        NotificationsUiState(
            isLoading = isLoading,
            selectedFilter = filter,
            notifications = filtered,
            unreadCount = unreadCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState()
    )

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepository.fetchNotifications()
            _isLoading.value = false
        }
    }

    fun onFilterSelect(filter: NotificationFilter) {
        _selectedFilter.value = filter
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun respondToFollowRequest(notificationId: String, accept: Boolean) {
        viewModelScope.launch {
            notificationRepository.respondToFollowRequest(notificationId, accept)
        }
    }

    fun toggleFollowBack(notificationId: String, actorId: String) {
        viewModelScope.launch {
            notificationRepository.toggleFollowBack(notificationId, actorId)
        }
    }
}
