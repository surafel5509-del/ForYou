package com.example.feature.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkResult
import com.example.domain.model.Post
import com.example.domain.usecase.post.CreatePostUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SelectedMediaItem(
    val id: String,
    val file: File? = null,
    val previewUrl: String = "",
    val isVideo: Boolean = false
)

data class CreatePostUiState(
    val caption: String = "",
    val location: String = "",
    val hashtagsInput: String = "",
    val selectedMedia: List<SelectedMediaItem> = emptyList(),
    val mediaType: String = "image", // image, video, reel
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f, // 0.0 to 1.0
    val activeHashtagQuery: String? = null,
    val activeMentionQuery: String? = null,
    val suggestedHashtags: List<String> = emptyList(),
    val suggestedMentions: List<String> = emptyList(),
    val popularLocations: List<String> = listOf(
        "Paris, France",
        "Tokyo, Japan",
        "New York, USA",
        "London, UK",
        "Bali, Indonesia",
        "San Francisco, CA",
        "Sydney, Australia",
        "Rome, Italy"
    ),
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class CreatePostViewModel(
    private val createPostUseCase: CreatePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val allPresetHashtags = listOf(
        "foryou", "trending", "photography", "vibes", "art", "creator",
        "travel", "lifestyle", "music", "sunset", "explore", "reels",
        "fitness", "foodie", "fashion", "tech", "design", "nature"
    )

    private val allPresetMentions = listOf(
        "foryou_team", "alex_adventures", "sophia_creates", "marco_lens",
        "elena_design", "jordan_beats", "chloe_style", "david_tech"
    )

    fun onCaptionChanged(newCaption: String) {
        // Detect active hashtag query
        val lastWord = newCaption.split(" ", "\n").lastOrNull() ?: ""
        val hashtagQuery = if (lastWord.startsWith("#") && lastWord.length > 1) {
            lastWord.removePrefix("#").lowercase()
        } else null

        val mentionQuery = if (lastWord.startsWith("@") && lastWord.length > 1) {
            lastWord.removePrefix("@").lowercase()
        } else null

        val matchingTags = if (hashtagQuery != null) {
            allPresetHashtags.filter { it.contains(hashtagQuery) }
        } else emptyList()

        val matchingMentions = if (mentionQuery != null) {
            allPresetMentions.filter { it.contains(mentionQuery) }
        } else emptyList()

        _uiState.update {
            it.copy(
                caption = newCaption,
                activeHashtagQuery = hashtagQuery,
                activeMentionQuery = mentionQuery,
                suggestedHashtags = matchingTags,
                suggestedMentions = matchingMentions
            )
        }
    }

    fun appendHashtag(tag: String) {
        val cleanTag = tag.removePrefix("#")
        val currentCaption = _uiState.value.caption
        val lastWord = currentCaption.split(" ", "\n").lastOrNull() ?: ""

        val updatedCaption = if (lastWord.startsWith("#")) {
            val prefix = currentCaption.substringBeforeLast(lastWord)
            "$prefix#$cleanTag "
        } else {
            if (currentCaption.isBlank() || currentCaption.endsWith(" ")) {
                "$currentCaption#$cleanTag "
            } else {
                "$currentCaption #$cleanTag "
            }
        }

        _uiState.update {
            it.copy(
                caption = updatedCaption,
                activeHashtagQuery = null,
                suggestedHashtags = emptyList()
            )
        }
    }

    fun appendMention(mention: String) {
        val cleanMention = mention.removePrefix("@")
        val currentCaption = _uiState.value.caption
        val lastWord = currentCaption.split(" ", "\n").lastOrNull() ?: ""

        val updatedCaption = if (lastWord.startsWith("@")) {
            val prefix = currentCaption.substringBeforeLast(lastWord)
            "$prefix@$cleanMention "
        } else {
            if (currentCaption.isBlank() || currentCaption.endsWith(" ")) {
                "$currentCaption@$cleanMention "
            } else {
                "$currentCaption @$cleanMention "
            }
        }

        _uiState.update {
            it.copy(
                caption = updatedCaption,
                activeMentionQuery = null,
                suggestedMentions = emptyList()
            )
        }
    }

    fun onLocationChanged(newLocation: String) {
        _uiState.update { it.copy(location = newLocation) }
    }

    fun onMediaTypeChanged(newType: String) {
        _uiState.update { it.copy(mediaType = newType) }
    }

    fun addSampleMedia(imageUrl: String, isVideo: Boolean = false) {
        val newItem = SelectedMediaItem(
            id = System.currentTimeMillis().toString(),
            previewUrl = imageUrl,
            isVideo = isVideo
        )
        _uiState.update {
            it.copy(selectedMedia = it.selectedMedia + newItem)
        }
    }

    fun removeMediaItem(id: String) {
        _uiState.update {
            it.copy(selectedMedia = it.selectedMedia.filter { item -> item.id != id })
        }
    }

    fun createPost(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.caption.isBlank() && state.selectedMedia.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a caption or add a photo.") }
            return
        }

        _uiState.update { it.copy(isUploading = true, uploadProgress = 0.1f, errorMessage = null) }

        viewModelScope.launch {
            // Smooth upload progress animation
            delay(200)
            _uiState.update { it.copy(uploadProgress = 0.45f) }
            delay(250)
            _uiState.update { it.copy(uploadProgress = 0.85f) }

            val mediaFiles = state.selectedMedia.mapNotNull { it.file }
            val tagsList = Regex("#([a-zA-Z0-9_]+)")
                .findAll(state.caption)
                .map { it.groupValues[1] }
                .toList()

            val result = createPostUseCase(
                caption = state.caption,
                mediaFiles = mediaFiles,
                mediaType = state.mediaType,
                location = state.location,
                hashtags = tagsList
            )

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isUploading = false, uploadProgress = 1f, isSuccess = true) }
                    delay(150)
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
