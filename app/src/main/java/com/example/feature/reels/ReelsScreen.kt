@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.example.feature.reels

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.core.designsystem.EmptyState
import com.example.core.designsystem.UserAvatar
import com.example.domain.model.Post
import com.example.feature.home.components.CommentsBottomSheet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ForyouCyan
import com.example.ui.theme.ForyouPink
import com.example.ui.theme.ForyouViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToProfile: (userId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize player manager
    val playerManager = remember { ReelsPlayerManager(context) }

    // Lifecycle observer to pause/resume video playback
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> playerManager.pauseAll()
                Lifecycle.Event.ON_RESUME -> playerManager.resumeActive()
                Lifecycle.Event.ON_DESTROY -> playerManager.releaseAll()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.releaseAll()
        }
    }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.reels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ForyouPink,
                        modifier = Modifier.testTag("reels_loading_indicator")
                    )
                }
            } else if (uiState.reels.isEmpty()) {
                EmptyState(
                    title = "No Reels Yet",
                    description = "Be the first creator to share a vertical video on Foryou!",
                    icon = Icons.Default.Movie,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { uiState.reels.size })

                // Autoplay active reel and preload adjacent
                LaunchedEffect(pagerState.currentPage, uiState.reels) {
                    if (uiState.reels.isNotEmpty() && pagerState.currentPage in uiState.reels.indices) {
                        val currentReel = uiState.reels[pagerState.currentPage]
                        val videoUrl = currentReel.mediaUrls.firstOrNull() ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                        playerManager.getPlayer(currentReel.id, videoUrl)
                        playerManager.playReel(currentReel.id)

                        // Preload next reel
                        if (pagerState.currentPage + 1 < uiState.reels.size) {
                            val nextReel = uiState.reels[pagerState.currentPage + 1]
                            val nextUrl = nextReel.mediaUrls.firstOrNull() ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
                            playerManager.preload(nextReel.id, nextUrl)
                        }
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("reels_vertical_pager"),
                    key = { index -> uiState.reels.getOrNull(index)?.id ?: index.toString() }
                ) { page ->
                    val reel = uiState.reels[page]
                    val isCurrentPage = pagerState.currentPage == page

                    ReelPageItem(
                        reel = reel,
                        isCurrentPage = isCurrentPage,
                        playerManager = playerManager,
                        isFollowing = uiState.followedUserIds.contains(reel.author.id),
                        onLike = { viewModel.toggleLike(reel) },
                        onComment = { viewModel.openComments(reel) },
                        onShare = { shareReel(context, reel) },
                        onSave = { viewModel.toggleSave(reel) },
                        onToggleFollow = { viewModel.toggleFollow(reel.author.id) },
                        onToggleMute = { viewModel.toggleMute(playerManager) },
                        onAuthorClick = { onNavigateToProfile(reel.author.id) }
                    )
                }
            }

            // Top Header: Reels Title & Volume Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )

                IconButton(
                    onClick = { viewModel.toggleMute(playerManager) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("reels_volume_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )

            // Comments Bottom Sheet
            uiState.activeCommentsReel?.let { activeReel ->
                CommentsBottomSheet(
                    post = activeReel,
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
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ReelPageItem(
    reel: Post,
    isCurrentPage: Boolean,
    playerManager: ReelsPlayerManager,
    isFollowing: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleMute: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIcon by remember { mutableStateOf(false) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    val videoUrl = remember(reel) {
        reel.mediaUrls.firstOrNull() ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    }

    val player = remember(reel.id, videoUrl) {
        playerManager.getPlayer(reel.id, videoUrl)
    }

    // Audio disc rotating animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!reel.isLiked) {
                            onLike()
                        }
                        showHeartAnimation = true
                        coroutineScope.launch {
                            delay(900)
                            showHeartAnimation = false
                        }
                    },
                    onTap = {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.play()
                            isPlaying = true
                        }
                        showPlayPauseIcon = true
                        coroutineScope.launch {
                            delay(600)
                            showPlayPauseIcon = false
                        }
                    }
                )
            }
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top & Bottom Gradient Vignettes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Play/Pause Center Indicator Overlay
        AnimatedVisibility(
            visible = showPlayPauseIcon,
            enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Explosive Heart Animation on Double Tap
        AnimatedVisibility(
            visible = showHeartAnimation,
            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(),
            exit = scaleOut(tween(300)) + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ForyouPink,
                modifier = Modifier.size(110.dp)
            )
        }

        // Bottom Left Content: Author Info, Follow Button, Caption, Audio Track
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, end = 8.dp, bottom = 84.dp)
        ) {
            // Author Row & Follow Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onAuthorClick)
            ) {
                UserAvatar(
                    avatarUrl = reel.author.avatarUrl,
                    name = reel.author.username,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "@${reel.author.username.ifBlank { "creator" }}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Follow / Following Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (isFollowing) Modifier.background(Color.White.copy(alpha = 0.2f))
                            else Modifier.background(Brush.horizontalGradient(listOf(ForyouPink, ForyouViolet)))
                        )
                        .clickable(onClick = onToggleFollow)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("reel_follow_button_${reel.author.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFollowing) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption
            Text(
                text = reel.caption,
                color = Color.White,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Location tag if present
            if (reel.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = reel.location,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Track Marquee
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Audio",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Original Audio - @${reel.author.username.ifBlank { "foryou_sound" }}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right Side Interaction Column (Like, Comment, Share, Save, Music Disc)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reel_like_button")
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Reel",
                        tint = if (reel.isLiked) ForyouPink else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = formatCount(reel.likesCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comment Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onComment,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reel_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = formatCount(reel.commentsCount),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share Button
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("reel_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Bookmark / Save Button
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("reel_save_button")
            ) {
                Icon(
                    imageVector = if (reel.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Save Reel",
                    tint = if (reel.isSaved) ForyouViolet else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Rotating Music Disc
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .rotate(discRotation),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    avatarUrl = reel.author.avatarUrl,
                    name = reel.author.username,
                    size = 24.dp
                )
            }
        }
    }
}

private fun shareReel(context: Context, reel: Post) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out this awesome Reel on Foryou by @${reel.author.username}: ${reel.caption.take(80)}... https://foryou.app/reels/${reel.id}"
        )
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Reel via"))
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
