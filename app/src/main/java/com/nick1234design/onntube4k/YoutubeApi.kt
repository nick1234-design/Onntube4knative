package com.nick1234design.onntube4k

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class YoutubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String,
    val description: String
)

object YoutubeApi {
    fun search(query: String, apiKey: String, maxResults: Int = 20): List<YoutubeVideo> {
        require(apiKey.isNotBlank()) { "YouTube API key is missing. Add YOUTUBE_API_KEY to the new repository secrets." }
        val uri = Uri.parse("https://www.googleapis.com/youtube/v3/search").buildUpon()
            .appendQueryParameter("part", "snippet")
            .appendQueryParameter("type", "video")
            .appendQueryParameter("maxResults", maxResults.coerceIn(1, 50).toString())
            .appendQueryParameter("q", query)
            .appendQueryParameter("safeSearch", "moderate")
            .appendQueryParameter("key", apiKey)
            .build()
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("YouTube API request failed (HTTP $code)")
                throw IllegalStateException(message)
            }
            val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
            return (0 until items.length()).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                if (id.isBlank()) return@mapNotNull null
                val snippet = item.optJSONObject("snippet") ?: JSONObject()
                val thumbs = snippet.optJSONObject("thumbnails") ?: JSONObject()
                val thumbnail = listOf("high", "medium", "default")
                    .firstNotNullOfOrNull { key -> thumbs.optJSONObject(key)?.optString("url")?.takeIf(String::isNotBlank) }
                    .orEmpty()
                YoutubeVideo(
                    id = id,
                    title = snippet.optString("title", "Untitled video"),
                    channel = snippet.optString("channelTitle", ""),
                    thumbnail = thumbnail,
                    description = snippet.optString("description", "")
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
