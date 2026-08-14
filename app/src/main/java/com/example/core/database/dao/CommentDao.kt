package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entities.CachedCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM cached_comments WHERE postId = :postId ORDER BY cachedAt ASC")
    fun getCommentsForPost(postId: String): Flow<List<CachedCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CachedCommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CachedCommentEntity)

    @Query("UPDATE cached_comments SET isLiked = :isLiked, likesCount = :likesCount WHERE id = :commentId")
    suspend fun updateCommentLike(commentId: String, isLiked: Boolean, likesCount: Int)

    @Query("DELETE FROM cached_comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("DELETE FROM cached_comments WHERE postId = :postId")
    suspend fun clearCommentsForPost(postId: String)
}
