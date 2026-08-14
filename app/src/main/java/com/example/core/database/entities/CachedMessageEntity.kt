package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderUsername: String,
    val senderAvatarUrl: String,
    val content: String,
    val mediaUrl: String = "",
    val mediaType: String = "text",
    val sharedPostId: String = "",
    val sharedPostCaption: String = "",
    val sharedPostMediaUrl: String = "",
    val sharedPostAuthorName: String = "",
    val isRead: Boolean = false,
    val isMine: Boolean = false,
    val status: String = "SENT", // SENDING, SENT, FAILED
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
