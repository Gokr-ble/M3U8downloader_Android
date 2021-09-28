package com.blesson.m3u8util

import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SliceDecrypter {
    fun decrypt(part: DiscontinuityPart) {
        val sliceFile = part.getSliceFile()
        val decFile = part.getDecFile()
        val key = part.getKey()
        val IV = part.getIV()

        try {
            // 初始化解密参数
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val keySpec = SecretKeySpec(key, "AES")
            val parameterSpec = IvParameterSpec(IV)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec)

            val fileCount = sliceFile.size
            for (i in 0 until fileCount) {
                val encFile = sliceFile[i]
                val DecFile = decFile[i]    // "decFile" Name shadowed??

                val iStream = FileInputStream(encFile)
                val iChannel = iStream.channel
                val buffer = ByteBuffer.allocate(1024)
                iChannel.read(buffer)
                // 翻转为读模式
                buffer.flip()
                // 解密
                val fileBytes = cipher.doFinal(buffer.array())
                // 清空缓冲区，写入解密数据
                buffer.clear()
                buffer.put(fileBytes)

                val oStream = FileOutputStream(DecFile)
                val oChannel = oStream.channel

                while (buffer.hasRemaining()) {
                    oChannel.write(buffer)
                }

                iChannel.close()
                oChannel.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}