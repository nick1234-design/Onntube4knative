package com.nick1234design.onntube4k

import android.graphics.Color
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        root.addView(playerView, FrameLayout.LayoutParams(-1, -1))
        val note = TextView(this).apply {
            text = "OnnTube4K native player • Direct stream test"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(16, 10, 16, 10)
            setBackgroundColor(0x99000000.toInt())
        }
        val noteParams = FrameLayout.LayoutParams(-2, -2).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }
        root.addView(note, noteParams)
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val url = intent.getStringExtra(EXTRA_URL)
        if (!url.isNullOrBlank()) {
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
    }

    override fun onStop() {
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    companion object {
        const val EXTRA_URL = "com.nick1234design.onntube4k.STREAM_URL"
    }
}
