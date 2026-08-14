package com.example.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.ForyouButton
import com.example.feature.home.components.CommentsBottomSheet
import com.example.feature.home.components.CreateStoryDialog
import com.example.feature.home.components.StoriesRow
import com.example.feature.home.components.StoryViewerDialog
import com.example.ui.theme.ForyouCyan
import com.example.ui.theme.ForyouPink
import com.example.ui.theme.ForyouViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToDirectMessages: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: (userId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val posts by viewModel.feedPosts.collectAsState()
    val stories by viewModel.storiesList.collectAsState()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Infinite scroll pagination detection
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMorePages && !uiState.isLoadingMore && !uiState.isLoadingFeed) {
            viewModel.loadNextPage()
        }
    }

    // Display user feedback snackbar
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "foryou",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            brush = Brush.linearGradient(listOf(ForyouPink, ForyouViolet, ForyouCyan))
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag("refresh_feed_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Feed",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNavigateToDirectMessages,
                        modifier = Modifier.testTag("messages_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Direct Messages",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Refreshing Progress Bar
                AnimatedVisibility(visible = uiState.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.isLoadingFeed && posts.isEmpty()) {
                    // Loading Skeleton State
                    FeedSkeleton()
                } else if (posts.isEmpty() && !uiState.isLoadingFeed) {
                    // Empty Feed State with Stories Header
                    Column(modifier = Modifier.fillMaxSize()) {
                        StoriesRow(
                            stories = stories.ifEmpty { uiState.stories },
                            onAddStoryClick = { viewModel.openCreateStory() },
                            onStoryClick = { index -> viewModel.openStoryViewer(index) }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("✨", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Welcome to your Home Feed!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Share your first photo or story to get started.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                ForyouButton(
                                    text = "Create First Post",
                                    onClick = onNavigateToCreatePost,
                                    testTag = "create_first_post_button"
                                )
                            }
                        }
                    }
                } else {
                    // Paginated Feed List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("feed_lazy_column"),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stories Row Header
                        item(key = "stories_row_header") {
                            Column {
                                StoriesRow(
                                    stories = stories.ifEmpty { uiState.stories },
                                    onAddStoryClick = { viewModel.openCreateStory() },
                                    onStoryClick = { index -> viewModel.openStoryViewer(index) }
                                )
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Feed Posts
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onLikeClick = { viewModel.onLikePost(post) },
                                onCommentClick = { viewModel.openCommentsForPost(post) },
                                onShareClick = { /* Native share handled in PostCard */ },
                                onSaveClick = { viewModel.onSavePost(post) },
                                onAuthorClick = { onNavigateToProfile(post.author.id) },
                                onEditPost = { viewModel.onStartEditPost(it) },
                                onDeletePost = { viewModel.onDeletePost(it) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Pagination Loading Footer
                        item(key = "pagination_footer") {
                            if (uiState.isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (!uiState.hasMorePages && posts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "You're all caught up ✨",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Comments Bottom Sheet
        uiState.activeCommentsPost?.let { activePost ->
            CommentsBottomSheet(
                post = activePost,
                comments = uiState.comments,
                isLoading = uiState.isLoadingComments,
                replyingToComment = uiState.replyingToComment,
                isSubmitting = uiState.isSubmittingComment,
                onDismiss = { viewModel.closeComments() },
                onAddComment = { viewModel.onAddComment(it) },
                onReplyToComment = { viewModel.onReplyToComment(it) },
                onCancelReply = { viewModel.onCancelReply() },
                onLikeComment = { viewModel.onLikeComment(it) },
                onDeleteComment = { viewModel.onDeleteComment(it) }
            )
        }

        // Active Story Viewer Modal
        if (uiState.isViewingStory && uiState.activeStoryIndex != null) {
            val storyList = stories.ifEmpty { uiState.stories }
            StoryViewerDialog(
                stories = storyList,
                initialIndex = uiState.activeStoryIndex!!,
                onClose = { viewModel.closeStoryViewer() },
                onStoryCompleted = { viewModel.closeStoryViewer() }
            )
        }

        // Create Story Modal
        if (uiState.isCreatingStory) {
            CreateStoryDialog(
                onDismiss = { viewModel.closeCreateStory() },
                onPublish = { caption -> viewModel.createStory(caption) }
            )
        }

        // Edit Post Dialog
        uiState.editingPost?.let { postToEdit ->
            EditPostDialog(
                post = postToEdit,
                onDismiss = { viewModel.onDismissEditPost() },
                onSave = { caption, location, tags ->
                    viewModel.onSaveEditedPost(postToEdit.id, caption, location, tags)
                }
            )
        }
    }
}

@Composable
fun FeedSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Skeleton Stories Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
        }

        // Skeleton Post Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        )
    }
}
