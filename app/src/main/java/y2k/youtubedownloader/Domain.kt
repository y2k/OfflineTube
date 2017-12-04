package y2k.youtubedownloader

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import y2k.youtubedownloader.ShareActivity.App
import y2k.youtubedownloader.youtube.*

/**
 * Created by y2k on 14/09/2017.
 **/

class DownloadTask(val url: String, val title: String?)

object Domain {

    fun createYoutubePageRequest(intent: Intent): HttpRequest? =
        intent.getStringExtra(Intent.EXTRA_TEXT)
            .mapNull(YoutubeUtils::extractVideoId)
            .mapNull(Youtube::createPageRequest)

    fun findVideoInHtml(html: String): FmtStreamMap? =
        html.let(Youtube::parse)
            .mapNull(::findBestVideoFormat)

    private fun findBestVideoFormat(formats: List<FmtStreamMap>): FmtStreamMap? =
        formats.firstOrNull { it.type?.startsWith("video/mp4") ?: false && it.quality == "medium" }

    fun makeDownloadRequest(task: DownloadTask): DownloadManager.Request =
        task.url
            .let(Uri::parse)
            .let(DownloadManager::Request)
            .setAllowedOverRoaming(false)
            .setAllowedOverMetered(false)
            .setMimeType("video/mp4")
            .setTitle(task.title)
}

object Downloader {

    suspend fun getAvailableVideos(intent: Intent): List<FmtStreamMap> =
        Domain.createYoutubePageRequest(intent)!!
            .let { Http.downloadHtml(it) }
            .let(Youtube::parse)!!

    @Suppress("unused")
    suspend fun startDownload(intent: Intent): Long =
        startDownload(App.instance, intent)

    private suspend fun startDownload(context: Context, intent: Intent): Long =
        Domain.createYoutubePageRequest(intent)!!
            .let { Http.downloadHtml(it) }
            .let(Domain::findVideoInHtml)!!
            .let { Downloader.tryExtractDownloadUrl(it) }
            .let(Domain::makeDownloadRequest)
            .let(context::enqueueDownload)

    private suspend fun tryExtractDownloadUrl(fmt: FmtStreamMap): DownloadTask =
        Youtube.tryExtractDownloadUrl(fmt)
            .let { specifyDownloadUrl(it, fmt) }
            .let { DownloadTask(it, fmt.title) }

    private suspend fun specifyDownloadUrl(parse: UrlParse, fmt: FmtStreamMap): String =
        when (parse) {
            is CompleteUrlParse ->
                parse.downloadUrl
            is IncompleteUrlParse ->
                parse.request
                    .let { Http.downloadHtml(it) }
                    .let { Youtube.decipher(it, fmt) }!!
        }
}