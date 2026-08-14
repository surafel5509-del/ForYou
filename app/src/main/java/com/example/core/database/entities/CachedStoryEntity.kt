package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_stories")
data class CachedStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val authorUsername: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorIsVerified: Boolean = false,
    val mediaUrl: String,
    val mediaType: String = "image",
    val caption: String = "",
    val viewsCount: Int = 0,
    val isViewed: Boolean = false,
    val createdAt: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)
