package com.nick1234design.onntube4k

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var qualityButton: TextView
    private var maxHeight = 1080
    private var sponsorSegments: List<SponsorSegment> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private val skipTask = object : Runnable {
        override fun run() {
            val current = player?.currentPosition?.div(1000.0) ?: 0.0
            val segment = sponsorSegments.firstOrNull { current >= it.startSeconds && current < it.endSeconds }
            if (segment != null) player?.seekTo((segment.endSeconds * 1000).toLong())
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        maxHeight = intent.getIntExtra(EXTRA_MAX_HEIGHT, 1080)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        root.addView(playerView, FrameLayout.LayoutParams(-1, -1))
        qualityButton = TextView(this).apply {
            text = "QUALITY"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(22, 14, 22, 14)
            setBackgroundColor(0xCC202124.toInt())
            isFocusable = true
            setOnClickListener { showQualityMenu() }
        }
        root.addView(qualityButton, FrameLayout.LayoutParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 22
            rightMargin = 22
        })
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val directUrl = intent.getStringExtra(EXTRA_URL)
        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        if (!directUrl.isNullOrBlank()) playUrl(directUrl)
        else if (!videoId.isNullOrBlank()) resolveAndPlay(videoId)
    }

    private fun resolveAndPlay(videoId: String) {
        qualityButton.text = "LOADING " + maxHeight + "P"
        Thread {
            try {
                val stream = NativeYoutubeExtractor.resolve(videoId, maxHeight)
                val segments = runCatching { SponsorBlockClient.getSegments(videoId) }.getOrDefault(emptyList())
                runOnUiThread {
                    sponsorSegments = segments
                    qualityButton.text = "QUALITY • " + stream.height + "P"
                    playUrl(stream.url)
                    handler.removeCallbacks(skipTask)
                    handler.post(skipTask)
                }
            } catch (error: Exception) {
                runOnUiThread {
                    qualityButton.text = "PLAYBACK ERROR"
                    AlertDialog.Builder(this).setTitle("Couldn't play this video")
                        .setMessage(error.message ?: "The video stream could not be extracted.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }

    private fun playUrl(url: String) {
        player?.release()
        player = ExoPlayer.Builder(this).build().also {
            playerView.player = it
            it.setMediaItem(MediaItem.fromUri(url))
            it.prepare()
            it.playWhenReady = true
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) it.seekTo(0)
                }
            })
        }
    }

    private fun showQualityMenu() {
        val options = arrayOf("1080p", "720p", "480p", "360p")
        val heights = intArrayOf(1080, 720, 480, 360)
        AlertDialog.Builder(this).setTitle("Video quality")
            .setSingleChoiceItems(options, heights.indexOf(maxHeight).coerceAtLeast(0)) { dialog, which ->
                maxHeight = heights[which]
                intent.putExtra(EXTRA_MAX_HEIGHT, maxHeight)
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
                if (!videoId.isNullOrBlank()) resolveAndPlay(videoId)
                dialog.dismiss()
            }.setNegativeButton("CANCEL", null).show()
    }

    override fun onStop() {
        handler.removeCallbacks(skipTask)
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    companion object {
        const val EXTRA_URL = "com.nick1234design.onntube4k.STREAM_URL"
        const val EXTRA_VIDEO_ID = "com.nick1234design.onntube4k.VIDEO_ID"
        const val EXTRA_MAX_HEIGHT = "com.nick1234design.onntube4k.MAX_HEIGHT"
    }
}
