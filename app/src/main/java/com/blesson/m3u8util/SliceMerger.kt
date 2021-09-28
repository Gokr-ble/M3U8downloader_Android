package com.blesson.m3u8util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SliceMerger {
    fun mergeSlice(part: DiscontinuityPart, isEncrypted: Boolean) {
        val destFile = if (isEncrypted) {
            part.getDecFile()
        } else {
            part.getSliceFile()
        }

        val savePath = part.getSavePath()
        val partIndex = part.getPartIndex()
        val name = "part$partIndex.mp4"

        try {
            val oStream = FileOutputStream("$savePath/$name")
            val buffer = ByteArray(4096)
            for (file in destFile) {
                val iStream = FileInputStream(file)
                while (iStream.read(buffer) != -1) {
                    oStream.write(buffer)
                }
                iStream.close()
            }
            oStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun mergePart(parts: ArrayList<DiscontinuityPart>) {
        val newFileName = parts[0].getNewFileName()
        val savePath = parts[0].getSavePath()

        val partCount = parts.size
        try {
            val oStream = FileOutputStream("$savePath/$newFileName.mp4")
            val buffer = ByteArray(4096)
            for (i in 0 until partCount) {
                val iStream = FileInputStream("$savePath/part$i.mp4")
                while(iStream.read(buffer) != -1) {
                    oStream.write(buffer)
                }
                iStream.close()
            }
            oStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}