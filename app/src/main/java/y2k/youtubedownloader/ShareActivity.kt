package y2k.youtubedownloader

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.facebook.soloader.SoLoader
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import y2k.litho.elmish.*
import y2k.youtubedownloader.ShareScreen.Model
import y2k.youtubedownloader.ShareScreen.Msg
import y2k.youtubedownloader.ShareScreen.Msg.VideosMsg
import y2k.youtubedownloader.youtube.FmtStreamMap
import y2k.youtubedownloader.Downloader as D

class ShareScreen(private val intent: Intent) : ElmFunctions<Model, Msg> {
    data class Model(val formats: List<FmtStreamMap>)
    sealed class Msg {
        class VideosMsg(val result: Result<List<FmtStreamMap>, Errors>) : Msg()
    }

    override fun init(): Pair<Model, Cmd<Msg>> =
        Model(emptyList()) to Cmd.fromSuspend({ D.getAvailableVideos(intent) }, ::VideosMsg)

    override fun update(model: Model, msg: Msg): Pair<Model, Cmd<Msg>> = when (msg) {
        is VideosMsg -> when (msg.result) {
            is Ok -> model.copy(formats = msg.result.value) to Cmd.none()
            is Error -> model to Cmd.none()
        }
    }

    override fun view(model: Model) =
        viewLoading()

    private fun viewLoading() = column {
        alignItems(YogaAlign.CENTER)
        justifyContent(YogaJustify.CENTER)
        children(
            progressL { layout ->
                colorRes(R.color.colorAccent)
                layout {
                    widthDip(50)
                    heightDip(50)
                    marginDip(YogaEdge.BOTTOM, 20)
                }
            },
            text {
                text("Find youtube URL...")
                textSizeSp(24f)
                textColorRes(R.color.colorAccent)
            })
    }
}

class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        program(ShareScreen(intent))
    }

    class App : Application() {
        override fun onCreate() {
            super.onCreate()
            instance = this
            SoLoader.init(this, false)
        }

        companion object {
            lateinit var instance: Application private set
        }
    }
}