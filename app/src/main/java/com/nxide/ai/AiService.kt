package com.nxide.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * AI 服务 —— 支持 OpenAI 兼容接口的 SSE 流式请求
 */
class AiService(private val config: AiConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 流式请求，通过 callback 逐块返回内容
     */
    suspend fun streamChat(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val requestBody = buildRequestJson(messages, stream = true)

        val request = Request.Builder()
            .url(config.apiEndpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)

        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) {
                        onError(mapNetworkError(e))
                        continuation.resume(Unit)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        val errorMsg = parseApiError(response.code, errorBody)
                        if (!continuation.isCancelled) {
                            onError(errorMsg)
                            continuation.resume(Unit)
                        }
                        return
                    }

                    try {
                        val reader = BufferedReader(
                            InputStreamReader(response.body!!.byteStream())
                        )
                        val fullContent = StringBuilder()

                        reader.use { r ->
                            var line: String?
                            while (r.readLine().also { line = it } != null) {
                                if (continuation.isCancelled) break

                                val l = line ?: continue
                                if (!l.startsWith("data: ")) continue
                                val data = l.removePrefix("data: ").trim()
                                if (data == "[DONE]") break

                                try {
                                    val json = JSONObject(data)
                                    val choices = json.optJSONArray("choices") ?: continue
                                    if (choices.length() == 0) continue

                                    val delta = choices.getJSONObject(0)
                                        .optJSONObject("delta") ?: continue
                                    val content = delta.optString("content", "")
                                    if (content.isNotEmpty()) {
                                        fullContent.append(content)
                                        onChunk(content)
                                    }
                                } catch (e: Exception) {
                                    // 跳过解析失败的行
                                }
                            }
                        }

                        if (!continuation.isCancelled) {
                            onComplete(fullContent.toString())
                            continuation.resume(Unit)
                        }
                    } catch (e: Exception) {
                        if (!continuation.isCancelled) {
                            onError("读取响应失败: ${e.message}")
                            continuation.resume(Unit)
                        }
                    }
                }
            })
        }
    }

    /**
     * 非流式请求，一次性返回完整内容
     */
    suspend fun chat(messages: List<ChatMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            val requestBody = buildRequestJson(messages, stream = false)

            val request = Request.Builder()
                .url(config.apiEndpoint)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(
                        Exception(parseApiError(response.code, errorBody))
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("空响应"))

                val json = JSONObject(body)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Result.success(content)
            } catch (e: IOException) {
                Result.failure(Exception(mapNetworkError(e)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildRequestJson(
        messages: List<ChatMessage>,
        stream: Boolean
    ): JSONObject {
        val json = JSONObject()
        json.put("model", config.model)
        json.put("stream", stream)
        json.put("max_tokens", config.maxTokens)
        json.put("temperature", config.temperature)

        val messagesArray = JSONArray()
        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            msgObj.put("content", msg.content)
            messagesArray.put(msgObj)
        }
        json.put("messages", messagesArray)
        return json
    }

    private fun mapNetworkError(e: IOException): String = when {
        e.message?.contains("timeout", true) == true -> "请求超时，请检查网络连接"
        e.message?.contains("connect", true) == true -> "无法连接到 API 服务器，请检查地址"
        e.message?.contains("resolve", true) == true -> "DNS 解析失败，请检查 API 地址"
        else -> "网络错误: ${e.message}"
    }

    private fun parseApiError(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: "未知错误"
            "API 错误 ($code): $message"
        } catch (e: Exception) {
            "API 错误 ($code): $body"
        }
    }

    data class ChatMessage(
        val role: String, // "system", "user", "assistant"
        val content: String
    )
}
