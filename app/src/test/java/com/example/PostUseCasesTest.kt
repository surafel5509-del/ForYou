package com.example

import com.example.core.network.NetworkResult
import com.example.data.repository.PostRepository
import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.Story
import com.example.domain.model.User
import com.example.domain.usecase.post.AddCommentUseCase
import com.example.domain.usecase.post.CreatePostUseCase
import com.example.domain.usecase.post.CreateStoryUseCase
import com.example.domain.usecase.post.DeleteCommentUseCase
import com.example.domain.usecase.post.DeletePostUseCase
import com.example.domain.usecase.post.GetCommentsUseCase
import com.example.domain.usecase.post.GetFeedUseCase
import com.example.domain.usecase.post.GetStoriesUseCase
import com.example.domain.usecase.post.MarkStoryViewedUseCase
import com.example.domain.usecase.post.ToggleLikeCommentUseCase
import com.example.domain.usecase.post.ToggleLikePostUseCase
import com.example.domain.usecase.post.ToggleSavePostUseCase
import com.example.domain.usecase.post.UpdatePostUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FakePostRepository : PostRepository {
    val postsFlow = MutableStateFlow<List<Post>>(emptyList())
    val storiesFlow = MutableStateFlow<List<Story>>(emptyList())
    val commentsFlow = MutableStateFlow<List<Comment>>(emptyList())

    override val cachedFeed: Flow<List<Post>> = postsFlow
    override val cachedStories: Flow<List<Story>> = storiesFlow

    var createPostCalledWithCaption: String? = null
    var createPostCalledWithTags: List<String> = emptyList()
    var deletePostCalledWithId: String? = null
    var toggleLikeCalledWithId: String? = null
    var toggleSaveCalledWithId: String? = null
    var addCommentCalledWithContent: String? = null

    override suspend fun getFeed(page: Int, perPage: Int): NetworkResult<List<Post>> {
        return NetworkResult.Success(postsFlow.value)
    }

    override suspend fun getStories(): NetworkResult<List<Story>> {
        return NetworkResult.Success(storiesFlow.value)
    }

    override suspend fun createPost(
        caption: String,
        mediaFiles: List<File>,
        mediaType: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post> {
        createPostCalledWithCaption = caption
        createPostCalledWithTags = hashtags
        val newPost = Post(
            id = "p_new",
            author = User(id = "me", username = "creator"),
            caption = caption,
            location = location,
            hashtags = hashtags
        )
        postsFlow.value = listOf(newPost) + postsFlow.value
        return NetworkResult.Success(newPost)
    }

    override suspend fun updatePost(
        postId: String,
        caption: String,
        location: String,
        hashtags: List<String>
    ): NetworkResult<Post> {
        val updated = Post(id = postId, caption = caption, location = location, hashtags = hashtags)
        return NetworkResult.Success(updated)
    }

    override suspend fun deletePost(postId: String): NetworkResult<Boolean> {
        deletePostCalledWithId = postId
        postsFlow.value = postsFlow.value.filter { it.id != postId }
        return NetworkResult.Success(true)
    }

    override suspend fun toggleLike(
        postId: String,
        currentLikes: Int,
        isCurrentlyLiked: Boolean
    ): NetworkResult<Boolean> {
        toggleLikeCalledWithId = postId
        return NetworkResult.Success(!isCurrentlyLiked)
    }

    override suspend fun toggleSave(postId: String, isCurrentlySaved: Boolean): NetworkResult<Boolean> {
        toggleSaveCalledWithId = postId
        return NetworkResult.Success(!isCurrentlySaved)
    }

    override suspend fun getComments(postId: String): Flow<List<Comment>> {
        return commentsFlow
    }

    override suspend fun fetchComments(postId: String): NetworkResult<List<Comment>> {
        return NetworkResult.Success(commentsFlow.value)
    }

    override suspend fun addComment(
        postId: String,
        content: String,
        parentCommentId: String
    ): NetworkResult<Comment> {
        addCommentCalledWithContent = content
        val newComment = Comment(id = "c_new", postId = postId, content = content, parentCommentId = parentCommentId)
        commentsFlow.value = commentsFlow.value + newComment
        return NetworkResult.Success(newComment)
    }

    override suspend fun toggleLikeComment(
        commentId: String,
        currentLikes: Int,
        isCurrentlyLiked: Boolean
    ): NetworkResult<Boolean> {
        return NetworkResult.Success(!isCurrentlyLiked)
    }

    override suspend fun deleteComment(commentId: String, postId: String): NetworkResult<Boolean> {
        commentsFlow.value = commentsFlow.value.filter { it.id != commentId }
        return NetworkResult.Success(true)
    }

    override suspend fun createStory(
        mediaFile: File?,
        caption: String,
        mediaType: String
    ): NetworkResult<Story> {
        val story = Story(id = "s_new", caption = caption)
        storiesFlow.value = listOf(story) + storiesFlow.value
        return NetworkResult.Success(story)
    }

    override suspend fun markStoryViewed(storyId: String): NetworkResult<Boolean> {
        return NetworkResult.Success(true)
    }

    override suspend fun getReels(page: Int): NetworkResult<List<Post>> {
        return NetworkResult.Success(emptyList())
    }
}

class PostUseCasesTest {

    private lateinit var fakeRepository: FakePostRepository
    private lateinit var getFeedUseCase: GetFeedUseCase
    private lateinit var getStoriesUseCase: GetStoriesUseCase
    private lateinit var createPostUseCase: CreatePostUseCase
    private lateinit var deletePostUseCase: DeletePostUseCase
    private lateinit var toggleLikePostUseCase: ToggleLikePostUseCase
    private lateinit var toggleSavePostUseCase: ToggleSavePostUseCase
    private lateinit var addCommentUseCase: AddCommentUseCase

    @Before
    fun setup() {
        fakeRepository = FakePostRepository()
        getFeedUseCase = GetFeedUseCase(fakeRepository)
        getStoriesUseCase = GetStoriesUseCase(fakeRepository)
        createPostUseCase = CreatePostUseCase(fakeRepository)
        deletePostUseCase = DeletePostUseCase(fakeRepository)
        toggleLikePostUseCase = ToggleLikePostUseCase(fakeRepository)
        toggleSavePostUseCase = ToggleSavePostUseCase(fakeRepository)
        addCommentUseCase = AddCommentUseCase(fakeRepository)
    }

    @Test
    fun testCreatePostExtractsHashtagsAutomatically() = runTest {
        val result = createPostUseCase(
            caption = "Loving the sunset vibes in Tokyo! #sunset #travel #japan",
            mediaFiles = emptyList(),
            location = "Tokyo, Japan"
        )

        assertTrue(result is NetworkResult.Success)
        assertEquals("Loving the sunset vibes in Tokyo! #sunset #travel #japan", fakeRepository.createPostCalledWithCaption)
        assertTrue(fakeRepository.createPostCalledWithTags.contains("sunset"))
        assertTrue(fakeRepository.createPostCalledWithTags.contains("travel"))
        assertTrue(fakeRepository.createPostCalledWithTags.contains("japan"))
    }

    @Test
    fun testToggleLikePostReturnsToggledStatus() = runTest {
        val result = toggleLikePostUseCase("p1", currentLikes = 10, isCurrentlyLiked = false)
        assertTrue(result is NetworkResult.Success)
        assertEquals(true, (result as NetworkResult.Success).data)
        assertEquals("p1", fakeRepository.toggleLikeCalledWithId)
    }

    @Test
    fun testToggleSavePostReturnsToggledStatus() = runTest {
        val result = toggleSavePostUseCase("p1", isCurrentlySaved = false)
        assertTrue(result is NetworkResult.Success)
        assertEquals(true, (result as NetworkResult.Success).data)
        assertEquals("p1", fakeRepository.toggleSaveCalledWithId)
    }

    @Test
    fun testAddCommentAppendsToComments() = runTest {
        val result = addCommentUseCase("p1", "Amazing photography! ✨")
        assertTrue(result is NetworkResult.Success)
        assertEquals("Amazing photography! ✨", fakeRepository.addCommentCalledWithContent)
        assertEquals(1, fakeRepository.commentsFlow.value.size)
    }

    @Test
    fun testDeletePostRemovesFromFeed() = runTest {
        fakeRepository.postsFlow.value = listOf(
            Post(id = "p1", caption = "First post"),
            Post(id = "p2", caption = "Second post")
        )
        val result = deletePostUseCase("p1")
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, fakeRepository.postsFlow.value.size)
        assertEquals("p2", fakeRepository.postsFlow.value.first().id)
    }
}
