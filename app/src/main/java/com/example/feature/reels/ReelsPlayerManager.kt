package com.example.feature.reels

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, lifecycle-aware Player Pool for Reels.
 * Manages active and preloaded ExoPlayer instances to minimize memory usage
 * and eliminate buffering latency during vertical swiping.
 */
@OptIn(UnstableApi::class)
class ReelsPlayerManager(private val context: Context) {

    private val playerPool = ConcurrentHashMap<String, ExoPlayer>()
    private var activeReelId: String? = null
    var isMuted: Boolean = false
        private set

    /**
     * Get or create an ExoPlayer for a specific reel ID.
     */
    fun getPlayer(reelId: String, videoUrl: String): ExoPlayer {
        val existing = playerPool[reelId]
        if (existing != null) {
            existing.volume = if (isMuted) 0f else 1f
            return existing
        }

        // Clean up excess players to keep pool size <= 3
        if (playerPool.size >= 3) {
            val toRemove = playerPool.keys.firstOrNull { it != activeReelId }
            toRemove?.let { removeAndReleasePlayer(it) }
        }

        val player = ExoPlayer.Builder(context)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = if (isMuted) 0f else 1f
                prepare()
            }

        playerPool[reelId] = player
        return player
    }

    /**
     * Preload video for the upcoming reel to ensure instant playback.
     */
    fun preload(reelId: String, videoUrl: String) {
        if (!playerPool.containsKey(reelId) && videoUrl.isNotBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = false
                prepare()
            }
            playerPool[reelId] = player
        }
    }

    /**
     * Set active reel and play it while pausing all others.
     */
    fun playReel(reelId: String) {
        activeReelId = reelId
        playerPool.forEach { (id, player) ->
            if (id == reelId) {
                player.volume = if (isMuted) 0f else 1f
                player.playWhenReady = true
                player.play()
            } else {
                player.playWhenReady = false
                player.pause()
            }
        }
    }

    fun pauseAll() {
        playerPool.values.forEach { player ->
            player.playWhenReady = false
            player.pause()
        }
    }

    fun resumeActive() {
        activeReelId?.let { id ->
            playerPool[id]?.let { player ->
                player.volume = if (isMuted) 0f else 1f
                player.playWhenReady = true
                player.play()
            }
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        playerPool.values.forEach { player ->
            player.volume = if (isMuted) 0f else 1f
        }
        return isMuted
    }

    fun removeAndReleasePlayer(reelId: String) {
        playerPool.remove(reelId)?.let { player ->
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    fun releaseAll() {
        playerPool.forEach { (_, player) ->
            player.stop()
            player.clearMediaItems()
            player.release()
        }
        playerPool.clear()
        activeReelId = null
    }
}
