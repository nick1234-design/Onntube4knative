package com.nick1234design.onntube4k

import android.net.Uri
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class SponsorSegment(val startSeconds: Double, val endSeconds: Double, val category: String)

object SponsorBlockClient {
    private val categories = """["sponsor","selfpromo","interaction","intro","outro","preview","music_offtopic"]"""

    fun getSegments(videoId: String): List<SponsorSegment> {
        val uri = Uri.parse("https://sponsor.ajay.app/api/skipSegments").buildUpon()
            .appendQueryParameter("videoID", videoId)
            .appendQueryParameter("categories", categories)
            .build()
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 10000
            requestMethod = "GET"
        }
        try {
            if (connection.responseCode == 404) return emptyList()
            if (connection.responseCode !in 200..299) return emptyList()
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val segment = item.optJSONArray("segment") ?: return@mapNotNull null
                if (segment.length() < 2) return@mapNotNull null
                SponsorSegment(segment.optDouble(0), segment.optDouble(1), item.optString("category"))
            }.sortedBy { it.startSeconds }
        } finally {
            connection.disconnect()
        }
    }
}
