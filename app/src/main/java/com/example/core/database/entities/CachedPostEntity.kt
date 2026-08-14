package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val authorUsername: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorIsVerified: Boolean = false,
    val caption: String,
    val mediaUrls: String, // Comma-separated list of URLs
    val mediaType: String, // image, video, reel
    val location: String = "",
    val hashtags: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isMine: Boolean = false,
    val createdAt: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
