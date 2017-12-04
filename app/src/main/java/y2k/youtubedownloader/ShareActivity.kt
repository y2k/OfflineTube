package y2k.youtubedownloader

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.facebook.litho.ComponentLayout.ContainerBuilder
import com.facebook.soloader.SoLoader
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import kotlinx.types.Result
import kotlinx.types.Result.Error
import kotlinx.types.Result.Ok
import y2k.litho.elmish.experimental.*
import y2k.litho.elmish.experimental.Views.column
import y2k.youtubedownloader.ShareScreen.Model
import y2k.youtubedownloader.ShareScreen.Msg
import y2k.youtubedownloader.ShareScreen.Msg.VideosMsg
import y2k.youtubedownloader.youtube.FmtStreamMap
import y2k.youtubedownloader.Downloader as D

class ShareScreen(private val intent: Intent) : ElmFunctions<Model, Msg> {

    data class Model(val formats: List<FmtStreamMap> = emptyList())
    sealed class Msg {
        class VideosMsg(val result: Result<List<FmtStreamMap>, Exception>) : Msg()
    }

    override fun init(): Pair<Model, Cmd<Msg>> =
        Model() to Cmd.fromContext({ D.getAvailableVideos(intent) }, ::VideosMsg)

    override fun update(model: Model, msg: Msg): Pair<Model, Cmd<Msg>> = when (msg) {
        is VideosMsg -> {
            when (msg.result) {
                is Ok -> model.copy(formats = msg.result.value) to Cmd.none()
                is Error -> Log.log(msg.result.error, model) to Cmd.none()
            }
        }
    }

    override fun ContainerBuilder.view(model: Model) =
        if (model.formats.isEmpty())
            viewLoading()
        else viewFormats(model)

    private fun ContainerBuilder.viewFormats(model: Model) =
        column {
            model.formats
                .map(::viewItem)
                .forEach(::layoutChild)
        }

    private fun viewItem(item: FmtStreamMap) =
        column {
            text {
                text(item.quality)
            }
        }

    private fun ContainerBuilder.viewLoading() =
        column {
            alignItems(YogaAlign.CENTER)
            justifyContent(YogaJustify.CENTER)

            progress {
                colorRes(R.color.colorAccent)
                widthDip(50f)
                heightDip(50f)
                marginDip(YogaEdge.BOTTOM, 20f)
            }

            text {
                textSizeSp(24f)
                textColorRes(R.color.colorAccent)
                text("Find youtube URL...")
            }
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