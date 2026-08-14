package com.example

import android.app.Application
import com.example.core.database.AppDatabase
import com.example.core.datastore.SessionDataStore
import com.example.core.network.PocketBaseClient
import com.example.core.network.PocketBaseRealtimeManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.PostRepository
import com.example.data.repository.PostRepositoryImpl
import com.example.domain.usecase.auth.CheckVerificationStatusUseCase
import com.example.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.example.domain.usecase.auth.ConfirmVerificationUseCase
import com.example.domain.usecase.auth.LoginUseCase
import com.example.domain.usecase.auth.LogoutUseCase
import com.example.domain.usecase.auth.RegisterUseCase
import com.example.domain.usecase.auth.RequestPasswordResetUseCase
import com.example.domain.usecase.auth.RequestVerificationUseCase
import com.example.domain.usecase.post.AddCommentUseCase
import com.example.domain.usecase.post.CreatePostUseCase
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
import com.example.feature.auth.AuthViewModel
import com.example.feature.create.CreatePostViewModel
import com.example.feature.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForyouApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var sessionDataStore: SessionDataStore
        private set
    lateinit var pocketBaseClient: PocketBaseClient
        private set
    lateinit var realtimeManager: PocketBaseRealtimeManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var postRepository: PostRepository
        private set
    lateinit var chatRepository: com.example.data.repository.ChatRepository
        private set
    lateinit var searchRepository: com.example.data.repository.SearchRepository
        private set
    lateinit var notificationRepository: com.example.data.repository.NotificationRepository
        private set

    // Auth Use Cases
    lateinit var loginUseCase: LoginUseCase
    lateinit var registerUseCase: RegisterUseCase
    lateinit var logoutUseCase: LogoutUseCase
    lateinit var requestVerificationUseCase: RequestVerificationUseCase
    lateinit var confirmVerificationUseCase: ConfirmVerificationUseCase
    lateinit var checkVerificationStatusUseCase: CheckVerificationStatusUseCase
    lateinit var requestPasswordResetUseCase: RequestPasswordResetUseCase
    lateinit var confirmPasswordResetUseCase: ConfirmPasswordResetUseCase

    // Post & Feed Use Cases
    lateinit var getFeedUseCase: GetFeedUseCase
    lateinit var getStoriesUseCase: GetStoriesUseCase
    lateinit var createPostUseCase: CreatePostUseCase
    lateinit var updatePostUseCase: UpdatePostUseCase
    lateinit var deletePostUseCase: DeletePostUseCase
    lateinit var toggleLikePostUseCase: ToggleLikePostUseCase
    lateinit var toggleSavePostUseCase: ToggleSavePostUseCase
    lateinit var getCommentsUseCase: GetCommentsUseCase
    lateinit var addCommentUseCase: AddCommentUseCase
    lateinit var toggleLikeCommentUseCase: ToggleLikeCommentUseCase
    lateinit var deleteCommentUseCase: DeleteCommentUseCase
    lateinit var createStoryUseCase: CreateStoryUseCase
    lateinit var markStoryViewedUseCase: MarkStoryViewedUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        sessionDataStore = SessionDataStore(this)
        pocketBaseClient = PocketBaseClient(sessionDataStore = sessionDataStore)
        realtimeManager = PocketBaseRealtimeManager(pocketBaseClient)

        authRepository = AuthRepositoryImpl(pocketBaseClient, sessionDataStore)
        postRepository = PostRepositoryImpl(
            client = pocketBaseClient,
            postDao = database.postDao(),
            storyDao = database.storyDao(),
            commentDao = database.commentDao(),
            sessionDataStore = sessionDataStore
        )
        chatRepository = com.example.data.repository.ChatRepositoryImpl(
            client = pocketBaseClient,
            chatDao = database.chatDao(),
            realtimeManager = realtimeManager,
            sessionDataStore = sessionDataStore
        )
        searchRepository = com.example.data.repository.SearchRepositoryImpl(
            client = pocketBaseClient,
            sessionDataStore = sessionDataStore
        )
        notificationRepository = com.example.data.repository.NotificationRepositoryImpl(
            client = pocketBaseClient,
            realtimeManager = realtimeManager,
            sessionDataStore = sessionDataStore
        )

        // Init Auth Use Cases
        loginUseCase = LoginUseCase(authRepository)
        registerUseCase = RegisterUseCase(authRepository)
        logoutUseCase = LogoutUseCase(authRepository)
        requestVerificationUseCase = RequestVerificationUseCase(authRepository)
        confirmVerificationUseCase = ConfirmVerificationUseCase(authRepository)
        checkVerificationStatusUseCase = CheckVerificationStatusUseCase(authRepository)
        requestPasswordResetUseCase = RequestPasswordResetUseCase(authRepository)
        confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(authRepository)

        // Init Post Use Cases
        getFeedUseCase = GetFeedUseCase(postRepository)
        getStoriesUseCase = GetStoriesUseCase(postRepository)
        createPostUseCase = CreatePostUseCase(postRepository)
        updatePostUseCase = UpdatePostUseCase(postRepository)
        deletePostUseCase = DeletePostUseCase(postRepository)
        toggleLikePostUseCase = ToggleLikePostUseCase(postRepository)
        toggleSavePostUseCase = ToggleSavePostUseCase(postRepository)
        getCommentsUseCase = GetCommentsUseCase(postRepository)
        addCommentUseCase = AddCommentUseCase(postRepository)
        toggleLikeCommentUseCase = ToggleLikeCommentUseCase(postRepository)
        deleteCommentUseCase = DeleteCommentUseCase(postRepository)
        createStoryUseCase = CreateStoryUseCase(postRepository)
        markStoryViewedUseCase = MarkStoryViewedUseCase(postRepository)

        // Restore auth token on launch
        applicationScope.launch {
            val session = sessionDataStore.sessionFlow.first()
            if (session.isLoggedIn && session.token.isNotBlank()) {
                pocketBaseClient.setAuthToken(session.token)
            }
        }
    }

    fun provideAuthViewModel(): AuthViewModel {
        return AuthViewModel(
            loginUseCase = loginUseCase,
            registerUseCase = registerUseCase,
            logoutUseCase = logoutUseCase,
            requestVerificationUseCase = requestVerificationUseCase,
            confirmVerificationUseCase = confirmVerificationUseCase,
            checkVerificationStatusUseCase = checkVerificationStatusUseCase,
            requestPasswordResetUseCase = requestPasswordResetUseCase,
            confirmPasswordResetUseCase = confirmPasswordResetUseCase
        )
    }

    fun provideHomeViewModel(): HomeViewModel {
        return HomeViewModel(
            getFeedUseCase = getFeedUseCase,
            getStoriesUseCase = getStoriesUseCase,
            toggleLikePostUseCase = toggleLikePostUseCase,
            toggleSavePostUseCase = toggleSavePostUseCase,
            deletePostUseCase = deletePostUseCase,
            updatePostUseCase = updatePostUseCase,
            getCommentsUseCase = getCommentsUseCase,
            addCommentUseCase = addCommentUseCase,
            toggleLikeCommentUseCase = toggleLikeCommentUseCase,
            deleteCommentUseCase = deleteCommentUseCase,
            createStoryUseCase = createStoryUseCase,
            markStoryViewedUseCase = markStoryViewedUseCase
        )
    }

    fun provideCreatePostViewModel(): CreatePostViewModel {
        return CreatePostViewModel(createPostUseCase = createPostUseCase)
    }

    fun provideReelsViewModel(): com.example.feature.reels.ReelsViewModel {
        return com.example.feature.reels.ReelsViewModel(
            postRepository = postRepository,
            toggleLikePostUseCase = toggleLikePostUseCase,
            toggleSavePostUseCase = toggleSavePostUseCase,
            getCommentsUseCase = getCommentsUseCase,
            addCommentUseCase = addCommentUseCase,
            toggleLikeCommentUseCase = toggleLikeCommentUseCase,
            deleteCommentUseCase = deleteCommentUseCase
        )
    }

    fun provideChatViewModel(): com.example.feature.chat.ChatViewModel {
        return com.example.feature.chat.ChatViewModel(
            chatRepository = chatRepository,
            searchRepository = searchRepository
        )
    }

    fun provideSearchViewModel(): com.example.feature.search.SearchViewModel {
        return com.example.feature.search.SearchViewModel(
            searchRepository = searchRepository
        )
    }

    fun provideNotificationsViewModel(): com.example.feature.notifications.NotificationsViewModel {
        return com.example.feature.notifications.NotificationsViewModel(
            notificationRepository = notificationRepository
        )
    }

    companion object {
        lateinit var instance: ForyouApplication
            private set
    }
}
