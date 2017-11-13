package y2k.youtubedownloader.youtube

import org.json.JSONObject
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.squareup.duktape.Duktape

/**
 * Created by y2k on 13/09/2017.
 */
object Youtube {
    private val BASEURL = "http://www.youtube.com/"
    private val JSPLAYER = "ytplayer\\.config\\s*=\\s*([^\\n]+);"
    private val WATCHV = "https://www.youtube.com/watch?v=%s"
    private val USERAGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)"
    private val FUNCCALL = "([$\\w]+)=([$\\w]+)\\(((?:\\w+,?)+)\\)$"
    private val OBJCALL = "([$\\w]+).([$\\w]+)\\(((?:\\w+,?)+)\\)$"
    private val REGEX_PRE =
        arrayOf("*", ".", "?", "+", "$", "^", "[", "]", "(", ")", "{", "}", "|", "\\", "/")

    fun createPageRequest(vid: String) =
        HttpRequest(USERAGENT, String.format(WATCHV, vid))

    fun parse(pageContent: String): List<FmtStreamMap>? {
        val jsPattern = Pattern.compile(JSPLAYER, Pattern.MULTILINE)
        val matcher = jsPattern.matcher(pageContent)
        if (!matcher.find()) return null

        val ytplayerConfig = JSONObject(matcher.group(1))
        val args = ytplayerConfig.getJSONObject("args")

        var html5playerJS = ytplayerConfig.getJSONObject("assets").getString("js")
        if (html5playerJS.startsWith("//")) {
            html5playerJS = "http:" + html5playerJS
        } else if (html5playerJS.startsWith("/")) {
            html5playerJS = BASEURL + html5playerJS
        }

        return listOf("url_encoded_fmt_stream_map", "adaptive_fmts")
            .map { args.getString(it) }
            .flatMap { parseFmt(it, html5playerJS, args) }
    }

    private fun parseFmt(text: String, html5playerJS: String?, args: JSONObject): List<FmtStreamMap> =
        text.split(",")
            .dropLastWhile { it.isEmpty() }
            .map { fmt ->
                val parseFmtStreamMap = YoutubeUtils.parseFmtStreamMap(Scanner(fmt), "utf-8")
                parseFmtStreamMap.html5playerJS = html5playerJS
                parseFmtStreamMap.videoid = args.optString("video_id")
                parseFmtStreamMap.title = args.optString("title")
                parseFmtStreamMap
            }
            .filter { it.resolution != null }

    fun tryExtractDownloadUrl(fmtStreamMap: FmtStreamMap): UrlParse = when {
        !fmtStreamMap.sig.isNullOrEmpty() -> {
            val sig = fmtStreamMap.sig
            CompleteUrlParse(String.format("%s&signature=%s", fmtStreamMap.url, sig))
        }
        else -> IncompleteUrlParse(HttpRequest(USERAGENT, fmtStreamMap.html5playerJS!!))
    }

    fun decipher(rawJsContent: String, fmtStreamMap: FmtStreamMap): String? {
        val jsContent = rawJsContent.replace("\n", "")
        var f1 = YoutubeUtils.getRegexString(jsContent, "\\w+\\.sig\\|\\|([\$a-zA-Z]+)\\([\$a-zA-Z]+\\ .[\$a-zA-Z]+\\)")
        if (f1.isNullOrEmpty()) {
            f1 = YoutubeUtils.getRegexString(jsContent,
                "\\w+\\.sig.*?\\?.*&&\\w+\\.set\\(\\\"signature\\\",([\$a-zA-Z]+)\\([\$a-zA-Z]+\\" + ".[\$a-zA-Z]+\\)\\)")
        }
        var finalF1 = f1

        for (aREGEX_PRE in REGEX_PRE) {
            if (f1!!.contains(aREGEX_PRE)) {
                finalF1 = "\\" + f1
                break
            }
        }
        var f1def = YoutubeUtils.getRegexString(jsContent, String.format(
            "((function\\s+%s|[{;,]%s\\s*=\\s*function|var\\s+%s\\s*=\\s*function\\s*)\\([^)]*\\)" + "\\s*\\{[^\\{]+\\})",
            finalF1, finalF1, finalF1))

        if (f1def!!.startsWith(",")) {
            f1def = f1def.replaceFirst(",".toRegex(), "")
        }

        val functionSb = StringBuilder()
        trJs(f1def, jsContent, functionSb)

        if (functionSb.isNotEmpty()) {
            val jsStr = functionSb.toString() + "\n" + String.format("%s('%s')", f1, fmtStreamMap.s)
            Duktape.create()
                .use { duktape ->
                    val sig = duktape.evaluate(jsStr)
                    return String.format("%s&signature=%s", fmtStreamMap.url, sig)
                }
        }
        return null
    }

    private fun trJs(jsfunction: String, jsContent: String, functionSb: StringBuilder) {
        var jsfunction = jsfunction
        val split = jsfunction.split(";".toRegex()).dropLastWhile { it.isEmpty() }
        val funcPattern = Pattern.compile(FUNCCALL)
        val objPattern = Pattern.compile(OBJCALL)
        var matcher: Matcher?
        for (code in split) {
            var innerFuncCall: String? = null
            matcher = objPattern.matcher(code)
            if (matcher!!.matches()) {
                val strObj: String = matcher.group(1)
                val strFuncName: String = matcher.group(2)
                if (!strObj.isEmpty()) {
                    jsfunction = jsfunction.replace(strObj + ".", "")
                }
                val objFunction = "($strFuncName\\s*:\\s*function\\(.*?\\)\\{[^\\{]+\\})"
                var f1def = YoutubeUtils.getRegexString(jsContent, objFunction)

                if (!f1def.isNullOrEmpty()) {
                    var objFuncMain = "function "
                    f1def = f1def!!.replace(":function", "")
                    f1def = f1def.replace("}}", "}")
                    objFuncMain += f1def
                    functionSb.append(objFuncMain)
                    functionSb.append("\n")
                }
            }

            matcher = funcPattern.matcher(code)
            if (matcher!!.matches()) {
                var strFunName: String
                val strArgs: String = matcher.group(3)
                strFunName = matcher.group(2)
                if (!strFunName.isEmpty()) {
                    strFunName = Pattern.quote(strFunName)
                }
                if (!strArgs.isEmpty()) {
                    val args = strArgs.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    if (args.size == 1) {
                        innerFuncCall = String.format("(function %s\\(\\w+\\)\\{[^\\{]+\\})", strFunName)
                    } else {
                        innerFuncCall = String.format("(function %s\\(", strFunName)
                        for (i in 0 until args.size - 1) {
                            innerFuncCall += "\\w+,"
                        }
                        innerFuncCall += "\\w+\\)\\{[^\\{]+\\})"
                    }
                }
                if (!innerFuncCall.isNullOrEmpty()) {
                    val f1def = YoutubeUtils.getRegexString(jsContent, innerFuncCall!!)
                    functionSb.append(f1def)
                    functionSb.append("\n")
                }
            }

        }
        functionSb.append(jsfunction)
    }
}