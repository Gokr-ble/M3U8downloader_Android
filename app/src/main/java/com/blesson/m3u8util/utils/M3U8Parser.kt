package com.blesson.m3u8util.utils

import com.blesson.m3u8util.model.DiscontinuityPart
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class M3U8Parser(url: String) {
    private val baseUrl = url

    fun check(content: String): Int {
        if (!isValidM3U8File(content)) return 1 // 不是合法的m3u8文件
        if (containSecondUrl(content)) return 2 // 包含第二层m3u8链接
        return 0
    }

    private fun isValidM3U8File(content: String): Boolean {
        return content.contains("#EXTM3U")
    }

    private fun containSecondUrl(content: String): Boolean {
        return content.contains(".m3u8")
    }

    fun parseParts(content: String): ArrayList<DiscontinuityPart> {
        val partString = content.split("#EXT-X-DISCONTINUITY")
        // part总数
        val partCount = partString.size
        val parts = ArrayList<DiscontinuityPart>()
        for (i in 0 until partCount) {
            val s = partString[i]
            val part = DiscontinuityPart(baseUrl)
            // 设定savePath, folderName, newFileName
            parseNameAndPath(part)
            // 设定partIndex
            part.setPartIndex(i)
            // 逐行判断
            val lines = s.split("\n")
            for (line in lines) {
                if (line.contains("#EXT-X-KEY")) {
                    // 有加密，设置method, keyUrl, key, IV
                    part.apply {
                        setMethod(parseMethod(line))
                        setKeyUrl(parseKeyUrl(line))
                        setKey(parseKey(parseKeyUrl(line)))
                        setIV(parseIV(line))
                    }
                }
                if (line.contains(".ts")) {
                    part.addSliceUrl(parseSliceUrl(line))
                }
            }
            // 解析sliceFile或decFile
            parseSliceFile(part)
            if (part.getMethod().contains("AES")) {
                parseDecFile(part)
            }
            parts.add(part)
        }
        return parts
    }

    private fun parseMethod(s: String): String {
        //#EXT-X-KEY:METHOD=AES-128,URI="https://ts2.zhiyuanhongda.com/20210305/KbpCFCGq/1000kb/hls/key.key",IV=0x30eb47c22ac4db01433ef459d6d36a43
        return s.split(",")[0]
                .split(":")[1]
                .split("=")[1]
    }

    private fun parseKeyUrl(s: String): String {
        val uri = s.split(",")[1].split("\"")[1]
        return if (uri.contains("http")) uri
        else {
            val split = baseUrl.split("/")
            split[0] + "//" + split[2] + s
        }
    }

    private fun parseKey(keyUrl: String): ByteArray {
        var key = ByteArray(16)
        try {
            key = Downloader().downloadSync(keyUrl)!!.body!!.bytes()
        } catch (e: Exception) {
            // 大概是没获取到key？
            e.printStackTrace()
        }
        return key
    }

    private fun parseIV(s: String): ByteArray {
        if (s.contains("IV=")) {
            val value = s.split(",")[2].split("=")[1]
            val output = ByteArray(16)
            for (i in 2 until 33 step 2) {
                val tmp = value.substring(i, i+2).toInt(16)
                output[i / 2 - 1] = tmp.toByte()
            }
            return output
        } else {
            return ByteArray(16)
        }
    }

    private fun parseSliceUrl(s: String): String {
        if (s.contains("http")) {
            // 以绝对地址形式给出
            return s
        } else if (s.contains("/")) {
            // 以相对根路径地址给出
            val split = baseUrl.split("/")
            return split[0] + "//" + split[2] + s
        } else {
            // 只替换URL最后一节
            return baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1) + s
        }
    }

    private fun parseSliceFile(part: DiscontinuityPart) {
        val sliceUrl = part.getSliceUrl()
        val savePath = part.getSavePath()
        for (slice in sliceUrl) {
            val fileName = slice.substring(slice.lastIndexOf("/") + 1)
            part.addSliceFile(File("$savePath/$fileName"))
        }
    }

    private fun parseDecFile(part: DiscontinuityPart) {
        val sliceUrl = part.getSliceUrl()
        val savePath = part.getSavePath()
        var index = 0
        for (slice in sliceUrl) {
            val fileName = "out$index.ts"
            part.addDecFile(File("$savePath/$fileName"))
        }
    }

    private fun parseNameAndPath(part: DiscontinuityPart) {
        // 确定folderName
        val dateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val folderName = dateTime.format(formatter)
        // 确定newFileName
        val newName = baseUrl.substring(baseUrl.lastIndexOf("/") + 1).split(".")[0]
        // 确定savePath
        val savePath = ContextUtil.context.filesDir.path + "/" + folderName
        part.apply {
            setSavePath(savePath)
            setFolderName(folderName)
            setNewFileName(newName)
        }
    }

    fun getSecondUrl(content: String): String {
        val lines = content.split("\n")
        // 如果存在多种分辨率(码率)，分别解析并保存
        val urls = ArrayList<Pair<Int, String>>()
        for (i in 1 until lines.size step 2) {
            if (lines[i].contains("BANDWIDTH")) {
                val bandwidth = lines[i].split(",")[1].split("=")[1]
                urls.add(Pair(bandwidth.toInt(), lines[i+1]))
            }
        }
        // 选择最高码率下载
        val maxBandwidth = urls.maxByOrNull { it.first }
        val maxUrl = maxBandwidth?.second
        if (maxUrl != null) {
            return if (maxUrl.contains("http")) {
                maxUrl
            } else {
                val split = baseUrl.split("/")
                split[0] + "//" + split[2] + maxUrl
            }
        }

//        for (line in lines) {
//            if (line.contains(".m3u8")) {
//                if (line.contains("http")) {
//                    return line
//                } else {
//                    val split = baseUrl.split("/")
//                    return split[0] + "//" + split[2] + line
//                }
//            }
//        }
        return "获取重定向链接异常"
    }
}