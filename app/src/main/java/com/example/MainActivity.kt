package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.feature.navigation.ForyouNavGraph
import com.example.ui.theme.ForyouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ForyouApplication
        val authViewModel = app.provideAuthViewModel()
        val homeViewModel = app.provideHomeViewModel()
        val createPostViewModel = app.provideCreatePostViewModel()
        val reelsViewModel = app.provideReelsViewModel()
        val chatViewModel = app.provideChatViewModel()
        val searchViewModel = app.provideSearchViewModel()
        val notificationsViewModel = app.provideNotificationsViewModel()

        setContent {
            val session by app.sessionDataStore.sessionFlow.collectAsState(initial = null)
            val isDarkTheme = session?.darkTheme ?: isSystemInDarkTheme()

            ForyouTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (session != null) {
                        ForyouNavGraph(
                            authViewModel = authViewModel,
                            homeViewModel = homeViewModel,
                            createPostViewModel = createPostViewModel,
                            reelsViewModel = reelsViewModel,
                            chatViewModel = chatViewModel,
                            searchViewModel = searchViewModel,
                            notificationsViewModel = notificationsViewModel,
                            postRepository = app.postRepository,
                            sessionDataStore = app.sessionDataStore,
                            isLoggedIn = session?.isLoggedIn == true
                        )
                    }
                }
            }
        }
    }
}
