package y2k.youtubedownloader

import android.app.DownloadManager
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import y2k.youtubedownloader.youtube.HttpRequest
import java.net.URL
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

object Http {

    suspend fun downloadHtml(request: HttpRequest): Result<String, Errors> {
        return suspendCoroutine { continuation ->
            object : AsyncTask<Unit, Unit, String?>() {
                override fun doInBackground(vararg p0: Unit?): String? {
                    return try {
                        URL(request.url)
                            .openConnection()
                            .apply { addRequestProperty("User-Agent", request.userAgent) }
                            .getInputStream()
                            .use { it.bufferedReader().readText() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                override fun onPostExecute(result: String?) =
                    continuation.resume(result.toResult(CommonError))
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }
}

fun launch(action: suspend () -> Unit) {
    action.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resume(value: Unit) = Unit
        override fun resumeWithException(exception: Throwable) = throw exception
    })
}

fun Context.enqueueDownload(request: DownloadManager.Request): Long =
    getSystemService(Context.DOWNLOAD_SERVICE)
        .let { it as DownloadManager }
        .enqueue(request)

sealed class Result<out T, out E>
class Ok<out T>(val value: T) : Result<T, Nothing>()
class Error<out E>(val error: E) : Result<Nothing, E>()

fun <T, E> T?.toResult(defError: E): Result<T, E> =
    this?.let(::Ok) ?: Error(defError)

inline fun <T, E, R> Result<T, E>.map(f: (T) -> R): Result<R, E> =
    when (this) {
        is Ok -> Ok(f(value))
        is Error -> this
    }

inline fun <T, E, R> Result<T, E>.mapOption(f: (T) -> R?, defError: E): Result<R, E> =
    when (this) {
        is Ok -> f(value).toResult(defError)
        is Error -> this
    }

inline fun <T, E, R> Result<T, E>.bind(f: (T) -> Result<R, E>): Result<R, E> =
    when (this) {
        is Ok -> f(value)
        is Error -> this
    }

sealed class Errors
object CommonError : Errors()
object CantFindLink : Errors()

inline fun log(msgFactory: () -> String) {
    if (BuildConfig.DEBUG)
        Log.i("YoutubeDownloader", msgFactory())
}