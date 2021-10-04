package com.blesson.m3u8util

import android.content.ContentResolver
import android.content.ContentValues
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Scheduler(url: String, mainHandler: Handler, settings: Bundle) {
    private val baseUrl = url
    private val MainHandler = mainHandler
    private val deleteOnFinish = settings.getBoolean("deleteOnFinish")
    private val downloadToInner = settings.getBoolean("downToInner")
    private val downloadToOuter = settings.getBoolean("downToOuter")
    private val threadNumber = settings.getInt("threadNumber")
    private var cancelDownload = false

    fun start() {
        thread {
            val content = getContent(baseUrl)
            parseContent(content)
        }
    }

    fun stop() {
        cancelDownload = true
    }

    private fun deleteAllFiles(root: File) {
        val files = root.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    deleteAllFiles(f)
                    f.delete()
                } else {
                    if (f.exists()) {
                        deleteAllFiles(f)
                        f.delete()
                    }
                }
            }
        }
    }

    private fun getContent(baseUrl: String): String {
        val downloader = Downloader()
        var content = ""
        try {
            content = downloader.downloadSync(baseUrl)!!.body!!.string()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return content
    }

    private fun parseContent(content: String) {
        val parts: ArrayList<DiscontinuityPart>
        val parser = M3U8Parser(baseUrl)
        when (parser.check(content)) {
            2 -> {
                val secondUrl = parser.getSecondUrl(content)
                val newContent = getContent(secondUrl)
                parts = parser.parseParts(newContent)
                // 创建目录
                createSavePath(parts)
                scheduleTask(parts)
            }
            0 -> {
                parts = parser.parseParts(content)
                // 创建目录
                createSavePath(parts)
                scheduleTask(parts)
            }
        }
    }

    private fun scheduleTask(parts: ArrayList<DiscontinuityPart>) {
        val downloader = Downloader()
        var totalSlice = 0      // 统计分片总计数
        for (part in parts) {
            totalSlice += part.getSliceUrl().size
        }
        // 已完成的分片个数
        var finishedSlice = 0
        val mHandler = Handler(Looper.getMainLooper()) {
            when (it.what) {
                1 -> {
                    // 分片下载成功
                    val name = it.data.getString("fileName")
                    val content = it.data.getByteArray("fileContent")
                    // 将content写入文件
                    val savePath = parts[0].getSavePath()
                    try {
                        val channel = FileOutputStream("$savePath/$name").channel
                        channel.apply {
                            write(ByteBuffer.wrap(content))
                            force(true)
                            close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    finishedSlice++
                }
                2 -> {
                    stop()
                    Toast.makeText(ContextUtil.context, "下载失败", Toast.LENGTH_SHORT).show()
                }
            }
            false
        }

        val threadPool = Executors.newFixedThreadPool(threadNumber)
        for (part in parts) {
            val sliceUrl = part.getSliceUrl()
            for (url in sliceUrl) {
                threadPool.execute(downloader.downloadThreadSync(url, mHandler))
            }
        }
        threadPool.shutdown()

        // 开启监视线程 & 解密 & 合并
        thread {
            while (finishedSlice != totalSlice && !cancelDownload) {
                val message = Message().apply {
                    what = 1
                    arg1 = finishedSlice
                    arg2 = totalSlice
                }
                MainHandler.sendMessage(message)
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            if (!cancelDownload) {
                // 下载正常结束，更新最终数据
                var message = Message().apply {
                    what = 1
                    arg1 = finishedSlice
                    arg2 = totalSlice
                }
                MainHandler.sendMessage(message)
                // 解密，合并
                message = Message().apply {
                    what = 2
                    obj = "解密中"
                }
                MainHandler.sendMessage(message)
                // 合并分片、分段
                for (part in parts) {
                    if (part.getMethod().contains("AES")) {
                        SliceDecrypter().decrypt(part)
                        SliceMerger().mergeSlice(part, true)
                    } else {
                        SliceMerger().mergeSlice(part, false)
                    }
                }
                SliceMerger().mergePart(parts)
                // 删除下载片段
                if (deleteOnFinish) {
                    deleteSlices(parts)
                }
                Log.i("Scheduler", "分片清理完成")
                // 写入共享存储区域
                if (downloadToOuter) {
                    writeToSharedArea(parts)
                }

                if (!downloadToInner) {
                    deleteInnerVideo(parts)
                }

                message = Message().apply {
                    what = 2
                    obj = "下载完成"
                }
                MainHandler.sendMessage(message)

                message = Message().apply {
                    what = 3
                    obj = "文件存储位置: " + parts[0].getFolderName()
                }
                MainHandler.sendMessage(message)
            } else {
                threadPool.shutdownNow()
                val root = File(ContextUtil.context.filesDir.path)
                deleteAllFiles(root)

                val message = Message().apply {
                    what = 4
                    obj = "下载取消"
                }
                MainHandler.sendMessage(message)
            }
        }
    }

    private fun writeToSharedArea(parts: ArrayList<DiscontinuityPart>) {
        val savePath = parts[0].getSavePath()
        val folderName = parts[0].getFolderName()
        val newFileName = parts[0].getNewFileName()
        val file = File("$savePath/$newFileName.mp4")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = ContextUtil.context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, "$newFileName.mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Download/m3u8downloader/$folderName")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val contentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            var iStream: InputStream? = null
            var oStream: OutputStream? = null
            try {
                iStream = FileInputStream(file)
                if (contentUri != null) {
                    oStream = resolver.openOutputStream(contentUri)
                }
                if (oStream != null) {
                    val buffer = ByteArray(1024)
                    while (iStream.read(buffer) != -1) {
                        oStream.write(buffer)
                    }
                }
                Log.i(
                    "saveToSharedPath",
                    "文件位置：Download/m3u8downloader/" + folderName + "/" + file.name
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    iStream?.close()
                    oStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun createSavePath(parts: ArrayList<DiscontinuityPart>) {
        val savePath = parts[0].getSavePath()
        val file = File(savePath)
        if (!file.exists()) {
            if (file.mkdir()) {
                Log.i("Scheduler/createSavePath", "目录创建成功: $savePath")
            }
        }
    }

    private fun deleteSlices(parts: ArrayList<DiscontinuityPart>) {
        val savePath = parts[0].getSavePath()
        val file = File(savePath)
        val remains = file.listFiles()
        if (remains != null) {
            for (f in remains) {
                if (f.name.contains(".ts") || f.name.contains("part")) {
                    f.delete()
                }
            }
        }
    }

    private fun deleteInnerVideo(parts: ArrayList<DiscontinuityPart>) {
        val savePath = parts[0].getSavePath()
        val file = File(savePath)
        val remains = file.listFiles()
        if (remains != null) {
            for (f in remains) {
                if (f.name.contains(".mp4")) {
                    f.delete()
                }
            }
        }
        file.delete()
    }
}