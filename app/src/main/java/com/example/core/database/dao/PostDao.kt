package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entities.CachedPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM cached_posts ORDER BY cachedAt DESC")
    fun getAllPosts(): Flow<List<CachedPostEntity>>

    @Query("SELECT * FROM cached_posts WHERE id = :postId LIMIT 1")
    fun getPostById(postId: String): Flow<CachedPostEntity?>

    @Query("SELECT * FROM cached_posts WHERE userId = :userId ORDER BY cachedAt DESC")
    fun getPostsByUserId(userId: String): Flow<List<CachedPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CachedPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CachedPostEntity)

    @Update
    suspend fun updatePost(post: CachedPostEntity)

    @Query("UPDATE cached_posts SET caption = :caption, location = :location, hashtags = :hashtags WHERE id = :postId")
    suspend fun updatePostContent(postId: String, caption: String, location: String, hashtags: String)

    @Query("UPDATE cached_posts SET isLiked = :isLiked, likesCount = :likesCount WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, isLiked: Boolean, likesCount: Int)

    @Query("UPDATE cached_posts SET isSaved = :isSaved WHERE id = :postId")
    suspend fun updateSavedStatus(postId: String, isSaved: Boolean)

    @Query("UPDATE cached_posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: String)

    @Query("UPDATE cached_posts SET commentsCount = CASE WHEN commentsCount > 0 THEN commentsCount - 1 ELSE 0 END WHERE id = :postId")
    suspend fun decrementCommentCount(postId: String)

    @Query("DELETE FROM cached_posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM cached_posts")
    suspend fun clearAllPosts()
}
