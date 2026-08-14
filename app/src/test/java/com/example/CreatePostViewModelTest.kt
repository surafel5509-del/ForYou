package com.example

import com.example.domain.usecase.post.CreatePostUseCase
import com.example.feature.create.CreatePostViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePostRepository
    private lateinit var viewModel: CreatePostViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePostRepository()
        viewModel = CreatePostViewModel(
            createPostUseCase = CreatePostUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTypingHashtagTriggersSuggestions() {
        viewModel.onCaptionChanged("Exploring the city #tra")
        assertEquals("tra", viewModel.uiState.value.activeHashtagQuery)
        assertTrue(viewModel.uiState.value.suggestedHashtags.contains("travel"))
    }

    @Test
    fun testAppendingHashtagUpdatesCaption() {
        viewModel.onCaptionChanged("Check out this view #pho")
        viewModel.appendHashtag("photography")
        assertEquals("Check out this view #photography ", viewModel.uiState.value.caption)
    }

    @Test
    fun testTypingMentionTriggersSuggestions() {
        viewModel.onCaptionChanged("Shoutout to @ale")
        assertEquals("ale", viewModel.uiState.value.activeMentionQuery)
        assertTrue(viewModel.uiState.value.suggestedMentions.contains("alex_adventures"))
    }

    @Test
    fun testAddAndRemoveMediaItem() {
        viewModel.addSampleMedia("https://images.unsplash.com/photo-1", isVideo = false)
        assertEquals(1, viewModel.uiState.value.selectedMedia.size)

        val itemId = viewModel.uiState.value.selectedMedia.first().id
        viewModel.removeMediaItem(itemId)
        assertEquals(0, viewModel.uiState.value.selectedMedia.size)
    }

    @Test
    fun testCreatePostSuccessFlow() = runTest {
        var successCallbackCalled = false
        viewModel.onCaptionChanged("First test post on Foryou! #foryou")
        viewModel.createPost(onSuccess = { successCallbackCalled = true })

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertTrue(successCallbackCalled)
    }
}
