package com.example.data.repository

import com.example.core.datastore.SessionDataStore
import com.example.core.datastore.UserSession
import com.example.core.network.NetworkResult
import com.example.core.network.PBAuthResponse
import com.example.core.network.PBUserRecord
import com.example.core.network.PocketBaseClient
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val currentSession: Flow<UserSession>
    val currentUser: Flow<User?>

    suspend fun login(identity: String, password: String): NetworkResult<User>
    suspend fun register(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String,
        name: String
    ): NetworkResult<User>
    suspend fun requestVerification(email: String): NetworkResult<Boolean>
    suspend fun confirmVerification(token: String): NetworkResult<Boolean>
    suspend fun checkVerificationStatus(email: String? = null): NetworkResult<Boolean>
    suspend fun requestPasswordReset(email: String): NetworkResult<Boolean>
    suspend fun confirmPasswordReset(token: String, password: String, passwordConfirm: String): NetworkResult<Boolean>
    suspend fun refreshAuth(): NetworkResult<User>
    suspend fun logout()
}

class AuthRepositoryImpl(
    private val pocketBaseClient: PocketBaseClient,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    override val currentSession: Flow<UserSession> = sessionDataStore.sessionFlow

    override val currentUser: Flow<User?> = currentSession.map { session ->
        if (session.isLoggedIn) {
            User(
                id = session.userId,
                username = session.username,
                email = session.email,
                name = session.name,
                avatarUrl = session.avatarUrl,
                isVerified = session.isVerified,
                isPrivate = session.isPrivate
            )
        } else {
            null
        }
    }

    override suspend fun login(identity: String, password: String): NetworkResult<User> {
        return when (val result = pocketBaseClient.authWithPassword(identity, password)) {
            is NetworkResult.Success -> {
                val record = result.data.record
                val avatarUrl = if (record.avatar.isNotBlank()) {
                    pocketBaseClient.getFileUrl("users", record.id, record.avatar)
                } else ""

                sessionDataStore.saveSession(
                    token = result.data.token,
                    userId = record.id,
                    email = record.email,
                    username = record.username,
                    name = record.name.ifBlank { record.username },
                    avatarUrl = avatarUrl,
                    isVerified = record.verified,
                    isPrivate = record.isPrivate
                )

                NetworkResult.Success(
                    User(
                        id = record.id,
                        username = record.username,
                        email = record.email,
                        name = record.name,
                        avatarUrl = avatarUrl,
                        bio = record.bio,
                        isVerified = record.verified,
                        isPrivate = record.isPrivate,
                        followersCount = record.followersCount,
                        followingCount = record.followingCount,
                        postsCount = record.postsCount
                    )
                )
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun register(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String,
        name: String
    ): NetworkResult<User> {
        return when (val result = pocketBaseClient.register(email, username, password, passwordConfirm, name)) {
            is NetworkResult.Success -> {
                // Trigger verification email automatically
                pocketBaseClient.requestVerification(email)
                NetworkResult.Success(
                    User(
                        id = result.data.id,
                        username = result.data.username,
                        email = result.data.email,
                        name = result.data.name,
                        isVerified = result.data.verified
                    )
                )
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun requestVerification(email: String): NetworkResult<Boolean> {
        return pocketBaseClient.requestVerification(email)
    }

    override suspend fun confirmVerification(token: String): NetworkResult<Boolean> {
        return when (val result = pocketBaseClient.confirmVerification(token)) {
            is NetworkResult.Success -> {
                sessionDataStore.updateVerifiedStatus(true)
                NetworkResult.Success(true)
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun checkVerificationStatus(email: String?): NetworkResult<Boolean> {
        // First try refreshing auth if session has token
        val refreshResult = pocketBaseClient.authRefresh()
        if (refreshResult is NetworkResult.Success) {
            val isVerified = refreshResult.data.record.verified
            sessionDataStore.updateVerifiedStatus(isVerified)
            return NetworkResult.Success(isVerified)
        }

        // If email is provided, query by email
        if (!email.isNullOrBlank()) {
            val listResult = pocketBaseClient.getList(
                collectionName = "users",
                filter = "email = '$email'",
                perPage = 1,
                itemType = PBUserRecord::class.java
            )
            if (listResult is NetworkResult.Success) {
                val user = listResult.data.items.firstOrNull()
                if (user != null) {
                    if (user.verified) {
                        sessionDataStore.updateVerifiedStatus(true)
                    }
                    return NetworkResult.Success(user.verified)
                }
            }
        }

        return NetworkResult.Error("Unable to verify status at this moment. Please check back shortly.")
    }

    override suspend fun requestPasswordReset(email: String): NetworkResult<Boolean> {
        return pocketBaseClient.requestPasswordReset(email)
    }

    override suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): NetworkResult<Boolean> {
        return pocketBaseClient.confirmPasswordReset(token, password, passwordConfirm)
    }

    override suspend fun refreshAuth(): NetworkResult<User> {
        return when (val result = pocketBaseClient.authRefresh()) {
            is NetworkResult.Success -> {
                val record = result.data.record
                val avatarUrl = if (record.avatar.isNotBlank()) {
                    pocketBaseClient.getFileUrl("users", record.id, record.avatar)
                } else ""

                sessionDataStore.saveSession(
                    token = result.data.token,
                    userId = record.id,
                    email = record.email,
                    username = record.username,
                    name = record.name.ifBlank { record.username },
                    avatarUrl = avatarUrl,
                    isVerified = record.verified,
                    isPrivate = record.isPrivate
                )

                NetworkResult.Success(
                    User(
                        id = record.id,
                        username = record.username,
                        email = record.email,
                        name = record.name,
                        avatarUrl = avatarUrl,
                        isVerified = record.verified
                    )
                )
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun logout() {
        pocketBaseClient.setAuthToken(null)
        sessionDataStore.clearSession()
    }
}
