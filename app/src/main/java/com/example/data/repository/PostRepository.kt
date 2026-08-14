package com.example.data.repository

import com.example.core.database.dao.CommentDao
import com.example.core.database.dao.PostDao
import com.example.core.database.dao.StoryDao
import com.example.core.database.entities.CachedCommentEntity
import com.example.core.database.entities.CachedPostEntity
import com.example.core.database.entities.CachedStoryEntity
import com.example.core.datastore.SessionDataStore
import com.example.core.network.NetworkResult
import com.example.core.network.PBCommentRecord
import com.example.core.network.PBPostRecord
import com.example.core.network.PBStoryRecord
import com.example.core.network.PocketBaseClient
import com.example.core.network.PocketBaseConstants
import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.Story
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File

interface PostRepository {
    val cachedFeed: Flow<List<Post>>
    val cachedStories: Flow<List<Story>>

    suspend fun getFeed(page: Int = 1, perPage: Int = 15): NetworkResult<List<Post>>
    suspend fun getStories(): NetworkResult<List<Story>>
    suspend fun createPost(
        caption: String,
        mediaFiles: List<File>,
        mediaType: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post>
    suspend fun updatePost(
        postId: String,
        caption: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post>
    suspend fun deletePost(postId: String): NetworkResult<Boolean>
    suspend fun toggleLike(postId: String, currentLikes: Int, isCurrentlyLiked: Boolean): NetworkResult<Boolean>
    suspend fun toggleSave(postId: String, isCurrentlySaved: Boolean): NetworkResult<Boolean>
    suspend fun getComments(postId: String): Flow<List<Comment>>
    suspend fun fetchComments(postId: String): NetworkResult<List<Comment>>
    suspend fun addComment(postId: String, content: String, parentCommentId: String = ""): NetworkResult<Comment>
    suspend fun toggleLikeComment(commentId: String, currentLikes: Int, isCurrentlyLiked: Boolean): NetworkResult<Boolean>
    suspend fun deleteComment(commentId: String, postId: String): NetworkResult<Boolean>
    suspend fun createStory(mediaFile: File?, caption: String, mediaType: String): NetworkResult<Story>
    suspend fun markStoryViewed(storyId: String): NetworkResult<Boolean>
    suspend fun getReels(page: Int = 1): NetworkResult<List<Post>>
}

class PostRepositoryImpl(
    private val client: PocketBaseClient,
    private val postDao: PostDao,
    private val storyDao: StoryDao? = null,
    private val commentDao: CommentDao? = null,
    private val sessionDataStore: SessionDataStore? = null
) : PostRepository {

    override val cachedFeed: Flow<List<Post>> = postDao.getAllPosts().map { entities ->
        entities.map { it.toDomain() }
    }

    override val cachedStories: Flow<List<Story>> = (storyDao?.getAllStories() ?: postDao.getAllPosts().map { emptyList() }).map { entities ->
        entities.map { it.toDomain() }
    }

    private suspend fun getCurrentUserId(): String {
        return sessionDataStore?.sessionFlow?.firstOrNull()?.userId ?: ""
    }

    override suspend fun getFeed(page: Int, perPage: Int): NetworkResult<List<Post>> {
        val currentUserId = getCurrentUserId()
        val result = client.getList(
            collectionName = PocketBaseConstants.Collections.POSTS,
            page = page,
            perPage = perPage,
            sort = "-created",
            expand = "user",
            itemType = PBPostRecord::class.java
        )

        return when (result) {
            is NetworkResult.Success -> {
                val posts = result.data.items.map { record ->
                    val user = record.expand?.user
                    val avatarUrl = if (!user?.avatar.isNullOrBlank()) {
                        client.getFileUrl("users", user?.id ?: "", user?.avatar ?: "")
                    } else ""

                    val mediaUrls = record.media.map { fileName ->
                        client.getFileUrl(PocketBaseConstants.Collections.POSTS, record.id, fileName)
                    }

                    val mentions = Regex("@([a-zA-Z0-9_]+)")
                        .findAll(record.caption)
                        .map { it.groupValues[1] }
                        .toList()

                    Post(
                        id = record.id,
                        author = User(
                            id = user?.id ?: record.userId,
                            username = user?.username ?: "creator",
                            name = user?.name ?: user?.username ?: "Creator",
                            avatarUrl = avatarUrl,
                            isVerified = user?.verified ?: false
                        ),
                        caption = record.caption,
                        mediaUrls = mediaUrls,
                        mediaType = record.mediaType,
                        location = record.location,
                        hashtags = record.hashtags,
                        mentions = mentions,
                        likesCount = record.likesCount,
                        commentsCount = record.commentsCount,
                        isMine = (user?.id ?: record.userId) == currentUserId,
                        createdAt = record.created
                    )
                }

                // Cache in Room database
                if (page == 1) {
                    val entities = posts.map { post -> post.toEntity() }
                    postDao.insertPosts(entities)
                } else {
                    val entities = posts.map { post -> post.toEntity() }
                    postDao.insertPosts(entities)
                }

                NetworkResult.Success(posts)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getStories(): NetworkResult<List<Story>> {
        val currentUserId = getCurrentUserId()
        val result = client.getList(
            collectionName = PocketBaseConstants.Collections.STORIES,
            page = 1,
            perPage = 30,
            sort = "-created",
            expand = "user",
            itemType = PBStoryRecord::class.java
        )

        return when (result) {
            is NetworkResult.Success -> {
                val stories = result.data.items.map { record ->
                    val user = record.expand?.user
                    val avatarUrl = if (!user?.avatar.isNullOrBlank()) {
                        client.getFileUrl("users", user?.id ?: "", user?.avatar ?: "")
                    } else ""

                    val mediaUrl = if (record.media.isNotBlank()) {
                        client.getFileUrl(PocketBaseConstants.Collections.STORIES, record.id, record.media)
                    } else ""

                    Story(
                        id = record.id,
                        author = User(
                            id = user?.id ?: record.userId,
                            username = user?.username ?: "creator",
                            name = user?.name ?: "Creator",
                            avatarUrl = avatarUrl,
                            isVerified = user?.verified ?: false
                        ),
                        mediaUrl = mediaUrl,
                        mediaType = record.mediaType,
                        caption = record.caption,
                        viewsCount = record.viewsCount,
                        isMine = (user?.id ?: record.userId) == currentUserId,
                        createdAt = record.created
                    )
                }
                storyDao?.insertStories(stories.map { it.toEntity() })
                NetworkResult.Success(stories)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun createPost(
        caption: String,
        mediaFiles: List<File>,
        mediaType: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post> {
        val currentUserId = getCurrentUserId()
        val existingFiles = mediaFiles.filter { it.exists() }

        val res = if (existingFiles.isNotEmpty()) {
            client.uploadFiles(
                collectionName = PocketBaseConstants.Collections.POSTS,
                recordId = null,
                fileField = "media",
                files = existingFiles,
                extraFields = mapOf(
                    "caption" to caption,
                    "media_type" to mediaType,
                    "location" to location,
                    "user" to currentUserId
                ),
                itemType = PBPostRecord::class.java
            )
        } else {
            val bodyMap = mapOf(
                "caption" to caption,
                "media_type" to mediaType,
                "location" to location,
                "hashtags" to hashtags,
                "user" to currentUserId
            )
            client.createRecord(
                collectionName = PocketBaseConstants.Collections.POSTS,
                bodyMap = bodyMap,
                itemType = PBPostRecord::class.java
            )
        }

        return when (res) {
            is NetworkResult.Success -> {
                val record = res.data
                val post = Post(
                    id = record.id,
                    author = User(id = currentUserId),
                    caption = record.caption,
                    mediaType = record.mediaType,
                    location = record.location,
                    hashtags = hashtags,
                    isMine = true,
                    createdAt = record.created
                )
                postDao.insertPost(post.toEntity())
                getFeed(1, 20)
                NetworkResult.Success(post)
            }
            is NetworkResult.Error -> res
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun updatePost(
        postId: String,
        caption: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post> {
        // Optimistic update in Room
        postDao.updatePostContent(
            postId = postId,
            caption = caption,
            location = location,
            hashtags = hashtags.joinToString(",")
        )

        val res = client.updateRecord(
            collectionName = PocketBaseConstants.Collections.POSTS,
            id = postId,
            bodyMap = mapOf(
                "caption" to caption,
                "location" to location,
                "hashtags" to hashtags
            ),
            itemType = PBPostRecord::class.java
        )

        return when (res) {
            is NetworkResult.Success -> {
                val record = res.data
                NetworkResult.Success(
                    Post(
                        id = record.id,
                        caption = record.caption,
                        location = record.location,
                        hashtags = record.hashtags,
                        createdAt = record.created
                    )
                )
            }
            is NetworkResult.Error -> res
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun deletePost(postId: String): NetworkResult<Boolean> {
        postDao.deletePostById(postId)
        commentDao?.clearCommentsForPost(postId)
        return client.deleteRecord(
            collectionName = PocketBaseConstants.Collections.POSTS,
            id = postId
        )
    }

    override suspend fun toggleLike(
        postId: String,
        currentLikes: Int,
        isCurrentlyLiked: Boolean
    ): NetworkResult<Boolean> {
        val newLiked = !isCurrentlyLiked
        val newLikesCount = if (newLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)

        // Optimistic UI update in Room
        postDao.updateLikeStatus(postId, newLiked, newLikesCount)

        val updateResult = client.updateRecord(
            collectionName = PocketBaseConstants.Collections.POSTS,
            id = postId,
            bodyMap = mapOf("likes_count" to newLikesCount),
            itemType = PBPostRecord::class.java
        )

        return when (updateResult) {
            is NetworkResult.Success -> NetworkResult.Success(newLiked)
            is NetworkResult.Error -> {
                postDao.updateLikeStatus(postId, isCurrentlyLiked, currentLikes)
                updateResult
            }
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun toggleSave(postId: String, isCurrentlySaved: Boolean): NetworkResult<Boolean> {
        val newSaved = !isCurrentlySaved
        postDao.updateSavedStatus(postId, newSaved)
        return NetworkResult.Success(newSaved)
    }

    override suspend fun getComments(postId: String): Flow<List<Comment>> {
        return (commentDao?.getCommentsForPost(postId) ?: postDao.getAllPosts().map { emptyList() }).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun fetchComments(postId: String): NetworkResult<List<Comment>> {
        val currentUserId = getCurrentUserId()
        val result = client.getList(
            collectionName = PocketBaseConstants.Collections.COMMENTS,
            page = 1,
            perPage = 50,
            filter = "post = '$postId'",
            sort = "created",
            expand = "user",
            itemType = PBCommentRecord::class.java
        )

        return when (result) {
            is NetworkResult.Success -> {
                val comments = result.data.items.map { record ->
                    val user = record.expand?.user
                    val avatarUrl = if (!user?.avatar.isNullOrBlank()) {
                        client.getFileUrl("users", user?.id ?: "", user?.avatar ?: "")
                    } else ""

                    Comment(
                        id = record.id,
                        postId = record.postId,
                        author = User(
                            id = user?.id ?: record.userId,
                            username = user?.username ?: "creator",
                            name = user?.name ?: user?.username ?: "Creator",
                            avatarUrl = avatarUrl,
                            isVerified = user?.verified ?: false
                        ),
                        content = record.content,
                        parentCommentId = record.parentCommentId,
                        likesCount = record.likesCount,
                        isMine = (user?.id ?: record.userId) == currentUserId,
                        createdAt = record.created
                    )
                }

                commentDao?.insertComments(comments.map { it.toEntity() })
                NetworkResult.Success(comments)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun addComment(
        postId: String,
        content: String,
        parentCommentId: String
    ): NetworkResult<Comment> {
        val currentUserId = getCurrentUserId()
        val bodyMap = mutableMapOf<String, Any?>(
            "post" to postId,
            "user" to currentUserId,
            "content" to content
        )
        if (parentCommentId.isNotBlank()) {
            bodyMap["parent_comment"] = parentCommentId
        }

        val res = client.createRecord(
            collectionName = PocketBaseConstants.Collections.COMMENTS,
            bodyMap = bodyMap,
            itemType = PBCommentRecord::class.java
        )

        return when (res) {
            is NetworkResult.Success -> {
                val record = res.data
                val comment = Comment(
                    id = record.id,
                    postId = postId,
                    author = User(id = currentUserId, username = "me"),
                    content = record.content,
                    parentCommentId = record.parentCommentId,
                    likesCount = 0,
                    isMine = true,
                    createdAt = record.created
                )
                commentDao?.insertComment(comment.toEntity())
                postDao.incrementCommentCount(postId)
                NetworkResult.Success(comment)
            }
            is NetworkResult.Error -> res
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun toggleLikeComment(
        commentId: String,
        currentLikes: Int,
        isCurrentlyLiked: Boolean
    ): NetworkResult<Boolean> {
        val newLiked = !isCurrentlyLiked
        val newCount = if (newLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
        commentDao?.updateCommentLike(commentId, newLiked, newCount)

        client.updateRecord(
            collectionName = PocketBaseConstants.Collections.COMMENTS,
            id = commentId,
            bodyMap = mapOf("likes_count" to newCount),
            itemType = PBCommentRecord::class.java
        )
        return NetworkResult.Success(newLiked)
    }

    override suspend fun deleteComment(commentId: String, postId: String): NetworkResult<Boolean> {
        commentDao?.deleteComment(commentId)
        postDao.decrementCommentCount(postId)
        return client.deleteRecord(
            collectionName = PocketBaseConstants.Collections.COMMENTS,
            id = commentId
        )
    }

    override suspend fun createStory(
        mediaFile: File?,
        caption: String,
        mediaType: String
    ): NetworkResult<Story> {
        val currentUserId = getCurrentUserId()
        val res = if (mediaFile != null && mediaFile.exists()) {
            client.uploadFile(
                collectionName = PocketBaseConstants.Collections.STORIES,
                recordId = null,
                fileField = "media",
                file = mediaFile,
                extraFields = mapOf(
                    "caption" to caption,
                    "media_type" to mediaType,
                    "user" to currentUserId
                ),
                itemType = PBStoryRecord::class.java
            )
        } else {
            client.createRecord(
                collectionName = PocketBaseConstants.Collections.STORIES,
                bodyMap = mapOf(
                    "caption" to caption,
                    "media_type" to mediaType,
                    "user" to currentUserId
                ),
                itemType = PBStoryRecord::class.java
            )
        }

        return when (res) {
            is NetworkResult.Success -> {
                val record = res.data
                val story = Story(
                    id = record.id,
                    author = User(id = currentUserId),
                    caption = record.caption,
                    mediaType = record.mediaType,
                    isMine = true,
                    createdAt = record.created
                )
                storyDao?.insertStory(story.toEntity())
                getStories()
                NetworkResult.Success(story)
            }
            is NetworkResult.Error -> res
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun markStoryViewed(storyId: String): NetworkResult<Boolean> {
        storyDao?.markStoryViewed(storyId)
        return NetworkResult.Success(true)
    }

    override suspend fun getReels(page: Int): NetworkResult<List<Post>> {
        val result = client.getList(
            collectionName = PocketBaseConstants.Collections.POSTS,
            page = page,
            perPage = 10,
            filter = "media_type = 'reel' || media_type = 'video'",
            sort = "-created",
            expand = "user",
            itemType = PBPostRecord::class.java
        )

        return when (result) {
            is NetworkResult.Success -> {
                val reels = result.data.items.map { record ->
                    val user = record.expand?.user
                    val avatarUrl = if (!user?.avatar.isNullOrBlank()) {
                        client.getFileUrl("users", user?.id ?: "", user?.avatar ?: "")
                    } else ""

                    val mediaUrls = record.media.map { fileName ->
                        client.getFileUrl(PocketBaseConstants.Collections.POSTS, record.id, fileName)
                    }

                    Post(
                        id = record.id,
                        author = User(
                            id = user?.id ?: record.userId,
                            username = user?.username ?: "creator",
                            name = user?.name ?: "Creator",
                            avatarUrl = avatarUrl,
                            isVerified = user?.verified ?: false
                        ),
                        caption = record.caption,
                        mediaUrls = mediaUrls,
                        mediaType = "reel",
                        likesCount = record.likesCount,
                        commentsCount = record.commentsCount,
                        createdAt = record.created
                    )
                }
                NetworkResult.Success(reels)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}

// Domain Mapping Extensions
fun CachedPostEntity.toDomain(): Post {
    val tags = if (hashtags.isNotBlank()) hashtags.split(",") else emptyList()
    val rawMentions = Regex("@([a-zA-Z0-9_]+)").findAll(caption).map { it.groupValues[1] }.toList()
    return Post(
        id = id,
        author = User(
            id = userId,
            username = authorUsername,
            name = authorName,
            avatarUrl = authorAvatarUrl,
            isVerified = authorIsVerified
        ),
        caption = caption,
        mediaUrls = if (mediaUrls.isNotBlank()) mediaUrls.split(",").filter { it.isNotBlank() } else emptyList(),
        mediaType = mediaType,
        location = location,
        hashtags = tags,
        mentions = rawMentions,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLiked = isLiked,
        isSaved = isSaved,
        isMine = isMine,
        createdAt = createdAt
    )
}

fun Post.toEntity(): CachedPostEntity {
    return CachedPostEntity(
        id = id,
        userId = author.id,
        authorUsername = author.username,
        authorName = author.name,
        authorAvatarUrl = author.avatarUrl,
        authorIsVerified = author.isVerified,
        caption = caption,
        mediaUrls = mediaUrls.joinToString(","),
        mediaType = mediaType,
        location = location,
        hashtags = hashtags.joinToString(","),
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLiked = isLiked,
        isSaved = isSaved,
        isMine = isMine,
        createdAt = createdAt
    )
}

fun CachedStoryEntity.toDomain(): Story {
    return Story(
        id = id,
        author = User(
            id = userId,
            username = authorUsername,
            name = authorName,
            avatarUrl = authorAvatarUrl,
            isVerified = authorIsVerified
        ),
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        caption = caption,
        viewsCount = viewsCount,
        isViewed = isViewed,
        createdAt = createdAt
    )
}

fun Story.toEntity(): CachedStoryEntity {
    return CachedStoryEntity(
        id = id,
        userId = author.id,
        authorUsername = author.username,
        authorName = author.name,
        authorAvatarUrl = author.avatarUrl,
        authorIsVerified = author.isVerified,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        caption = caption,
        viewsCount = viewsCount,
        isViewed = isViewed,
        createdAt = createdAt
    )
}

fun CachedCommentEntity.toDomain(): Comment {
    return Comment(
        id = id,
        postId = postId,
        author = User(
            id = userId,
            username = authorUsername,
            name = authorName,
            avatarUrl = authorAvatarUrl,
            isVerified = authorIsVerified
        ),
        content = content,
        parentCommentId = parentCommentId,
        likesCount = likesCount,
        isLiked = isLiked,
        createdAt = createdAt
    )
}

fun Comment.toEntity(): CachedCommentEntity {
    return CachedCommentEntity(
        id = id,
        postId = postId,
        userId = author.id,
        authorUsername = author.username,
        authorName = author.name,
        authorAvatarUrl = author.avatarUrl,
        authorIsVerified = author.isVerified,
        content = content,
        parentCommentId = parentCommentId,
        likesCount = likesCount,
        isLiked = isLiked,
        createdAt = createdAt
    )
}
