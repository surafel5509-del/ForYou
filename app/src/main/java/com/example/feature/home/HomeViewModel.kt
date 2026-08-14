package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.Story
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val isLoadingFeed: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val stories: List<Story> = emptyList(),
    val errorMessage: String? = null,
    // Story viewing & creation
    val activeStoryIndex: Int? = null,
    val isViewingStory: Boolean = false,
    val isCreatingStory: Boolean = false,
    // Comments sheet state
    val activeCommentsPost: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val replyingToComment: Comment? = null,
    val isSubmittingComment: Boolean = false,
    // Post editing dialog
    val editingPost: Post? = null,
    // Snackbar feedback
    val userNotice: String? = null
)

class HomeViewModel(
    private val getFeedUseCase: GetFeedUseCase,
    private val getStoriesUseCase: GetStoriesUseCase,
    private val toggleLikePostUseCase: ToggleLikePostUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeCommentUseCase: ToggleLikeCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val createStoryUseCase: CreateStoryUseCase,
    private val markStoryViewedUseCase: MarkStoryViewedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Observe cached posts directly from Room for instant offline / reactive updates
    val feedPosts: StateFlow<List<Post>> = getFeedUseCase.cachedFeed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Observe cached stories
    val storiesList: StateFlow<List<Story>> = getStoriesUseCase.cachedStories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadInitialFeedAndStories()
    }

    fun loadInitialFeedAndStories() {
        _uiState.update { it.copy(isLoadingFeed = true, errorMessage = null, currentPage = 1, hasMorePages = true) }
        viewModelScope.launch {
            // Load Stories
            when (val storyRes = getStoriesUseCase()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(stories = storyRes.data) }
                }
                is NetworkResult.Error -> {}
                is NetworkResult.Loading -> {}
            }

            // Load Feed Page 1
            when (val feedRes = getFeedUseCase(page = 1, perPage = 15)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingFeed = false,
                            hasMorePages = feedRes.data.size >= 15,
                            currentPage = 1
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingFeed = false, errorMessage = feedRes.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoadingFeed || !state.hasMorePages) return

        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            when (val res = getFeedUseCase(page = nextPage, perPage = 15)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            currentPage = nextPage,
                            hasMorePages = res.data.size >= 15
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            getStoriesUseCase()
            val feedRes = getFeedUseCase(page = 1, perPage = 15)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    currentPage = 1,
                    hasMorePages = (feedRes is NetworkResult.Success && feedRes.data.size >= 15)
                )
            }
        }
    }

    // LIKES & BOOKMARKS
    fun onLikePost(post: Post) {
        viewModelScope.launch {
            toggleLikePostUseCase(post.id, post.likesCount, post.isLiked)
        }
    }

    fun onSavePost(post: Post) {
        viewModelScope.launch {
            toggleSavePostUseCase(post.id, post.isSaved)
            val notice = if (!post.isSaved) "Saved to your bookmarks" else "Removed from bookmarks"
            _uiState.update { it.copy(userNotice = notice) }
        }
    }

    // POST EDIT & DELETE
    fun onStartEditPost(post: Post) {
        _uiState.update { it.copy(editingPost = post) }
    }

    fun onDismissEditPost() {
        _uiState.update { it.copy(editingPost = null) }
    }

    fun onSaveEditedPost(postId: String, newCaption: String, newLocation: String, newHashtags: List<String>) {
        viewModelScope.launch {
            updatePostUseCase(postId, newCaption, newLocation, newHashtags)
            _uiState.update { it.copy(editingPost = null, userNotice = "Post updated") }
        }
    }

    fun onDeletePost(post: Post) {
        viewModelScope.launch {
            deletePostUseCase(post.id)
            _uiState.update { it.copy(userNotice = "Post deleted") }
        }
    }

    // COMMENTS & REPLIES
    fun openCommentsForPost(post: Post) {
        _uiState.update { it.copy(activeCommentsPost = post, isLoadingComments = true, replyingToComment = null) }
        viewModelScope.launch {
            getCommentsUseCase(post.id).collect { commentsList ->
                _uiState.update {
                    it.copy(
                        comments = commentsList,
                        isLoadingComments = false
                    )
                }
            }
        }
    }

    fun closeComments() {
        _uiState.update { it.copy(activeCommentsPost = null, comments = emptyList(), replyingToComment = null) }
    }

    fun onReplyToComment(comment: Comment) {
        _uiState.update { it.copy(replyingToComment = comment) }
    }

    fun onCancelReply() {
        _uiState.update { it.copy(replyingToComment = null) }
    }

    fun onAddComment(content: String) {
        val activePost = _uiState.value.activeCommentsPost ?: return
        val replyingTo = _uiState.value.replyingToComment
        val parentId = replyingTo?.id ?: ""

        _uiState.update { it.copy(isSubmittingComment = true) }
        viewModelScope.launch {
            when (val res = addCommentUseCase(activePost.id, content, parentId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSubmittingComment = false, replyingToComment = null) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSubmittingComment = false, userNotice = res.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onLikeComment(comment: Comment) {
        viewModelScope.launch {
            toggleLikeCommentUseCase(comment.id, comment.likesCount, comment.isLiked)
        }
    }

    fun onDeleteComment(comment: Comment) {
        val activePost = _uiState.value.activeCommentsPost ?: return
        viewModelScope.launch {
            deleteCommentUseCase(comment.id, activePost.id)
        }
    }

    // STORIES
    fun openStoryViewer(index: Int) {
        _uiState.update { it.copy(activeStoryIndex = index, isViewingStory = true) }
        val stories = _uiState.value.stories
        if (index in stories.indices) {
            val story = stories[index]
            viewModelScope.launch {
                markStoryViewedUseCase(story.id)
            }
        }
    }

    fun closeStoryViewer() {
        _uiState.update { it.copy(activeStoryIndex = null, isViewingStory = false) }
    }

    fun openCreateStory() {
        _uiState.update { it.copy(isCreatingStory = true) }
    }

    fun closeCreateStory() {
        _uiState.update { it.copy(isCreatingStory = false) }
    }

    fun createStory(caption: String, mediaFile: File? = null) {
        viewModelScope.launch {
            createStoryUseCase(mediaFile, caption, "image")
            _uiState.update { it.copy(isCreatingStory = false, userNotice = "Story published ✨") }
            getStoriesUseCase()
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }
}
