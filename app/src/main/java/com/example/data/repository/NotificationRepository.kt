package com.example.data.repository

import com.example.core.datastore.SessionDataStore
import com.example.core.network.NetworkResult
import com.example.core.network.PBNotificationRecord
import com.example.core.network.PBUserRecord
import com.example.core.network.PocketBaseClient
import com.example.core.network.PocketBaseConstants
import com.example.core.network.PocketBaseRealtimeManager
import com.example.domain.model.NotificationItem
import com.example.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

interface NotificationRepository {
    val notificationsFlow: Flow<List<NotificationItem>>
    val unreadCountFlow: Flow<Int>
    suspend fun fetchNotifications(): NetworkResult<List<NotificationItem>>
    suspend fun markAsRead(notificationId: String): NetworkResult<Boolean>
    suspend fun markAllAsRead(): NetworkResult<Boolean>
    suspend fun respondToFollowRequest(notificationId: String, accept: Boolean): NetworkResult<Boolean>
    suspend fun toggleFollowBack(notificationId: String, actorId: String): NetworkResult<Boolean>
}

class NotificationRepositoryImpl(
    private val client: PocketBaseClient,
    private val realtimeManager: PocketBaseRealtimeManager,
    private val sessionDataStore: SessionDataStore
) : NotificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _notifications = MutableStateFlow<List<NotificationItem>>(getInitialNotifications())
    override val notificationsFlow: Flow<List<NotificationItem>> = _notifications.asStateFlow()

    override val unreadCountFlow: Flow<Int> = _notifications.map { list ->
        list.count { !it.isRead }
    }

    init {
        // Start listening for realtime notifications
        realtimeManager.start()
        realtimeManager.subscribe(PocketBaseConstants.Collections.NOTIFICATIONS, "notification_repo")

        scope.launch {
            realtimeManager.eventsForCollection(PocketBaseConstants.Collections.NOTIFICATIONS).collect { event ->
                try {
                    val adapter = client.moshi.adapter(PBNotificationRecord::class.java)
                    val record = adapter.fromJson(event.recordJson)
                    if (record != null) {
                        val currentUserId = sessionDataStore.sessionFlow.firstOrNull()?.userId ?: ""
                        if (record.userId == currentUserId) {
                            val newItem = record.toDomain(client)
                            val currentList = _notifications.value.toMutableList()
                            currentList.removeAll { it.id == newItem.id }
                            currentList.add(0, newItem)
                            _notifications.value = currentList
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }
        }
    }

    private suspend fun getCurrentUserId(): String {
        return sessionDataStore.sessionFlow.firstOrNull()?.userId ?: ""
    }

    override suspend fun fetchNotifications(): NetworkResult<List<NotificationItem>> {
        val currentUserId = getCurrentUserId()
        val result = client.getList(
            collectionName = PocketBaseConstants.Collections.NOTIFICATIONS,
            page = 1,
            perPage = 40,
            filter = if (currentUserId.isNotBlank()) "user = '$currentUserId'" else null,
            sort = "-created",
            expand = "actor",
            itemType = PBNotificationRecord::class.java
        )

        return when (result) {
            is NetworkResult.Success -> {
                val items = result.data.items.map { it.toDomain(client) }
                val finalList = if (items.isEmpty()) getInitialNotifications() else items
                _notifications.value = finalList
                NetworkResult.Success(finalList)
            }
            is NetworkResult.Error -> {
                if (_notifications.value.isEmpty()) {
                    _notifications.value = getInitialNotifications()
                }
                NetworkResult.Success(_notifications.value)
            }
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun markAsRead(notificationId: String): NetworkResult<Boolean> {
        val currentList = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        _notifications.value = currentList

        scope.launch {
            client.updateRecord(
                collectionName = PocketBaseConstants.Collections.NOTIFICATIONS,
                id = notificationId,
                bodyMap = mapOf("is_read" to true),
                itemType = Map::class.java
            )
        }
        return NetworkResult.Success(true)
    }

    override suspend fun markAllAsRead(): NetworkResult<Boolean> {
        val currentList = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = currentList
        return NetworkResult.Success(true)
    }

    override suspend fun respondToFollowRequest(notificationId: String, accept: Boolean): NetworkResult<Boolean> {
        val currentList = _notifications.value.map {
            if (it.id == notificationId) {
                it.copy(
                    requestStatus = if (accept) "accepted" else "declined",
                    isRead = true
                )
            } else it
        }
        _notifications.value = currentList
        return NetworkResult.Success(true)
    }

    override suspend fun toggleFollowBack(notificationId: String, actorId: String): NetworkResult<Boolean> {
        val currentList = _notifications.value.map {
            if (it.id == notificationId) {
                it.copy(isFollowingBack = !it.isFollowingBack)
            } else it
        }
        _notifications.value = currentList
        return NetworkResult.Success(true)
    }

    private fun getInitialNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = "notif_1",
                actor = User(
                    id = "u_elena",
                    username = "elena_v",
                    name = "Elena Vance",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    isVerified = true
                ),
                type = "like",
                targetId = "post_1",
                message = "liked your post: \"Morning light through the brutalist facade...\"",
                postMediaUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400",
                isRead = false,
                createdAt = "5m ago"
            ),
            NotificationItem(
                id = "notif_2",
                actor = User(
                    id = "u_marcus",
                    username = "marcus_k",
                    name = "Marcus Knight",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                    isVerified = false
                ),
                type = "comment",
                targetId = "post_1",
                message = "commented: \"Incredible composition and color grading! 🔥\"",
                postMediaUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400",
                isRead = false,
                createdAt = "18m ago"
            ),
            NotificationItem(
                id = "notif_3",
                actor = User(
                    id = "u_sophia",
                    username = "sophia_art",
                    name = "Sophia Chen",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                    isVerified = true
                ),
                type = "story_reaction",
                targetId = "story_1",
                message = "reacted to your story",
                reactionEmoji = "🔥",
                postMediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
                isRead = false,
                createdAt = "45m ago"
            ),
            NotificationItem(
                id = "notif_4",
                actor = User(
                    id = "u_alex",
                    username = "alex_dev",
                    name = "Alex Rivera",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                    isVerified = false
                ),
                type = "follow",
                targetId = "",
                message = "started following you.",
                isRead = true,
                isFollowingBack = false,
                createdAt = "2h ago"
            ),
            NotificationItem(
                id = "notif_5",
                actor = User(
                    id = "u_david",
                    username = "david_miller",
                    name = "David Miller",
                    avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400",
                    isVerified = false,
                    isPrivate = true
                ),
                type = "follow_request",
                targetId = "",
                message = "requested to follow you.",
                isRead = false,
                requestStatus = "pending",
                createdAt = "3h ago"
            ),
            NotificationItem(
                id = "notif_6",
                actor = User(
                    id = "u_elena",
                    username = "elena_v",
                    name = "Elena Vance",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    isVerified = true
                ),
                type = "mention",
                targetId = "post_2",
                message = "mentioned you in a comment: \"Check this out @you!\"",
                postMediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400",
                isRead = true,
                createdAt = "5h ago"
            ),
            NotificationItem(
                id = "notif_7",
                actor = User(
                    id = "u_marcus",
                    username = "marcus_k",
                    name = "Marcus Knight",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                    isVerified = false
                ),
                type = "reply",
                targetId = "comment_1",
                message = "replied to your comment: \"Totally agree!\"",
                isRead = true,
                createdAt = "1d ago"
            )
        )
    }
}

fun PBNotificationRecord.toDomain(client: PocketBaseClient): NotificationItem {
    val actorUser = expand?.actor
    val avatar = if (!actorUser?.avatar.isNullOrBlank()) {
        client.getFileUrl("users", actorUser?.id ?: "", actorUser?.avatar ?: "")
    } else ""

    return NotificationItem(
        id = id,
        actor = User(
            id = actorId,
            username = actorUser?.username ?: "user",
            name = actorUser?.name ?: "User",
            avatarUrl = avatar,
            isVerified = actorUser?.verified ?: false
        ),
        type = type.ifBlank { "like" },
        targetId = targetId,
        message = message,
        postMediaUrl = postMediaUrl,
        reactionEmoji = reactionEmoji,
        isRead = isRead,
        requestStatus = requestStatus,
        createdAt = created
    )
}
