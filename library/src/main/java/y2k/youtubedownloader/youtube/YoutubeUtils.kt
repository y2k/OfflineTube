package y2k.youtubedownloader.youtube

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

object YoutubeUtils {
    private val PARAMETER_SEPARATOR = "&"
    private val NAME_VALUE_SEPARATOR = "="
    private val playResolutions = ArrayList<Resolution>()

    init {
        playResolutions.add(Resolution("17", "176x144", "3gp", "normal", ResolutionNote.LHD))
        playResolutions.add(Resolution("36", "320x240", "3gp", "normal", ResolutionNote.LHD))
        playResolutions.add(Resolution("18", "640x360", "mp4", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("242", "360x240", "webm", "normal", ResolutionNote.LHD))
        playResolutions.add(Resolution("242", "360x240", "webm", "normal", ResolutionNote.LHD))
        playResolutions.add(Resolution("243", "480x360", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("243", "480x360", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("43", "640x360", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("244", "640x480", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("245", "640x480", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("167", "640x480", "webm", "video", ResolutionNote.MHD))
        playResolutions.add(Resolution("246", "640x480", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("247", "720x480", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("44", "854x480", "webm", "normal", ResolutionNote.MHD))
        playResolutions.add(Resolution("168", "854x480", "webm", "video", ResolutionNote.MHD))
    }

    private val Resolutions = HashMap<String, Resolution>()

    init {
        Resolutions.put("5", Resolution("5", "400x240", "flv", "normal", ResolutionNote.LHD))//
        Resolutions.put("6", Resolution("6", "450x270", "flv", "normal", ResolutionNote.MHD))
        Resolutions.put("17", Resolution("17", "176x144", "3gp", "normal", ResolutionNote.LHD))//
        Resolutions.put("18", Resolution("18", "640x360", "mp4", "normal", ResolutionNote.MHD))
        Resolutions.put("22", Resolution("22", "1280x720", "mp4", "normal", ResolutionNote.HD))
        Resolutions.put("34", Resolution("34", "640x360", "flv", "normal", ResolutionNote.MHD))
        Resolutions.put("35", Resolution("35", "854x480", "flv", "normal", ResolutionNote.MHD))
        Resolutions.put("36", Resolution("36", "320x240", "3gp", "normal", ResolutionNote.LHD))//
        Resolutions.put("37", Resolution("37", "1920x1080", "mp4", "normal", ResolutionNote.XLHD))
        Resolutions.put("38", Resolution("38", "4096x3072", "mp4", "normal", ResolutionNote.XLHD))
        Resolutions.put("43", Resolution("43", "640x360", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("44", Resolution("44", "854x480", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("45", Resolution("45", "1280x720", "webm", "normal", ResolutionNote.HD))
        Resolutions.put("46", Resolution("46", "1920x1080", "webm", "normal", ResolutionNote.XLHD))
        Resolutions.put("167", Resolution("167", "640x480", "webm", "video", ResolutionNote.MHD))
        Resolutions.put("168", Resolution("168", "854x480", "webm", "video", ResolutionNote.MHD))
        Resolutions.put("169", Resolution("169", "1280x720", "webm", "video", ResolutionNote.HD))
        Resolutions.put("170", Resolution("170", "1920x1080", "webm", "video", ResolutionNote.XLHD))
        Resolutions.put("242", Resolution("242", "360x240", "webm", "normal", ResolutionNote.LHD))//
        Resolutions.put("243", Resolution("243", "480x360", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("244", Resolution("244", "640x480", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("245", Resolution("245", "640x480", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("246", Resolution("246", "640x480", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("247", Resolution("247", "720x480", "webm", "normal", ResolutionNote.MHD))

        Resolutions.put("82", Resolution("82", "360p", "mp4", "normal", ResolutionNote.MHD))
        Resolutions.put("83", Resolution("83", "480p", "mp4", "normal", ResolutionNote.MHD))
        Resolutions.put("84", Resolution("84", "720p", "mp4", "normal", ResolutionNote.MHD))
        Resolutions.put("85", Resolution("85", "1080p", "mp4", "normal", ResolutionNote.MHD))
        Resolutions.put("100", Resolution("100", "360p", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("101", Resolution("101", "480p", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("102", Resolution("102", "720p", "webm", "normal", ResolutionNote.MHD))

        Resolutions.put("139", Resolution("139", "Audio only", "m4a", "normal", ResolutionNote.MHD))
        Resolutions.put("140", Resolution("140", "Audio only", "m4a", "normal", ResolutionNote.MHD))
        Resolutions.put("141", Resolution("141", "Audio only", "m4a", "normal", ResolutionNote.MHD))

        Resolutions.put("171", Resolution("313", "Audio only", "webm", "normal", ResolutionNote.MHD))
        Resolutions.put("172", Resolution("313", "Audio only", "webm", "normal", ResolutionNote.MHD))
    }

    fun getRegexString(content: String, pattern: String): String? {
        val p = Pattern.compile(pattern)
        val matcher = p.matcher(content)
        var group: String? = null
        if (matcher.find()) {
            group = matcher.group(1)
        }
        return group
    }

    fun extractVideoId(url: String): String? {
        val p = Pattern.compile("(?:^|[^\\w-]+)([\\w-]{11})(?:[^\\w-]+|$)")
        val matcher = p.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    fun parseFmtStreamMap(scanner: Scanner, encoding: String): FmtStreamMap {
        val streamMap = FmtStreamMap()
        scanner.useDelimiter(PARAMETER_SEPARATOR)
        while (scanner.hasNext()) {
            val nameValue = scanner.next()
                .split(NAME_VALUE_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }
            if (nameValue.isEmpty() || nameValue.size > 2) {
                throw IllegalArgumentException("bad parameter")
            }

            val name = decode(nameValue[0], encoding)
            var value: String? = null
            if (nameValue.size == 2) {
                value = decode(nameValue[1], encoding)
            }

            if ("fallback_host" == name)
                streamMap.fallbackHost = value
            if ("s" == name)
                streamMap.s = value
            if ("itag" == name)
                streamMap.itag = value
            if ("type" == name)
                streamMap.type = value
            if ("quality" == name)
                streamMap.quality = value
            if ("url" == name)
                streamMap.url = value
            if ("sig" == name)
                streamMap.sig = value
            if (!streamMap.itag.isNullOrEmpty())
                streamMap.resolution = Resolutions[streamMap.itag]
            if (streamMap.resolution != null)
                streamMap.extension = streamMap.resolution!!.format

        }
        return streamMap
    }

    private fun decode(content: String, encoding: String?): String {
        try {
            return URLDecoder.decode(content, encoding ?: "ISO-8859-1")
        } catch (problem: UnsupportedEncodingException) {
            throw IllegalArgumentException(problem)
        }
    }
}