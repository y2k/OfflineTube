package y2k.youtubedownloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.FromDataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import y2k.youtubedownloader.youtube.HttpRequest
import y2k.youtubedownloader.youtube.IncompleteUrlParse
import y2k.youtubedownloader.youtube.Youtube
import y2k.youtubedownloader.youtube.YoutubeUtils
import java.net.URL

@RunWith(Theories::class)
class YoutubeUtilsTests {

    @Theory
    fun test(@FromDataPoints("urls") url: String, @FromDataPoints("vids") vid: String) {
        assertEquals(vid, YoutubeUtils.extractVideoId(url))
    }

    companion object {

        @JvmField
        @DataPoints("urls")
        val urls = arrayOf(
            "https://www.youtube.com/watch?v=WRiYmXZvk44&t=2s",
            "https://youtu.be/WRiYmXZvk44")
        @JvmField
        @DataPoints("vids")
        val vids = arrayOf(
            "WRiYmXZvk44",
            "WRiYmXZvk44")
    }
}

class `Integration tests` {

    @Test
    fun `extract url from link is correct`() {
        val formats = Youtube
            .createPageRequest("WRiYmXZvk44")
            .downloadHtml()
            .let(Youtube::parse)!!
        assertEquals(11, formats.size)

        val fmt = formats.first { it.itag == "18" }
        println(fmt) // FIXME
        val js = Youtube.tryExtractDownloadUrl(fmt)
            .let { it as IncompleteUrlParse }
            .request
            .downloadHtml()
        assertNotNull(js)

        Youtube.decipher(js, fmt)
        assertEquals("todo", js)
    }
}

private fun HttpRequest.downloadHtml(): String =
    URL(url)
        .openConnection()
        .apply { addRequestProperty("User-Agent", userAgent) }
        .getInputStream()
        .use { it.bufferedReader().readText() }