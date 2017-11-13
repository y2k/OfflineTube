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

    fun createYoutubePageRequest(intent: Intent): Result<HttpRequest, Errors> =
        intent
            .getStringExtra(Intent.EXTRA_TEXT)
            .toResult(CommonError)
            .mapOption(YoutubeUtils::extractVideoId, CantFindLink)
            .map(Youtube::createPageRequest)

    fun findVideoInHtml(html: String): Result<FmtStreamMap, Errors> =
        html.let(Youtube::parse)
            .toResult(CommonError)
            .bind(this::findBestVideoFormat)

    private fun findBestVideoFormat(formats: List<FmtStreamMap>): Result<FmtStreamMap, Errors> =
        formats
            .firstOrNull { it.type?.startsWith("video/mp4") ?: false && it.quality == "medium" }
            .toResult(CantFindLink)

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

    suspend fun getAvailableVideos(intent: Intent): Result<List<FmtStreamMap>, Errors> =
        TODO()

    suspend fun startDownload(intent: Intent): Result<Long, Errors> =
        startDownload(App.instance, intent)

    suspend fun startDownload(context: Context, intent: Intent): Result<Long, Errors> =
        Domain.createYoutubePageRequest(intent)
            .bind { Http.downloadHtml(it) }
            .bind(Domain::findVideoInHtml)
            .bind { Downloader.tryExtractDownloadUrl(it) }
            .map(Domain::makeDownloadRequest)
            .map(context::enqueueDownload)

    private suspend fun tryExtractDownloadUrl(fmt: FmtStreamMap): Result<DownloadTask, Errors> =
        Youtube.tryExtractDownloadUrl(fmt)
            .let { specifyDownloadUrl(it, fmt) }
            .map { DownloadTask(it, fmt.title) }

    private suspend fun specifyDownloadUrl(parse: UrlParse, fmt: FmtStreamMap): Result<String, Errors> =
        when (parse) {
            is CompleteUrlParse -> Ok(parse.downloadUrl)
            is IncompleteUrlParse -> {
                parse.request
                    .toResult(CommonError)
                    .bind { Http.downloadHtml(it) }
                    .mapOption({ Youtube.decipher(it, fmt) }, CommonError)
            }
        }
}