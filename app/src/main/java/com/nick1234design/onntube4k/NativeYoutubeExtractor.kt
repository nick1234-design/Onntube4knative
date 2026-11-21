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

        // Prefer YouTube's adaptive DASH manifest: it carries separate audio/video tracks,
        // which is why filtering only pre-muxed combined streams caused playback failures.
        val dashUrl = extractor.dashMpdUrl
        if (dashUrl.isNotBlank()) return ResolvedYoutubeStream(dashUrl, maxHeight, "DASH")

        val candidates = extractor.videoStreams.filter { it.isUrl && !it.isVideoOnly() && it.height > 0 }
        val selected = candidates.filter { it.height <= maxHeight }.maxByOrNull { it.height }
            ?: candidates.minByOrNull { it.height }
        if (selected != null) return ResolvedYoutubeStream(selected.content, selected.height, selected.getFormat()?.name ?: "video")
        throw IllegalStateException("YouTube did not provide a DASH manifest or a directly playable combined stream for this video. Try another video; YouTube may restrict this stream.")
    }
}

data class ResolvedYoutubeStream(val url: String, val height: Int, val format: String)