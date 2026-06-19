package com.musiccontrol.controller.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 网络层 — 向 Windows 服务端发送 HTTP 请求
 */
class MusicApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * 发送控制指令
     * @return Result 包装的响应字符串
     */
    suspend fun sendCommand(host: String, port: Int, command: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "http://$host:$port/$command"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(response.body!!.string())
                } else {
                    Result.failure(Exception("服务器返回错误: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 测试与服务器的连接
     */
    suspend fun checkConnection(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://$host:$port/"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
