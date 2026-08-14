package com.example.feature.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.data.repository.PostRepository
import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.User
import com.example.domain.usecase.post.AddCommentUseCase
import com.example.domain.usecase.post.DeleteCommentUseCase
import com.example.domain.usecase.post.GetCommentsUseCase
import com.example.domain.usecase.post.ToggleLikeCommentUseCase
import com.example.domain.usecase.post.ToggleLikePostUseCase
import com.example.domain.usecase.post.ToggleSavePostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReelsUiState(
    val reels: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isMuted: Boolean = false,
    val activeCommentsReel: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val replyingToComment: Comment? = null,
    val isSubmittingComment: Boolean = false,
    val followedUserIds: Set<String> = emptySet(),
    val userNotice: String? = null,
    val errorMessage: String? = null
)

class ReelsViewModel(
    private val postRepository: PostRepository,
    private val toggleLikePostUseCase: ToggleLikePostUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeCommentUseCase: ToggleLikeCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    // Curated high-performance sample reels for rich fallback experience
    private val defaultReels = listOf(
        Post(
            id = "reel_1",
            author = User(
                id = "user_aurora",
                username = "aurora_vibes",
                name = "Aurora Skye",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                isVerified = true
            ),
            caption = "Golden hour vibes in Tokyo ✨🌆 #foryou #travel #tokyo #sunset #aesthetic",
            mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            mediaType = "reel",
            likesCount = 14200,
            commentsCount = 384,
            isLiked = false,
            isSaved = false,
            location = "Tokyo, Japan",
            createdAt = "2h ago"
        ),
        Post(
            id = "reel_2",
            author = User(
                id = "user_chef",
                username = "culinary_art",
                name = "Chef Marco",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                isVerified = true
            ),
            caption = "Crispy Truffle Gnocchi in 60 seconds 🍝🔥 Would you try this? #foodie #cooking #recipe #cheflife",
            mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
            mediaType = "reel",
            likesCount = 28900,
            commentsCount = 742,
            isLiked = true,
            isSaved = false,
            location = "Rome, Italy",
            createdAt = "5h ago"
        ),
        Post(
            id = "reel_3",
            author = User(
                id = "user_neo",
                username = "neo_motion",
                name = "Neo Visuals",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200",
                isVerified = false
            ),
            caption = "Cyberpunk neon synth wave 3D loop 🌌 Rate 1-10! #blender #3dart #cyberpunk #vfx",
            mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"),
            mediaType = "reel",
            likesCount = 9520,
            commentsCount = 129,
            isLiked = false,
            isSaved = true,
            location = "Neo Seoul",
            createdAt = "1d ago"
        ),
        Post(
            id = "reel_4",
            author = User(
                id = "user_fitness",
                username = "fit_clara",
                name = "Clara Strong",
                avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200",
                isVerified = true
            ),
            caption = "15-minute core burn workout 🔥 Save this for leg day tomorrow! 💪 #fitness #workout #motivation",
            mediaUrls = listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"),
            mediaType = "reel",
            likesCount = 41300,
            commentsCount = 912,
            isLiked = false,
            isSaved = false,
            location = "Santa Monica, CA",
            createdAt = "1d ago"
        )
    )

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = postRepository.getReels(1)) {
                is NetworkResult.Success -> {
                    val serverReels = res.data
                    val merged = if (serverReels.isNotEmpty()) serverReels else defaultReels
                    _uiState.update { it.copy(reels = merged, isLoading = false) }
                }
                else -> {
                    _uiState.update { it.copy(reels = defaultReels, isLoading = false) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val res = postRepository.getReels(1)) {
                is NetworkResult.Success -> {
                    val serverReels = res.data
                    val merged = if (serverReels.isNotEmpty()) serverReels else defaultReels
                    _uiState.update { it.copy(reels = merged, isRefreshing = false) }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun toggleLike(reel: Post) {
        val currentlyLiked = reel.isLiked
        val updatedLikesCount = if (currentlyLiked) (reel.likesCount - 1).coerceAtLeast(0) else reel.likesCount + 1

        // Optimistic update
        _uiState.update { state ->
            val updated = state.reels.map {
                if (it.id == reel.id) it.copy(isLiked = !currentlyLiked, likesCount = updatedLikesCount)
                else it
            }
            state.copy(reels = updated)
        }

        viewModelScope.launch {
            toggleLikePostUseCase(reel.id, reel.likesCount, currentlyLiked)
        }
    }

    fun toggleSave(reel: Post) {
        val currentlySaved = reel.isSaved
        _uiState.update { state ->
            val updated = state.reels.map {
                if (it.id == reel.id) it.copy(isSaved = !currentlySaved)
                else it
            }
            state.copy(
                reels = updated,
                userNotice = if (!currentlySaved) "Reel saved to bookmarks" else "Reel removed from bookmarks"
            )
        }

        viewModelScope.launch {
            toggleSavePostUseCase(reel.id, currentlySaved)
        }
    }

    fun toggleFollow(userId: String) {
        _uiState.update { state ->
            val currentFollowed = state.followedUserIds
            val isFollowing = currentFollowed.contains(userId)
            val updated = if (isFollowing) currentFollowed - userId else currentFollowed + userId
            state.copy(
                followedUserIds = updated,
                userNotice = if (!isFollowing) "Following creator" else "Unfollowed creator"
            )
        }
    }

    fun toggleMute(playerManager: ReelsPlayerManager) {
        val muted = playerManager.toggleMute()
        _uiState.update { it.copy(isMuted = muted) }
    }

    fun openComments(reel: Post) {
        _uiState.update { it.copy(activeCommentsReel = reel, isLoadingComments = true) }
        viewModelScope.launch {
            getCommentsUseCase(reel.id).collect { commentsList ->
                _uiState.update { it.copy(comments = commentsList, isLoadingComments = false) }
            }
        }
    }

    fun closeComments() {
        _uiState.update {
            it.copy(
                activeCommentsReel = null,
                comments = emptyList(),
                replyingToComment = null
            )
        }
    }

    fun onReplyToComment(comment: Comment) {
        _uiState.update { it.copy(replyingToComment = comment) }
    }

    fun onCancelReply() {
        _uiState.update { it.copy(replyingToComment = null) }
    }

    fun onAddComment(content: String) {
        val activeReel = _uiState.value.activeCommentsReel ?: return
        val parentId = _uiState.value.replyingToComment?.id ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true) }
            when (val res = addCommentUseCase(activeReel.id, content, parentId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        val updatedReels = state.reels.map {
                            if (it.id == activeReel.id) it.copy(commentsCount = it.commentsCount + 1)
                            else it
                        }
                        state.copy(
                            reels = updatedReels,
                            replyingToComment = null,
                            isSubmittingComment = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isSubmittingComment = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun onLikeComment(comment: Comment) {
        viewModelScope.launch {
            toggleLikeCommentUseCase(comment.id, comment.likesCount, comment.isLiked)
        }
    }

    fun onDeleteComment(comment: Comment) {
        val activeReel = _uiState.value.activeCommentsReel ?: return
        viewModelScope.launch {
            deleteCommentUseCase(comment.id, activeReel.id)
            _uiState.update { state ->
                val updatedReels = state.reels.map {
                    if (it.id == activeReel.id) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0))
                    else it
                }
                state.copy(reels = updatedReels)
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }
}
