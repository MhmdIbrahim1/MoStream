package com.lagradost.cloudstream3.extractors


import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class EPlayExtractor : ExtractorApi() {
    override var name = "EPlay"
    override var mainUrl = "https://eplayvid.net"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).document
        val trueUrl = response.select("source").attr("src")
        return listOf(
            ExtractorLink(
                this.name,
                this.name,
                trueUrl,
                mainUrl,
                getQualityFromName(""), // this needs to be auto
                false
            )
        )
    }
}
