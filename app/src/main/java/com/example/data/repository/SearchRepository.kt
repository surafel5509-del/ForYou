package com.example.data.repository

import com.example.core.datastore.SessionDataStore
import com.example.core.network.NetworkResult
import com.example.core.network.PBPostRecord
import com.example.core.network.PBUserRecord
import com.example.core.network.PocketBaseClient
import com.example.core.network.PocketBaseConstants
import com.example.domain.model.Post
import com.example.domain.model.RecentSearchItem
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

enum class SearchCategory {
    ALL, ACCOUNTS, HASHTAGS, POSTS, REELS
}

data class SearchResultData(
    val users: List<User> = emptyList(),
    val hashtags: List<Pair<String, Int>> = emptyList(),
    val posts: List<Post> = emptyList(),
    val reels: List<Post> = emptyList()
)

interface SearchRepository {
    val recentSearchesFlow: Flow<List<RecentSearchItem>>
    suspend fun search(query: String, category: SearchCategory = SearchCategory.ALL): NetworkResult<SearchResultData>
    suspend fun addRecentSearch(query: String, type: String = "query", user: User? = null)
    suspend fun removeRecentSearch(id: String)
    suspend fun clearRecentSearches()
    suspend fun getTrendingHashtags(): List<Pair<String, Int>>
    suspend fun getSuggestedUsers(): List<User>
    suspend fun toggleFollowUser(userId: String, isCurrentlyFollowing: Boolean): NetworkResult<Boolean>
}

