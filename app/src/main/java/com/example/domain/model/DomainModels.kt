package com.example.domain.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowing: Boolean = false
)

data class Post(
    val id: String = "",
    val author: User = User(),
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaType: String = "image", // image, video, reel
    val location: String = "",
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isMine: Boolean = false,
    val createdAt: String = ""
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val author: User = User(),
    val content: String = "",
    val parentCommentId: String = "",
    val replies: List<Comment> = emptyList(),
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isMine: Boolean = false,
    val createdAt: String = ""
)

data class Story(
    val id: String = "",
    val author: User = User(),
    val mediaUrl: String = "",
    val mediaType: String = "image",
    val caption: String = "",
    val viewsCount: Int = 0,
    val isViewed: Boolean = false,
    val isMine: Boolean = false,
    val createdAt: String = ""
)

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val sender: User = User(),
    val content: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "text", // "text", "image", "video", "shared_post", "shared_reel"
    val sharedPostId: String = "",
    val sharedPostCaption: String = "",
    val sharedPostMediaUrl: String = "",
    val sharedPostAuthorName: String = "",
    val isRead: Boolean = false,
    val isMine: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT, // SENDING, SENT, FAILED
    val createdAt: String = ""
)

enum class MessageStatus {
    SENDING, SENT, FAILED
}

data class Conversation(
    val id: String = "",
    val otherUser: User = User(),
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: String = "Active recently",
    val isTyping: Boolean = false
)

data class NotificationItem(
    val id: String = "",
    val actor: User = User(),
    val type: String = "like", // like, comment, reply, follow, follow_request, mention, message, story_reaction
    val targetId: String = "",
    val message: String = "",
    val postMediaUrl: String = "",
    val reactionEmoji: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",
    val isFollowingBack: Boolean = false,
    val requestStatus: String = "pending" // pending, accepted, declined
)

data class RecentSearchItem(
    val id: String = "",
    val query: String = "",
    val type: String = "query", // query, user, hashtag
    val timestamp: Long = System.currentTimeMillis(),
    val user: User? = null
)
