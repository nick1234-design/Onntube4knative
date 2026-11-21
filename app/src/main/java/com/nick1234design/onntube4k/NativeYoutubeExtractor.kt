package com.nick1234design.onntube4k

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL

object NativeYoutubeExtractor {
    @Volatile private var initialized = false

    @Synchronized
    private fun initialize() {
        if (initialized) return
        NewPipe.init(object : Downloader() {
            override fun execute(request: Request): Response {
                val connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                    requestMethod = request.httpMethod()
                    connectTimeout = 15000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                    request.headers().forEach { (name, values) -> values.forEach { addRequestProperty(name, it) } }
                    request.dataToSend()?.let { data -> doOutput = true; outputStream.use { it.write(data) } }
                }
                val status = connection.responseCode
                val stream = if (status in 200..399) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val headers = connection.headerFields.filterKeys { it != null }.mapKeys { it.key!! }.mapValues { it.value ?: emptyList() }
                val response = Response(status, connection.responseMessage ?: "", headers, body, connection.url.toString())
                connection.disconnect()
                return response
            }
        })
        initialized = true
    }

    fun resolve(videoId: String, maxHeight: Int): ResolvedYoutubeStream {
        initialize()
        val extractor = NewPipe.getService("YouTube").getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
        extractor.fetchPage()

        // NewPipe exposes combined, video-only and audio-only streams separately.
        // Prefer a combined stream when available; otherwise return a matched video/audio pair.
        val combined = extractor.videoStreams
            .filter { it.isUrl && !it.isVideoOnly() && it.height > 0 }
            .filter { it.height <= maxHeight }
            .maxByOrNull { it.height }
            ?: extractor.videoStreams
                .filter { it.isUrl && !it.isVideoOnly() && it.height > 0 }
                .minByOrNull { it.height }

        if (combined != null) {
            return ResolvedYoutubeStream(
                videoUrl = combined.content,
                audioUrl = null,
                height = combined.height,
                format = combined.getFormat()?.name ?: "progressive"
            )
        }

        val videoCandidates = extractor.videoOnlyStreams
            .filter { it.isUrl && it.height > 0 }
        val selectedVideo = videoCandidates.filter { it.height <= maxHeight }.maxByOrNull { it.height }
            ?: videoCandidates.minByOrNull { it.height }
        val selectedAudio = extractor.audioStreams.firstOrNull { it.isUrl }

        if (selectedVideo != null && selectedAudio != null) {
            return ResolvedYoutubeStream(
                videoUrl = selectedVideo.content,
                audioUrl = selectedAudio.content,
                height = selectedVideo.height,
                format = "separate audio/video"
            )
        }

        val dashUrl = extractor.dashMpdUrl
        if (dashUrl.isNotBlank()) {
            return ResolvedYoutubeStream(dashUrl, null, maxHeight, "DASH")
        }

        throw IllegalStateException(
            "YouTube extraction returned no usable streams. Combined: ${extractor.videoStreams.count { it.isUrl }}, " +
                "video-only: ${extractor.videoOnlyStreams.count { it.isUrl }}, audio-only: ${extractor.audioStreams.count { it.isUrl }}. " +
                "YouTube may be restricting this video or the extractor may need an update."
        )
    }
}

data class ResolvedYoutubeStream(
    val videoUrl: String,
    val audioUrl: String?,
    val height: Int,
    val format: String
)