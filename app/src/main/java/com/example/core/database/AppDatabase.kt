package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.ChatDao
import com.example.core.database.dao.CommentDao
import com.example.core.database.dao.PostDao
import com.example.core.database.dao.StoryDao
import com.example.core.database.dao.UserDao
import com.example.core.database.entities.CachedCommentEntity
import com.example.core.database.entities.CachedMessageEntity
import com.example.core.database.entities.CachedPostEntity
import com.example.core.database.entities.CachedStoryEntity
import com.example.core.database.entities.CachedUserEntity

@Database(
    entities = [
        CachedPostEntity::class,
        CachedStoryEntity::class,
        CachedCommentEntity::class,
        CachedUserEntity::class,
        CachedMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun commentDao(): CommentDao
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foryou_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
