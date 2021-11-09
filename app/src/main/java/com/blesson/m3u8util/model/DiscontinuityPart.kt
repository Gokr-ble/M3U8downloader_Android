package com.blesson.m3u8util.model

import java.io.File

class DiscontinuityPart(url: String) {
    private val baseUrl = url
    private var method = "NONE"
    private var keyUrl = ""
    private var key = ByteArray(16)
    private var IV = ByteArray(16)
    private var sliceUrl: ArrayList<String> = ArrayList()
    private var sliceFile: ArrayList<File> = ArrayList()
    private var decFile: ArrayList<File> = ArrayList()
    private lateinit var mergedFile: File
    private lateinit var savePath: String
    private lateinit var folderName: String
    private lateinit var newFileName: String
    private var partIndex = 0

    fun getMethod(): String { return method }
    fun getKeyUrl(): String { return keyUrl }
    fun getKey(): ByteArray { return key }
    fun getIV(): ByteArray { return IV }
    fun getSliceUrl(): ArrayList<String> { return sliceUrl }
    fun getSliceFile(): ArrayList<File> { return sliceFile }
    fun getDecFile(): ArrayList<File> { return decFile }
    fun getMergeFile(): File { return mergedFile }
    fun getSavePath(): String { return savePath }
    fun getFolderName(): String { return folderName }
    fun getNewFileName(): String { return newFileName}
    fun getPartIndex(): Int { return partIndex }

    fun setMethod(m: String) { method = m }
    fun setKeyUrl(u: String) { keyUrl = u }
    fun setKey(k: ByteArray) { System.arraycopy(k, 0, key, 0, 16) }
    fun setIV(iv: ByteArray) { System.arraycopy(iv, 0, IV, 0, 16) }
    fun addSliceUrl(url: String) { sliceUrl.add(url) }
    fun addSliceFile(f: File) { sliceFile.add(f) }
    fun addDecFile(f: File) { decFile.add(f) }
    fun setSavePath(p: String) { savePath = p }
    fun setFolderName(n: String) { folderName = n }
    fun setNewFileName(n: String) { newFileName = n }
    fun setPartIndex(i: Int) { partIndex = i }
}