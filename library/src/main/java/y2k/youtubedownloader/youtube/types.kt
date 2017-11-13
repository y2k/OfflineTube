package y2k.youtubedownloader.youtube

class HttpRequest(val userAgent: String, val url: String)

sealed class UrlParse
class CompleteUrlParse(val downloadUrl: String) : UrlParse()
class IncompleteUrlParse(val request: HttpRequest) : UrlParse()

enum class ResolutionNote {
    HD, MHD, LHD, XLHD
}

data class Resolution(
    val id: String,
    val resolution: String,
    val format: String,
    val type: String,
    val notes: ResolutionNote)

data class FmtStreamMap(
    var fallbackHost: String? = null,
    var s: String? = null,
    var itag: String? = null,
    var type: String? = null,
    var quality: String? = null,
    var url: String? = null,
    var sig: String? = null,
    var title: String? = null,
    var extension: String? = null,
    var resolution: Resolution? = null,
    var html5playerJS: String? = null,
    var videoid: CharSequence? = null) {

    val streamString: String?
        get() = if (resolution != null) {
            String.format("%s (%s)", extension, resolution!!.resolution)
        } else {
            null
        }
}