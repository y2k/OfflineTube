package y2k.youtubedownloader

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.withContext
import y2k.youtubedownloader.youtube.HttpRequest
import java.net.URL

object Http {

    suspend fun downloadHtml(request: HttpRequest): String = withContext(DefaultDispatcher) {
        URL(request.url)
            .openConnection()
            .apply { addRequestProperty("User-Agent", request.userAgent) }
            .getInputStream()
            .use { it.bufferedReader().readText() }
    }
}

fun <T, R> T?.mapNull(f: (T) -> R?): R? =
    if (this == null) null else f(this)

fun Context.enqueueDownload(request: DownloadManager.Request): Long =
    getSystemService(Context.DOWNLOAD_SERVICE)
        .let { it as DownloadManager }
        .enqueue(request)

object Log {

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> log(e: Exception, x: T): T {
        e.printStackTrace()
        return x
    }
}