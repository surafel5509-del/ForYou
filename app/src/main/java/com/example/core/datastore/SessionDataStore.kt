package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "foryou_session")

data class UserSession(
    val token: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val darkTheme: Boolean? = null // null means system default
) {
    val isLoggedIn: Boolean get() = token.isNotBlank() && userId.isNotBlank()
}

class SessionDataStore(private val context: Context) {

    private object PreferencesKeys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val USERNAME = stringPreferencesKey("username")
        val NAME = stringPreferencesKey("name")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val IS_VERIFIED = booleanPreferencesKey("is_verified")
        val IS_PRIVATE = booleanPreferencesKey("is_private")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val HAS_SET_THEME = booleanPreferencesKey("has_set_theme")
    }

    val sessionFlow: Flow<UserSession> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasSetTheme = preferences[PreferencesKeys.HAS_SET_THEME] ?: false
            val darkTheme = if (hasSetTheme) preferences[PreferencesKeys.DARK_THEME] else null

            UserSession(
                token = preferences[PreferencesKeys.TOKEN] ?: "",
                userId = preferences[PreferencesKeys.USER_ID] ?: "",
                email = preferences[PreferencesKeys.EMAIL] ?: "",
                username = preferences[PreferencesKeys.USERNAME] ?: "",
                name = preferences[PreferencesKeys.NAME] ?: "",
                avatarUrl = preferences[PreferencesKeys.AVATAR_URL] ?: "",
                isVerified = preferences[PreferencesKeys.IS_VERIFIED] ?: false,
                isPrivate = preferences[PreferencesKeys.IS_PRIVATE] ?: false,
                darkTheme = darkTheme
            )
        }

    suspend fun saveSession(
        token: String,
        userId: String,
        email: String,
        username: String,
        name: String,
        avatarUrl: String = "",
        isVerified: Boolean = false,
        isPrivate: Boolean = false
    ) {
        context.sessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN] = token
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.EMAIL] = email
            preferences[PreferencesKeys.USERNAME] = username
            preferences[PreferencesKeys.NAME] = name
            preferences[PreferencesKeys.AVATAR_URL] = avatarUrl
            preferences[PreferencesKeys.IS_VERIFIED] = isVerified
            preferences[PreferencesKeys.IS_PRIVATE] = isPrivate
        }
    }

    suspend fun updateVerifiedStatus(isVerified: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VERIFIED] = isVerified
        }
    }

    suspend fun updateProfile(name: String, username: String, avatarUrl: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.NAME] = name
            preferences[PreferencesKeys.USERNAME] = username
            if (avatarUrl.isNotBlank()) {
                preferences[PreferencesKeys.AVATAR_URL] = avatarUrl
            }
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TOKEN)
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.EMAIL)
            preferences.remove(PreferencesKeys.USERNAME)
            preferences.remove(PreferencesKeys.NAME)
            preferences.remove(PreferencesKeys.AVATAR_URL)
            preferences.remove(PreferencesKeys.IS_VERIFIED)
            preferences.remove(PreferencesKeys.IS_PRIVATE)
        }
    }

    suspend fun setThemeMode(isDark: Boolean?) {
        context.sessionDataStore.edit { preferences ->
            if (isDark == null) {
                preferences[PreferencesKeys.HAS_SET_THEME] = false
                preferences.remove(PreferencesKeys.DARK_THEME)
            } else {
                preferences[PreferencesKeys.HAS_SET_THEME] = true
                preferences[PreferencesKeys.DARK_THEME] = isDark
            }
        }
    }
}
