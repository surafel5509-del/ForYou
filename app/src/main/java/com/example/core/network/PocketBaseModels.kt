package com.example.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PBAuthResponse(
    @Json(name = "token") val token: String = "",
    @Json(name = "record") val record: PBUserRecord = PBUserRecord()
)

@JsonClass(generateAdapter = true)
data class PBListResponse<T>(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "perPage") val perPage: Int = 30,
    @Json(name = "totalItems") val totalItems: Int = 0,
    @Json(name = "totalPages") val totalPages: Int = 0,
    @Json(name = "items") val items: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PBUserRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "username") val username: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "avatar") val avatar: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "is_private") val isPrivate: Boolean = false,
    @Json(name = "followers_count") val followersCount: Int = 0,
    @Json(name = "following_count") val followingCount: Int = 0,
    @Json(name = "posts_count") val postsCount: Int = 0,
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = ""
)

@JsonClass(generateAdapter = true)
data class PBPostRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "user") val userId: String = "",
    @Json(name = "caption") val caption: String = "",
    @Json(name = "media") val media: List<String> = emptyList(),
    @Json(name = "media_type") val mediaType: String = "image", // image, video, reel
    @Json(name = "location") val location: String = "",
    @Json(name = "hashtags") val hashtags: List<String> = emptyList(),
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBPostExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBPostExpand(
    @Json(name = "user") val user: PBUserRecord? = null
)

@JsonClass(generateAdapter = true)
data class PBCommentRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "post") val postId: String = "",
    @Json(name = "user") val userId: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "parent_comment") val parentCommentId: String = "",
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBCommentExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBCommentExpand(
    @Json(name = "user") val user: PBUserRecord? = null
)

@JsonClass(generateAdapter = true)
data class PBStoryRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "user") val userId: String = "",
    @Json(name = "media") val media: String = "",
    @Json(name = "media_type") val mediaType: String = "image",
    @Json(name = "caption") val caption: String = "",
    @Json(name = "views_count") val viewsCount: Int = 0,
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBStoryExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBStoryExpand(
    @Json(name = "user") val user: PBUserRecord? = null
)

@JsonClass(generateAdapter = true)
data class PBMessageRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "conversation") val conversationId: String = "",
    @Json(name = "sender") val senderId: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "media") val media: String = "",
    @Json(name = "media_type") val mediaType: String = "text",
    @Json(name = "shared_post_id") val sharedPostId: String = "",
    @Json(name = "shared_post_caption") val sharedPostCaption: String = "",
    @Json(name = "shared_post_media_url") val sharedPostMediaUrl: String = "",
    @Json(name = "shared_post_author_name") val sharedPostAuthorName: String = "",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBMessageExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBMessageExpand(
    @Json(name = "sender") val sender: PBUserRecord? = null
)

@JsonClass(generateAdapter = true)
data class PBConversationRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "participants") val participants: List<String> = emptyList(),
    @Json(name = "last_message") val lastMessage: String = "",
    @Json(name = "last_message_time") val lastMessageTime: String = "",
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBConversationExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBConversationExpand(
    @Json(name = "participants") val participantsList: List<PBUserRecord>? = null
)

@JsonClass(generateAdapter = true)
data class PBNotificationRecord(
    @Json(name = "id") val id: String = "",
    @Json(name = "user") val userId: String = "",
    @Json(name = "actor") val actorId: String = "",
    @Json(name = "type") val type: String = "", // like, comment, reply, follow, follow_request, mention, message, story_reaction
    @Json(name = "target_id") val targetId: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "post_media_url") val postMediaUrl: String = "",
    @Json(name = "reaction_emoji") val reactionEmoji: String = "",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "request_status") val requestStatus: String = "pending",
    @Json(name = "created") val created: String = "",
    @Json(name = "updated") val updated: String = "",
    @Json(name = "expand") val expand: PBNotificationExpand? = null
)

@JsonClass(generateAdapter = true)
data class PBNotificationExpand(
    @Json(name = "actor") val actor: PBUserRecord? = null
)

@JsonClass(generateAdapter = true)
data class PBErrorResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "message") val message: String = "",
    @Json(name = "data") val data: Map<String, Any>? = null
)

data class RealtimeEvent(
    val action: String, // "create", "update", "delete"
    val record: String, // raw json or record type
    val collection: String
)
