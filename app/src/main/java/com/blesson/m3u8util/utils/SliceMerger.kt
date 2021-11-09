package com.blesson.m3u8util.utils

import com.blesson.m3u8util.model.DiscontinuityPart
import java.io.File

class SliceMerger {
    fun mergeSlice(part: DiscontinuityPart, isEncrypted: Boolean) {
        val destFile = if (isEncrypted) {
            part.getDecFile()
        } else {
            part.getSliceFile()
        }

        val savePath = part.getSavePath()
        val partIndex = part.getPartIndex()
        val name = "part${partIndex}.mp4"

        try {
            val oFile = File("${savePath}/${name}")

            for (file in destFile) {
                val iStream = file.inputStream()
                oFile.appendBytes(iStream.readBytes())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun mergePart(parts: ArrayList<DiscontinuityPart>) {
        val newFileName = parts[0].getNewFileName()
        val savePath = parts[0].getSavePath()

        val partCount = parts.size
        try {
            val oFile = File("${savePath}/${newFileName}.mp4")

            for (i in 0 until partCount) {
                val iFile = File("${savePath}/part${i}.mp4")
                val iStream = iFile.inputStream()
                oFile.writeBytes(iStream.readBytes())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}