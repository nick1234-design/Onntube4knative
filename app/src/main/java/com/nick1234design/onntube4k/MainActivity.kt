package com.nick1234design.onntube4k

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(16, 17, 20)
    private val panel = Color.rgb(29, 31, 36)
    private val red = Color.rgb(255, 48, 48)
    private lateinit var search: EditText
    private lateinit var results: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        buildHome()
    }

    private fun buildHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(42), dp(28), dp(42), dp(24))
            setBackgroundColor(bg)
            isFocusableInTouchMode = true
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "OnnTube"
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }
        val fourK = TextView(this).apply {
            text = " 4K"
            textSize = 34f
            setTextColor(red)
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(brand)
        header.addView(fourK)
        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(makeButton("SETTINGS") { showSettings() })
        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "YOUR VIDEO SPACE"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(22))
        }
        root.addView(subtitle)

        search = EditText(this).apply {
            hint = "Search videos (YouTube connection coming next)"
            textSize = 18f
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(panel)
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEARCH) {
                    runSearch()
                    true
                } else false
            }
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(58)))
        search.setOnClickListener { search.requestFocus() }

        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, dp(4)) }
        root.addView(results)

        val section = TextView(this).apply {
            text = "WELCOME TO ONNTUBE4K"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(34), 0, dp(12))
        }
        root.addView(section)

        val intro = TextView(this).apply {
            text = "A native Android TV app built for your Onn 4K Plus.\n\nThis first build establishes the native TV interface and video player foundation. YouTube search, stream extraction, and quality controls are being added in the next development steps."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        root.addView(intro)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(26), 0, 0)
        }
        actions.addView(makeButton("OPEN PLAYER TEST") { openPlayer() })
        actions.addView(makeButton("ABOUT") { showAbout() }, LinearLayout.LayoutParams(dp(150), dp(54)).apply { leftMargin = dp(12) })
        root.addView(actions)

        val footer = TextView(this).apply {
            text = "BUILD 0.1.0  •  NATIVE ANDROID  •  NO WEBVIEW"
            textSize = 11f
            setTextColor(Color.GRAY)
            setPadding(0, dp(28), 0, 0)
        }
        root.addView(footer)
        setContentView(root)
        search.requestFocus()
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(panel)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener { action() }
            minHeight = dp(52)
            minWidth = dp(130)
        }
    }

    private fun runSearch() {
        val query = search.text.toString().trim()
        if (query.isEmpty()) return
        results.removeAllViews()
        results.addView(TextView(this).apply { text = "Searching YouTube..."; setTextColor(Color.LTGRAY) })
        Thread {
            try {
                val videos = YoutubeApi.search(query, BuildConfig.YOUTUBE_API_KEY)
                runOnUiThread {
                    results.removeAllViews()
                    if (videos.isEmpty()) results.addView(TextView(this).apply { text = "No videos found."; setTextColor(Color.WHITE) })
                    videos.forEach { video ->
                        val item = Button(this).apply {
                            text = video.title + "\n" + video.channel
                            textSize = 14f; isAllCaps = false; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            setTextColor(Color.WHITE); setBackgroundColor(panel)
                            setOnClickListener { startActivity(Intent(this@MainActivity, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_VIDEO_ID, video.id).putExtra(PlayerActivity.EXTRA_MAX_HEIGHT, 1080)) }
                        }
                        results.addView(item, LinearLayout.LayoutParams(-1, dp(68)).apply { bottomMargin = dp(6) })
                    }
                }
            } catch (error: Exception) {
                runOnUiThread { results.removeAllViews(); results.addView(TextView(this).apply { text = error.message ?: "YouTube search failed."; setTextColor(Color.rgb(255, 120, 120)) }) }
            }
        }.start()
    }

    private fun openPlayer() {
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    private fun showSettings() {
        val input = EditText(this).apply {
            hint = "Paste a direct video stream URL"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Playback test")
            .setMessage("The native player can test a direct media stream URL. YouTube link extraction is not connected yet.")
            .setView(input)
            .setPositiveButton("PLAY") { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    startActivity(Intent(this, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_URL, url))
                } else showMessage("Enter a valid direct media URL.")
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("OnnTube4K")
            .setMessage("Version 0.1.0\nNative Android TV foundation for Onn 4K Plus.\n\nYouTube playback and ad-free stream handling are not implemented in this first build.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMessage(message: String) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
