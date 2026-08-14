package com.example.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.datastore.SessionDataStore
import com.example.core.designsystem.BottomNavDestination
import com.example.core.designsystem.ForyouBottomBar
import com.example.data.repository.PostRepository
import com.example.feature.auth.AuthViewModel
import com.example.feature.auth.EmailVerificationScreen
import com.example.feature.auth.ForgotPasswordScreen
import com.example.feature.auth.LoginScreen
import com.example.feature.auth.RegisterScreen
import com.example.feature.chat.ChatListScreen
import com.example.feature.chat.ChatViewModel
import com.example.feature.chat.DirectChatScreen
import com.example.feature.create.CreatePostScreen
import com.example.feature.create.CreatePostViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.notifications.NotificationsScreen
import com.example.feature.notifications.NotificationsViewModel
import com.example.feature.profile.ProfileScreen
import com.example.feature.reels.ReelsScreen
import com.example.feature.search.SearchScreen
import com.example.feature.search.SearchViewModel

object NavRoutes {
    const val LOGIN = "auth_login"
    const val REGISTER = "auth_register"
    const val VERIFY_EMAIL = "auth_verify/{email}"
    const val FORGOT_PASSWORD = "auth_forgot_password"
    const val MAIN = "main"
    const val NOTIFICATIONS = "notifications"
    const val CHAT_LIST = "chat_list"
    const val DIRECT_CHAT = "direct_chat"

    fun verifyEmail(email: String) = "auth_verify/${android.net.Uri.encode(email)}"
}

@Composable
fun ForyouNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    createPostViewModel: CreatePostViewModel,
    reelsViewModel: com.example.feature.reels.ReelsViewModel,
    chatViewModel: ChatViewModel,
    searchViewModel: SearchViewModel,
    notificationsViewModel: NotificationsViewModel,
    postRepository: PostRepository,
    sessionDataStore: SessionDataStore,
    isLoggedIn: Boolean
) {
    val startDestination = if (isLoggedIn) NavRoutes.MAIN else NavRoutes.LOGIN
    val chatUiState by chatViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(NavRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.MAIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegistrationSuccess = { email ->
                    navController.navigate(NavRoutes.verifyEmail(email))
                }
            )
        }

        composable(
            route = NavRoutes.VERIFY_EMAIL,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailVerificationScreen(
                email = email,
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPasswordResetSuccess = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.MAIN) {
            MainContainer(
                homeViewModel = homeViewModel,
                createPostViewModel = createPostViewModel,
                reelsViewModel = reelsViewModel,
                searchViewModel = searchViewModel,
                postRepository = postRepository,
                sessionDataStore = sessionDataStore,
                authViewModel = authViewModel,
                onNotificationsClick = { navController.navigate(NavRoutes.NOTIFICATIONS) },
                onDirectMessagesClick = { navController.navigate(NavRoutes.CHAT_LIST) },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.CHAT_LIST) {
            ChatListScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onOpenConversation = { conv ->
                    navController.navigate(NavRoutes.DIRECT_CHAT)
                }
            )
        }

        composable(NavRoutes.DIRECT_CHAT) {
            val activeConv = chatUiState.activeConversation
            if (activeConv != null) {
                DirectChatScreen(
                    conversation = activeConv,
                    viewModel = chatViewModel,
                    onBack = {
                        chatViewModel.closeActiveConversation()
                        navController.popBackStack()
                    }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}

@Composable
fun MainContainer(
    homeViewModel: HomeViewModel,
    createPostViewModel: CreatePostViewModel,
    reelsViewModel: com.example.feature.reels.ReelsViewModel,
    searchViewModel: SearchViewModel,
    postRepository: PostRepository,
    sessionDataStore: SessionDataStore,
    authViewModel: AuthViewModel,
    onNotificationsClick: () -> Unit,
    onDirectMessagesClick: () -> Unit,
    onLogout: () -> Unit
) {
    var currentTab by rememberSaveable { mutableStateOf(BottomNavDestination.HOME.route) }

    Scaffold(
        bottomBar = {
            ForyouBottomBar(
                currentRoute = currentTab,
                onNavigateToDestination = { destination ->
                    currentTab = destination.route
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentTab) {
                BottomNavDestination.HOME.route -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToCreatePost = { currentTab = BottomNavDestination.CREATE.route },
                        onNavigateToDirectMessages = onDirectMessagesClick,
                        onNavigateToNotifications = onNotificationsClick,
                        onNavigateToProfile = { /* Navigate to profile */ }
                    )
                }
                BottomNavDestination.SEARCH.route -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onUserClick = {}
                    )
                }
                BottomNavDestination.CREATE.route -> {
                    CreatePostScreen(
                        viewModel = createPostViewModel,
                        onNavigateBack = { currentTab = BottomNavDestination.HOME.route },
                        onPostCreated = {
                            currentTab = BottomNavDestination.HOME.route
                            homeViewModel.refresh()
                        }
                    )
                }
                BottomNavDestination.REELS.route -> {
                    ReelsScreen(viewModel = reelsViewModel)
                }
                BottomNavDestination.PROFILE.route -> {
                    ProfileScreen(
                        sessionDataStore = sessionDataStore,
                        authViewModel = authViewModel,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
