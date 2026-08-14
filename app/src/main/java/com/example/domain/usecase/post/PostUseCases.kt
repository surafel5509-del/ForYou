package com.example.domain.usecase.post

import com.example.core.network.NetworkResult
import com.example.data.repository.PostRepository
import com.example.domain.model.Comment
import com.example.domain.model.Post
import com.example.domain.model.Story
import kotlinx.coroutines.flow.Flow
import java.io.File

class GetFeedUseCase(private val repository: PostRepository) {
    val cachedFeed: Flow<List<Post>> = repository.cachedFeed

    suspend operator fun invoke(page: Int = 1, perPage: Int = 15): NetworkResult<List<Post>> {
        return repository.getFeed(page, perPage)
    }
}

class GetStoriesUseCase(private val repository: PostRepository) {
    val cachedStories: Flow<List<Story>> = repository.cachedStories

    suspend operator fun invoke(): NetworkResult<List<Story>> {
        return repository.getStories()
    }
}

class CreatePostUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(
        caption: String,
        mediaFiles: List<File>,
        mediaType: String = "image",
        location: String = "",
        hashtags: List<String> = emptyList()
    ): NetworkResult<Post> {
        val sanitizedCaption = caption.trim()
        val extractedTags = if (hashtags.isNotEmpty()) {
            hashtags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
        } else {
            Regex("#([a-zA-Z0-9_]+)").findAll(sanitizedCaption).map { it.groupValues[1] }.toList()
        }

        return repository.createPost(
            caption = sanitizedCaption,
            mediaFiles = mediaFiles,
            mediaType = mediaType,
            location = location.trim(),
            hashtags = extractedTags
        )
    }
}

class UpdatePostUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(
        postId: String,
        caption: String,
        location: String = "",
        hashtags: List<String> = emptyList()
    ): NetworkResult<Post> {
        return repository.updatePost(
            postId = postId,
            caption = caption.trim(),
            location = location.trim(),
            hashtags = hashtags
        )
    }
}

class DeletePostUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(postId: String): NetworkResult<Boolean> {
        return repository.deletePost(postId)
    }
}

class ToggleLikePostUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(postId: String, currentLikes: Int, isCurrentlyLiked: Boolean): NetworkResult<Boolean> {
        return repository.toggleLike(postId, currentLikes, isCurrentlyLiked)
    }
}

class ToggleSavePostUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(postId: String, isCurrentlySaved: Boolean): NetworkResult<Boolean> {
        return repository.toggleSave(postId, isCurrentlySaved)
    }
}

class GetCommentsUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(postId: String): Flow<List<Comment>> {
        repository.fetchComments(postId)
        return repository.getComments(postId)
    }
}

class AddCommentUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(postId: String, content: String, parentCommentId: String = ""): NetworkResult<Comment> {
        if (content.isBlank()) return NetworkResult.Error("Comment cannot be empty")
        return repository.addComment(postId, content.trim(), parentCommentId)
    }
}

class ToggleLikeCommentUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(commentId: String, currentLikes: Int, isCurrentlyLiked: Boolean): NetworkResult<Boolean> {
        return repository.toggleLikeComment(commentId, currentLikes, isCurrentlyLiked)
    }
}

class DeleteCommentUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(commentId: String, postId: String): NetworkResult<Boolean> {
        return repository.deleteComment(commentId, postId)
    }
}

class CreateStoryUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(mediaFile: File?, caption: String = "", mediaType: String = "image"): NetworkResult<Story> {
        return repository.createStory(mediaFile, caption.trim(), mediaType)
    }
}

class MarkStoryViewedUseCase(private val repository: PostRepository) {
    suspend operator fun invoke(storyId: String): NetworkResult<Boolean> {
        return repository.markStoryViewed(storyId)
    }
}
