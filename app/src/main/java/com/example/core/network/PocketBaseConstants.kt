package com.example.core.network

import com.example.BuildConfig

object PocketBaseConstants {
    const val DEFAULT_BASE_URL = "https://order-teacher.pockethost.io/"
    val BASE_URL: String = try {
        BuildConfig.POCKETBASE_BASE_URL.ifBlank { DEFAULT_BASE_URL }
    } catch (e: Throwable) {
        DEFAULT_BASE_URL
    }

    object Collections {
        const val USERS = "users"
        const val POSTS = "posts"
        const val POST_MEDIA = "post_media"
        const val COMMENTS = "comments"
        const val LIKES = "likes"
        const val SAVED_POSTS = "saved_posts"
        const val FOLLOWS = "follows"
        const val STORIES = "stories"
        const val STORY_VIEWS = "story_views"
        const val STORY_REACTIONS = "story_reactions"
        const val NOTIFICATIONS = "notifications"
        const val CONVERSATIONS = "conversations"
        const val CONVERSATION_MEMBERS = "conversation_members"
        const val MESSAGES = "messages"
        const val BLOCKS = "blocks"
        const val MUTES = "mutes"
        const val REPORTS = "reports"
    }

    object Endpoints {
        fun collection(name: String) = "api/collections/$name/records"
        fun record(collectionName: String, id: String) = "api/collections/$collectionName/records/$id"
        fun file(collectionName: String, recordId: String, fileName: String) = "api/files/$collectionName/$recordId/$fileName"
        const val AUTH_WITH_PASSWORD = "api/collections/users/auth-with-password"
        const val AUTH_REFRESH = "api/collections/users/auth-refresh"
        const val REQUEST_VERIFICATION = "api/collections/users/request-verification"
        const val CONFIRM_VERIFICATION = "api/collections/users/confirm-verification"
        const val REQUEST_PASSWORD_RESET = "api/collections/users/request-password-reset"
        const val CONFIRM_PASSWORD_RESET = "api/collections/users/confirm-password-reset"
        const val REALTIME = "api/realtime"
    }
}
