package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entities.CachedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<CachedUserEntity?>

    @Query("SELECT * FROM cached_users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): Flow<CachedUserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CachedUserEntity>)

    @Query("DELETE FROM cached_users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM cached_users")
    suspend fun clearUsers()
}
