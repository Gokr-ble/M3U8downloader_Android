package com.blesson.m3u8util

import android.content.ContentValues
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.blesson.m3u8util.model.DiscontinuityPart
import com.blesson.m3u8util.utils.*
import java.io.*
import java.nio.ByteBuffer
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
//        val files = root.listFiles()
        root.deleteRecursively()
//        if (files != null) {
//            for (f in files) {
//                if (f.isDirectory) {
//                    deleteAllFiles(f)
//                    f.delete()
//                } else {
//                    if (f.exists()) {
//                        deleteAllFiles(f)
//                        f.delete()
//                    }
//                }
//            }
//        }
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
                // ????????????
                createSavePath(parts)
                scheduleTask(parts)
            }
            0 -> {
                parts = parser.parseParts(content)
                // ????????????
                createSavePath(parts)
                scheduleTask(parts)
            }
        }
    }

    private fun scheduleTask(parts: ArrayList<DiscontinuityPart>) {
        val downloader = Downloader()
        var totalSlice = 0      // ?????????????????????
        for (part in parts) {
            totalSlice += part.getSliceUrl().size
        }
        // ????????????????????????
        var finishedSlice = 0
        val mHandler = Handler(Looper.getMainLooper()) {
            when (it.what) {
                1 -> {
                    // ??????????????????
                    val name = it.data.getString("fileName")
                    val content = it.data.getByteArray("fileContent")
                    // ???content????????????
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
                    Toast.makeText(ContextUtil.context, "????????????", Toast.LENGTH_SHORT).show()
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

        // ?????????????????? & ?????? & ??????
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
                // ???????????????????????????????????????
                var message = Message().apply {
                    what = 1
                    arg1 = finishedSlice
                    arg2 = totalSlice
                }
                MainHandler.sendMessage(message)
                // ???????????????
                message = Message().apply {
                    what = 2
                    obj = "?????????"
                }
                MainHandler.sendMessage(message)
                // ?????????????????????
                for (part in parts) {
                    if (part.getMethod().contains("AES")) {
                        SliceDecrypter().decrypt(part)
                        SliceMerger().mergeSlice(part, true)
                    } else {
                        SliceMerger().mergeSlice(part, false)
                    }
                }
                SliceMerger().mergePart(parts)
                // ??????????????????
                if (deleteOnFinish) {
                    deleteSlices(parts)
                }
                Log.i("Scheduler", "??????????????????")
                // ????????????????????????
                if (downloadToOuter) {
                    writeToSharedArea(parts)
                }

                if (!downloadToInner) {
                    deleteInnerVideo(parts)
                }

                message = Message().apply {
                    what = 2
                    obj = "????????????"
                }
                MainHandler.sendMessage(message)

                message = Message().apply {
                    what = 3
                    obj = "??????????????????: " + parts[0].getFolderName()
                }
                MainHandler.sendMessage(message)
            } else {
                threadPool.shutdownNow()
                val fileRoot = File(ContextUtil.context.filesDir.path + File.separator + parts[0].getFolderName())
                deleteAllFiles(fileRoot)

                val message = Message().apply {
                    what = 4
                    obj = "????????????"
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
                    "???????????????Download/m3u8downloader/" + folderName + "/" + file.name
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
                Log.i("Scheduler/createSavePath", "??????????????????: $savePath")
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