package com.blesson.m3u8util

class M3U8File(url: String) {
    private var baseUrl = url
    private var rootUrl = ""
    private var parts: ArrayList<DiscontinuityPart> = ArrayList()

    init {
        val tmp = baseUrl.split("/")
        setRootUrl(tmp[0] + "//" + tmp[2])
    }

    fun getBaseUrl(): String { return baseUrl }
    fun getRootUrl(): String { return rootUrl }
    fun getParts(): ArrayList<DiscontinuityPart> { return parts }

    fun setBaseUrl(u: String) { baseUrl = u }
    fun setRootUrl(u: String) { rootUrl = u }
    fun addPart(p: DiscontinuityPart) { parts.add(p) }
}