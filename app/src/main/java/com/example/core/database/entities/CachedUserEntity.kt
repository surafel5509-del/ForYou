package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_users")
data class CachedUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val isVerified: Boolean,
    val isPrivate: Boolean,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val isFollowing: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)
