package com.example

import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.Story
import com.example.domain.model.User
import com.example.domain.usecase.post.AddCommentUseCase
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
import com.example.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePostRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePostRepository()

        fakeRepository.postsFlow.value = listOf(
            Post(id = "p1", caption = "Scenic view", likesCount = 5, isLiked = false),
            Post(id = "p2", caption = "Urban vibes", likesCount = 12, isLiked = true)
        )
        fakeRepository.storiesFlow.value = listOf(
            Story(id = "s1", caption = "Morning coffee"),
            Story(id = "s2", caption = "Gym time")
        )

        viewModel = HomeViewModel(
            getFeedUseCase = GetFeedUseCase(fakeRepository),
            getStoriesUseCase = GetStoriesUseCase(fakeRepository),
            toggleLikePostUseCase = ToggleLikePostUseCase(fakeRepository),
            toggleSavePostUseCase = ToggleSavePostUseCase(fakeRepository),
            deletePostUseCase = DeletePostUseCase(fakeRepository),
            updatePostUseCase = UpdatePostUseCase(fakeRepository),
            getCommentsUseCase = GetCommentsUseCase(fakeRepository),
            addCommentUseCase = AddCommentUseCase(fakeRepository),
            toggleLikeCommentUseCase = ToggleLikeCommentUseCase(fakeRepository),
            deleteCommentUseCase = DeleteCommentUseCase(fakeRepository),
            createStoryUseCase = CreateStoryUseCase(fakeRepository),
            markStoryViewedUseCase = MarkStoryViewedUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFeedLoadingLoadsStoriesAndFeed() = runTest {
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.stories.size)
        assertEquals(false, viewModel.uiState.value.isLoadingFeed)
    }

    @Test
    fun testOpenAndCloseCommentsSheet() = runTest {
        val samplePost = Post(id = "p1", caption = "Hello world")
        viewModel.openCommentsForPost(samplePost)
        assertEquals("p1", viewModel.uiState.value.activeCommentsPost?.id)

        viewModel.closeComments()
        assertNull(viewModel.uiState.value.activeCommentsPost)
    }

    @Test
    fun testStoryViewerState() = runTest {
        viewModel.openStoryViewer(0)
        assertTrue(viewModel.uiState.value.isViewingStory)
        assertEquals(0, viewModel.uiState.value.activeStoryIndex)

        viewModel.closeStoryViewer()
        assertEquals(false, viewModel.uiState.value.isViewingStory)
    }

    @Test
    fun testReplyToCommentState() = runTest {
        val comment = Comment(id = "c1", content = "Nice post!", author = User(username = "alice"))
        viewModel.onReplyToComment(comment)
        assertEquals("c1", viewModel.uiState.value.replyingToComment?.id)

        viewModel.onCancelReply()
        assertNull(viewModel.uiState.value.replyingToComment)
    }
}
