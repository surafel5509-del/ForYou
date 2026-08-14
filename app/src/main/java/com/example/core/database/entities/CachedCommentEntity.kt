package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_comments")
data class CachedCommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val authorUsername: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorIsVerified: Boolean = false,
    val content: String,
    val parentCommentId: String = "",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
