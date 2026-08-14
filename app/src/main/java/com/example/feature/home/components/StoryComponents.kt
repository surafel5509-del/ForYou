package com.example.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.designsystem.ForyouButton
import com.example.core.designsystem.UserAvatar
import com.example.domain.model.Story
import com.example.domain.model.User
import com.example.ui.theme.ForyouCyan
import com.example.ui.theme.ForyouPink
import com.example.ui.theme.ForyouViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FloatingReaction(
    val id: Long,
    val emoji: String,
    val initialX: Float = (0.2f + Math.random().toFloat() * 0.6f)
)

@Composable
fun StoriesRow(
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val storyBrush = Brush.linearGradient(listOf(ForyouPink, ForyouViolet, ForyouCyan))

    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("stories_row")
    ) {
        // "Add Story" / Your Story item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .testTag("add_story_button")
                    .clickable(onClick = onAddStoryClick)
            ) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(
                            avatarUrl = "",
                            name = "You",
                            size = 60.dp
                        )
                    }

                    // Plus Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(storyBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your Story",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        // Creator Stories
        itemsIndexed(stories, key = { _, story -> story.id }) { index, story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .testTag("story_item_${story.id}")
                    .clickable { onStoryClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .then(
                            if (story.isViewed) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                            } else {
                                Modifier.border(2.5.dp, storyBrush, CircleShape)
                            }
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = story.author.avatarUrl,
                        name = story.author.name.ifBlank { story.author.username },
                        size = 58.dp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.author.username.take(9),
                    fontSize = 11.sp,
                    fontWeight = if (story.isViewed) FontWeight.Normal else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int,
    onClose: () -> Unit,
    onStoryCompleted: () -> Unit,
    onMarkViewed: (storyId: String) -> Unit = {}
) {
    if (stories.isEmpty() || initialIndex !in stories.indices) return

    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    var isPaused by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var replySentNotice by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }

    val reactionsList = remember { mutableStateListOf<FloatingReaction>() }
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val currentStory = stories[currentIndex]

    // Mark current story as viewed
    LaunchedEffect(currentStory.id) {
        onMarkViewed(currentStory.id)
    }

    // Story Progress Timer
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
            // Progress completed -> go to next story or close
            if (currentIndex < stories.lastIndex) {
                currentIndex++
            } else {
                onStoryCompleted()
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .pointerInput(currentIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.32f) {
                                    // Previous Story
                                    if (currentIndex > 0) currentIndex--
                                } else {
                                    // Next Story
                                    if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                                }
                            }
                        )
                    }
            ) {
                // Story Media or Styled Visual Gradient Canvas
                if (currentStory.mediaUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentStory.mediaUrl,
                        contentDescription = "Story Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Creative Gradient Card Story
                    val gradients = listOf(
                        Brush.verticalGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))),
                        Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))),
                        Brush.verticalGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
                        Brush.verticalGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
                        Brush.verticalGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
                    )
                    val gradient = gradients[currentIndex % gradients.size]

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradient)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentStory.caption.ifBlank { "✨ Living the moment! #foryou" },
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }

                // Top & Bottom Vignettes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Top Segmented Progress Indicators & Author Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Multi-Segmented Progress Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stories.forEachIndexed { idx, _ ->
                            val segmentProgress = when {
                                idx < currentIndex -> 1f
                                idx == currentIndex -> progress.value
                                else -> 0f
                            }
                            LinearProgressIndicator(
                                progress = { segmentProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Author Header Row with Expiration Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                avatarUrl = currentStory.author.avatarUrl,
                                name = currentStory.author.name.ifBlank { currentStory.author.username },
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = currentStory.author.username.ifBlank { "foryou_creator" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Expiration",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Expires in 22h • 2h ago",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Views button (if mine or shows views count)
                            if (currentStory.isMine) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { showViewersSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Viewers",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${currentStory.viewsCount.coerceAtLeast(1)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("close_story_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Story",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Ascending Floating Reaction Emojis Animation
                reactionsList.forEach { reaction ->
                    FloatingReactionItem(reaction = reaction)
                }

                // Reply sent toast notice
                AnimatedVisibility(
                    visible = replySentNotice,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, ForyouPink, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Reply sent to @${currentStory.author.username} 💌",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Bottom Quick Reactions Bar & Reply Field
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Emoji Reactions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val emojis = listOf("❤️", "🔥", "😂", "😮", "👏", "🎉", "💯")
                        emojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable {
                                        val newReaction = FloatingReaction(id = System.currentTimeMillis(), emoji = emoji)
                                        reactionsList.add(newReaction)
                                        coroutineScope.launch {
                                            delay(1600)
                                            reactionsList.remove(newReaction)
                                        }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Reply Text Field + Like Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Send reply...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("story_reply_input"),
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(alpha = 0.8f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            trailingIcon = {
                                if (replyText.isNotBlank()) {
                                    IconButton(onClick = {
                                        replyText = ""
                                        replySentNotice = true
                                        coroutineScope.launch {
                                            delay(2000)
                                            replySentNotice = false
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                isLiked = !isLiked
                                if (isLiked) {
                                    val heartReaction = FloatingReaction(id = System.currentTimeMillis(), emoji = "❤️")
                                    reactionsList.add(heartReaction)
                                    coroutineScope.launch {
                                        delay(1600)
                                        reactionsList.remove(heartReaction)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .testTag("story_like_button")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like Story",
                                tint = if (isLiked) ForyouPink else Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Story Viewers Modal Bottom Sheet
    if (showViewersSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Story Viewers (${currentStory.viewsCount.coerceAtLeast(4)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                val sampleViewers = listOf(
                    User(id = "v1", username = "aurora_vibes", name = "Aurora Skye", isVerified = true),
                    User(id = "v2", username = "chef_marco", name = "Marco Pierre"),
                    User(id = "v3", username = "neo_motion", name = "Neo Visuals"),
                    User(id = "v4", username = "clara_fitness", name = "Clara Strong")
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sampleViewers) { viewer ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(avatarUrl = viewer.avatarUrl, name = viewer.name, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = viewer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "@${viewer.username}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text("❤️", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingReactionItem(reaction: FloatingReaction) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(reaction.id) {
        launch {
            offsetY.animateTo(
                targetValue = -350f,
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
        }
        launch {
            delay(800)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 90.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = reaction.emoji,
            fontSize = 32.sp,
            modifier = Modifier
                .padding(start = (reaction.initialX * 280).dp)
                .align(Alignment.BottomStart)
                .pointerInput(Unit) {}
        )
    }
}

@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String) -> Unit
) {
    var storyMode by remember { mutableIntStateOf(0) } // 0: Text Gradient, 1: Media Photo/Video
    var storyCaption by remember { mutableStateOf("") }
    var mediaUrlInput by remember { mutableStateOf("") }

    val gradientColors = listOf(
        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)),
        listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899)),
        listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF9333EA)),
        listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF6366F1))
    )
    var selectedGradientIndex by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                    Text(
                        text = "Create Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ForyouButton(
                        text = "Share",
                        onClick = { onPublish(storyCaption) },
                        enabled = storyCaption.isNotBlank() || mediaUrlInput.isNotBlank(),
                        testTag = "share_story_button",
                        modifier = Modifier.height(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs (Text / Photo)
                TabRow(
                    selectedTabIndex = storyMode,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = storyMode == 0,
                        onClick = { storyMode = 0 },
                        text = { Text("Gradient Canvas") }
                    )
                    Tab(
                        selected = storyMode == 1,
                        onClick = { storyMode = 1 },
                        text = { Text("Media Photo") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Story Canvas Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (storyMode == 0) {
                                Modifier.background(Brush.verticalGradient(gradientColors[selectedGradientIndex]))
                            } else {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            }
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (storyMode == 0) {
                        OutlinedTextField(
                            value = storyCaption,
                            onValueChange = { storyCaption = it },
                            placeholder = {
                                Text(
                                    "Type your story...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("story_caption_input"),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val mediaPresets = listOf(
                                "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800" to "Beach Sunset",
                                "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=800" to "Tokyo Night",
                                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800" to "Coffee Moments",
                                "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800" to "Nature Escape"
                            )

                            if (mediaUrlInput.isNotBlank()) {
                                AsyncImage(
                                    model = mediaUrlInput,
                                    contentDescription = "Media Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Add Media",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "Choose a Media Template or Photo",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Preset selection chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(mediaPresets) { (url, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (mediaUrlInput == url) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.clickable { mediaUrlInput = url }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = storyCaption,
                                onValueChange = { storyCaption = it },
                                placeholder = { Text("Add a caption...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expiration Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stories automatically expire after 24 hours",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (storyMode == 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    // Color Theme Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        gradientColors.forEachIndexed { index, colors ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(colors))
                                    .clickable { selectedGradientIndex = index }
                                    .then(
                                        if (selectedGradientIndex == index) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