class SearchRepositoryImpl(
    private val client: PocketBaseClient,
    private val sessionDataStore: SessionDataStore
) : SearchRepository {

    private val _recentSearches = MutableStateFlow<List<RecentSearchItem>>(getInitialRecentSearches())
    override val recentSearchesFlow: Flow<List<RecentSearchItem>> = _recentSearches.asStateFlow()

    private suspend fun getCurrentUserId(): String {
        return sessionDataStore.sessionFlow.firstOrNull()?.userId ?: ""
    }

    override suspend fun search(query: String, category: SearchCategory): NetworkResult<SearchResultData> {
        val cleanQuery = query.trim().removePrefix("#").removePrefix("@")
        if (cleanQuery.isBlank()) {
            return NetworkResult.Success(
                SearchResultData(
                    users = getSuggestedUsers(),
                    hashtags = getTrendingHashtags()
                )
            )
        }

        val currentUserId = getCurrentUserId()
        var foundUsers = emptyList<User>()
        var foundPosts = emptyList<Post>()
        var foundReels = emptyList<Post>()
        var foundTags = emptyList<Pair<String, Int>>()

        // Search Users
        if (category == SearchCategory.ALL || category == SearchCategory.ACCOUNTS) {
            val userFilter = "username ~ '$cleanQuery' || name ~ '$cleanQuery' || bio ~ '$cleanQuery'"
            val usersRes = client.getList(
                collectionName = PocketBaseConstants.Collections.USERS,
                page = 1,
                perPage = 20,
                filter = userFilter,
                itemType = PBUserRecord::class.java
            )

            if (usersRes is NetworkResult.Success) {
                foundUsers = usersRes.data.items.map { pbUser ->
                    val avatar = if (pbUser.avatar.isNotBlank()) client.getFileUrl("users", pbUser.id, pbUser.avatar) else ""
                    User(
                        id = pbUser.id,
                        username = pbUser.username,
                        name = pbUser.name.ifBlank { pbUser.username },
                        avatarUrl = avatar,
                        bio = pbUser.bio,
                        isVerified = pbUser.verified,
                        followersCount = pbUser.followersCount,
                        followingCount = pbUser.followingCount,
                        postsCount = pbUser.postsCount,
                        isFollowing = false
                    )
                }
            }
            if (foundUsers.isEmpty()) {
                foundUsers = getSuggestedUsers().filter {
                    it.username.contains(cleanQuery, ignoreCase = true) || it.name.contains(cleanQuery, ignoreCase = true)
                }
            }
        }

        // Search Posts & Reels
        if (category == SearchCategory.ALL || category == SearchCategory.POSTS || category == SearchCategory.REELS || category == SearchCategory.HASHTAGS) {
            val postFilter = "caption ~ '$cleanQuery' || location ~ '$cleanQuery' || hashtags ~ '$cleanQuery'"
            val postsRes = client.getList(
                collectionName = PocketBaseConstants.Collections.POSTS,
                page = 1,
                perPage = 30,
                filter = postFilter,
                sort = "-created",
                expand = "user",
                itemType = PBPostRecord::class.java
            )

            if (postsRes is NetworkResult.Success) {
                val allPosts = postsRes.data.items.map { record ->
                    val user = record.expand?.user
                    val avatar = if (!user?.avatar.isNullOrBlank()) client.getFileUrl("users", user?.id ?: "", user?.avatar ?: "") else ""
                    val mediaUrls = record.media.map { client.getFileUrl(PocketBaseConstants.Collections.POSTS, record.id, it) }

                    Post(
                        id = record.id,
                        author = User(
                            id = user?.id ?: record.userId,
                            username = user?.username ?: "creator",
                            name = user?.name ?: "Creator",
                            avatarUrl = avatar,
                            isVerified = user?.verified ?: false
                        ),
                        caption = record.caption,
                        mediaUrls = mediaUrls,
                        mediaType = record.mediaType,
                        location = record.location,
                        hashtags = record.hashtags,
                        likesCount = record.likesCount,
                        commentsCount = record.commentsCount,
                        createdAt = record.created
                    )
                }

                foundPosts = allPosts.filter { it.mediaType != "reel" }
                foundReels = allPosts.filter { it.mediaType == "reel" || it.mediaType == "video" }
            }

            if (foundPosts.isEmpty() && foundReels.isEmpty()) {
                val demoContent = getDemoPostsAndReels()
                foundPosts = demoContent.first.filter { it.caption.contains(cleanQuery, ignoreCase = true) || it.hashtags.any { t -> t.contains(cleanQuery, ignoreCase = true) } }
                foundReels = demoContent.second.filter { it.caption.contains(cleanQuery, ignoreCase = true) || it.hashtags.any { t -> t.contains(cleanQuery, ignoreCase = true) } }
            }
        }

        // Hashtags
        if (category == SearchCategory.ALL || category == SearchCategory.HASHTAGS) {
            val allTags = getTrendingHashtags()
            foundTags = allTags.filter { it.first.contains(cleanQuery, ignoreCase = true) }
            if (foundTags.isEmpty()) {
                foundTags = listOf(cleanQuery to 1250)
            }
        }

        return NetworkResult.Success(
            SearchResultData(
                users = foundUsers,
                hashtags = foundTags,
                posts = foundPosts,
                reels = foundReels
            )
        )
    }

    override suspend fun addRecentSearch(query: String, type: String, user: User?) {
        val currentList = _recentSearches.value.toMutableList()
        currentList.removeAll { it.query.equals(query, ignoreCase = true) }
        val item = RecentSearchItem(
            id = UUID.randomUUID().toString(),
            query = query,
            type = type,
            timestamp = System.currentTimeMillis(),
            user = user
        )
        currentList.add(0, item)
        _recentSearches.value = currentList.take(15)
    }

    override suspend fun removeRecentSearch(id: String) {
        _recentSearches.value = _recentSearches.value.filter { it.id != id && it.query != id }
    }

    override suspend fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    override suspend fun getTrendingHashtags(): List<Pair<String, Int>> {
        return listOf(
            "photography" to 142800,
            "architecture" to 98300,
            "travelgoals" to 85200,
            "cyberpunk" to 73400,
            "digitalart" to 61900,
            "fitnessmotivation" to 54300,
            "streetfood" to 42100,
            "musiclovers" to 38900
        )
    }

    override suspend fun getSuggestedUsers(): List<User> {
        return listOf(
            User(
                id = "u_elena",
                username = "elena_v",
                name = "Elena Vance",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                bio = "Visual artist & traveler 📸 Capturing moments around the globe ✨",
                followersCount = 48200,
                isVerified = true
            ),
            User(
                id = "u_marcus",
                username = "marcus_k",
                name = "Marcus Knight",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                bio = "Street & Architecture Photographer 🏛️ Tokyo | London | NYC",
                followersCount = 32100,
                isVerified = false
            ),
            User(
                id = "u_sophia",
                username = "sophia_art",
                name = "Sophia Chen",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400",
                bio = "3D motion designer & digital illustrator 🎨",
                followersCount = 65400,
                isVerified = true
            ),
            User(
                id = "u_alex",
                username = "alex_dev",
                name = "Alex Rivera",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400",
                bio = "Building the future of mobile interfaces ⚡ Android & Compose",
                followersCount = 19800,
                isVerified = false
            )
        )
    }

    override suspend fun toggleFollowUser(userId: String, isCurrentlyFollowing: Boolean): NetworkResult<Boolean> {
        val currentUserId = getCurrentUserId()
        if (isCurrentlyFollowing) {
            // Unfollow
            return NetworkResult.Success(false)
        } else {
            // Follow
            client.createRecord(
                collectionName = PocketBaseConstants.Collections.FOLLOWS,
                bodyMap = mapOf("follower" to currentUserId, "following" to userId),
                itemType = Map::class.java
            )
            return NetworkResult.Success(true)
        }
    }

    private fun getInitialRecentSearches(): List<RecentSearchItem> {
        return listOf(
            RecentSearchItem(
                id = "rs_1",
                query = "elena_v",
                type = "user",
                user = User(
                    id = "u_elena",
                    username = "elena_v",
                    name = "Elena Vance",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                    isVerified = true
                )
            ),
            RecentSearchItem(
                id = "rs_2",
                query = "photography",
                type = "hashtag"
            ),
            RecentSearchItem(
                id = "rs_3",
                query = "cyberpunk",
                type = "hashtag"
            )
        )
    }

    private fun getDemoPostsAndReels(): Pair<List<Post>, List<Post>> {
        val posts = listOf(
            Post(
                id = "p_arch_1",
                author = User(name = "Elena Vance", username = "elena_v", avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"),
                caption = "Morning light through the brutalist facade 🏛️ #architecture #photography",
                mediaUrls = listOf("https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800"),
                hashtags = listOf("architecture", "photography"),
                likesCount = 1420,
                commentsCount = 84
            ),
            Post(
                id = "p_nature_2",
                author = User(name = "Marcus Knight", username = "marcus_k", avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400"),
                caption = "Calm waves on the southern coast 🌊 #travelgoals #ocean",
                mediaUrls = listOf("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
                hashtags = listOf("travelgoals", "ocean"),
                likesCount = 2890,
                commentsCount = 135
            )
        )

        val reels = listOf(
            Post(
                id = "r_tokyo",
                author = User(name = "Sophia Chen", username = "sophia_art", avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400"),
                caption = "Neon reflections in Shinjuku rain ☔ #cyberpunk #tokyo",
                mediaUrls = listOf("https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"),
                mediaType = "reel",
                hashtags = listOf("cyberpunk", "tokyo"),
                likesCount = 5420,
                commentsCount = 210
            )
        )

        return posts to reels
    }
}
