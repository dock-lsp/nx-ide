package com.nxide.ai

/**
 * AI API 配置
 * 支持 OpenAI 兼容接口（OpenAI、DeepSeek、通义千问、Moonshot 等）
 */
data class AiConfig(
    val apiEndpoint: String = DEFAULT_ENDPOINT,
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"

        // 预设的 API 提供商
        val PRESETS = listOf(
            ApiPreset("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
            ApiPreset("DeepSeek", "https://api.deepseek.com/v1/chat/completions", "deepseek-chat"),
            ApiPreset("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-turbo"),
            ApiPreset("Moonshot", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k"),
            ApiPreset("硅基流动", "https://api.siliconflow.cn/v1/chat/completions", "Qwen/Qwen2.5-7B-Instruct"),
            ApiPreset("自定义", "", "")
        )
    }
}

data class ApiPreset(
    val name: String,
    val endpoint: String,
    val defaultModel: String
)
