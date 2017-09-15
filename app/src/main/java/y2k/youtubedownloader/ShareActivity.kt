package y2k.youtubedownloader

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)

        launch {
            Downloader.startDownload(this, intent)
            finish()
        }
    }
}