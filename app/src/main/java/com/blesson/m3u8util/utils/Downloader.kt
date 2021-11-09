package com.blesson.m3u8util.utils

import android.os.Bundle
import android.os.Handler
import android.os.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.Exception
import java.util.concurrent.TimeUnit

class Downloader {
    private val TIMEOUT: Long = 3000;
    private val UA = "User-Agent,Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.93 Mobile Safari/537.36"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .build()

    fun downloadSync(url: String): Response? {
        val request = Request.Builder().url(url)
            .addHeader("User-Agent", UA)
            .build()
        var response: Response? = null
        var retryCount = 0
        while (retryCount <= 3) {
            try {
                response = client.newCall(request).execute()
                break
            } catch (e: Exception) {
                e.printStackTrace()
                retryCount++
            }
        }
        return response
    }

    fun downloadThreadSync(url: String, mHandler: Handler): Thread {
        return Thread {
            var retryCount = 0
            while (retryCount <= 5) {
                try {
                    val request = Request.Builder().url(url)
                        .addHeader("User-Agent", UA)
                        .build()
                    val response = client.newCall(request).execute()

                    val bundle = Bundle().apply {
                        putString("fileName", url.substring(url.lastIndexOf("/") + 1))
                        putByteArray("fileContent", response.body?.bytes())
                    }

                    val message = Message()
                    message.what = 1
                    message.data = bundle

                    mHandler.sendMessage(message)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    retryCount++
                }
            }
            if (retryCount > 5) {
                val dFailMessage = Message()
                dFailMessage.what = 2
                mHandler.sendMessage(dFailMessage)
            }
        }
    }
}