package com.lagradost.cloudstream3.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

// Code found in https://github.com/KillerDogeEmpire/vidplay-keys
// special credits to @KillerDogeEmpire for providing key

class AnyVidplay(hostUrl: String) : Vidplay() {
    override val mainUrl = hostUrl
}

class MyCloud : Vidplay() {
    override val name = "MyCloud"
    override val mainUrl = "https://mcloud.bz"
}

class MegaF : Vidplay() {
    override val name = "MegaF"
    override val mainUrl = "https://megaf.cc"
}

class VidplayOnline : Vidplay() {
    override val mainUrl = "https://vidplay.online"
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
open class Vidplay : ExtractorApi() {
    override val name = "Vidplay"
    override val mainUrl = "https://vidplay.site"
    override val requiresReferer = true

    companion object {
        private val keySource = "https://rowdy-avocado.github.io/multi-keys/"

        private var keys: List<String>? = null

        private suspend fun getKeys(): List<String> {
            return keys
                ?: run {
                    val res =
                        app.get(keySource).parsedSafe<KeysData>()
                            ?: throw ErrorLoadingException("Unable to get keys")
                    keys = res.keys
                    res.keys
                }
        }

        private data class KeysData(@JsonProperty("vidplay") val keys: List<String>)
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val myKeys = getKeys()
        val domain = url.substringBefore("/e/")
        val id = url.substringBefore("?").substringAfterLast("/")
        val encodedId = encode(id, myKeys.get(0))
        val t = url.substringAfter("t=").substringBefore("&")
        val h = encode(id, myKeys.get(1))
        val mediaUrl = "$domain/mediainfo/$encodedId?t=$t&h=$h"
        val encodedRes =
            app.get("$mediaUrl").parsedSafe<Response>()?.result
                ?: throw Exception("Unable to fetch link")
        val decodedRes = decode(encodedRes, myKeys.get(2))
        val res = tryParseJson<Result>(decodedRes)
        res?.sources?.map {
            M3u8Helper.generateM3u8(this.name, it.file ?: return@map, "$mainUrl/").forEach(callback)
        }

        res?.tracks?.filter { it.kind == "captions" }?.map {
            subtitleCallback.invoke(SubtitleFile(it.label ?: return@map, it.file ?: return@map))
        }
    }

    private fun encode(input: String, key: String): String {
        val rc4Key = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.ENCRYPT_MODE, rc4Key)
        val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        return Base64.UrlSafe.encode(encryptedBytes)
    }

    fun decode(input: String, key: String): String {
        val decodedBytes = Base64.UrlSafe.decode(input)
        val rc4Key = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        val decodedString = String(decryptedBytes, Charsets.UTF_8)
        return URLDecoder.decode(decodedString, "UTF-8")
    }

    data class Tracks(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind") val kind: String? = null,
    )

    data class Sources(
        @JsonProperty("file") val file: String? = null,
    )

    data class Result(
        @JsonProperty("sources") val sources: ArrayList<Sources>? = arrayListOf(),
        @JsonProperty("tracks") val tracks: ArrayList<Tracks>? = arrayListOf(),
    )

    data class Response(@JsonProperty("result") val result: String? = null)
}