package com.nxide.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.ai.AiConfig
import com.nxide.ai.ApiPreset
import com.nxide.ui.theme.*

@Composable
fun SettingsScreen(
    config: AiConfig,
    onSave: (AiConfig) -> Unit,
    onTest: (() -> Unit)? = null,
    testResult: String? = null
) {
    var endpoint by remember(config) { mutableStateOf(config.apiEndpoint) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var maxTokens by remember(config) { mutableStateOf(config.maxTokens.toString()) }
    var temperature by remember(config) { mutableStateOf(config.temperature.toString()) }
    var showKey by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableIntStateOf(-1) }
    var showSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NxBgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Text(
            "⚙️ 设置",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NxTextPrimary
        )

        // ===== AI API Section =====
        SectionCard {
            Text(
                "🤖 AI 模型配置",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NxGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "配置 OpenAI 兼容的 API 接口，支持 OpenAI、DeepSeek、通义千问等",
                fontSize = 12.sp,
                color = NxTextMuted
            )

            Spacer(Modifier.height(16.dp))

            // Quick presets
            Text("快速选择", fontSize = 12.sp, color = NxTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AiConfig.PRESETS.take(5).forEachIndexed { index, preset ->
                    val isSelected = selectedPreset == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) NxGreen.copy(alpha = 0.15f)
                                else NxBgInput
                            )
                            .clickable {
                                selectedPreset = index
                                endpoint = preset.endpoint
                                if (preset.defaultModel.isNotEmpty()) {
                                    model = preset.defaultModel
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            preset.name,
                            fontSize = 12.sp,
                            color = if (isSelected) NxGreen else NxTextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // API Endpoint
            LabeledInput(
                label = "API 地址",
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    selectedPreset = -1
                },
                placeholder = "https://api.openai.com/v1/chat/completions"
            )

            // API Key
            Spacer(Modifier.height(12.dp))
            Text("API Key", fontSize = 12.sp, color = NxTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        color = NxTextPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(NxGreen),
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NxBgInput)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (apiKey.isEmpty()) {
                                Text("sk-...", fontSize = 13.sp, color = NxTextMuted)
                            }
                            innerTextField()
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NxBgInput)
                        .clickable { showKey = !showKey }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (showKey) "🙈" else "👁️",
                        fontSize = 14.sp
                    )
                }
            }

            // Model
            Spacer(Modifier.height(12.dp))
            LabeledInput(
                label = "模型",
                value = model,
                onValueChange = { model = it },
                placeholder = "gpt-4o-mini"
            )

            // Advanced
            Spacer(Modifier.height(16.dp))
            Text("高级设置", fontSize = 12.sp, color = NxTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Max Tokens", fontSize = 11.sp, color = NxTextMuted)
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                        textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(NxGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NxBgInput)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) { inner() }
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Temperature", fontSize = 11.sp, color = NxTextMuted)
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = temperature,
                        onValueChange = { temperature = it.filter { c -> c.isDigit() || c == '.' } },
                        textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(NxGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NxBgInput)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) { inner() }
                        }
                    )
                }
            }

            // Buttons
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NxGreen)
                        .clickable {
                            onSave(
                                AiConfig(
                                    apiEndpoint = endpoint.ifBlank { AiConfig.DEFAULT_ENDPOINT },
                                    apiKey = apiKey,
                                    model = model.ifBlank { AiConfig.DEFAULT_MODEL },
                                    maxTokens = maxTokens.toIntOrNull() ?: 4096,
                                    temperature = temperature.toDoubleOrNull() ?: 0.7
                                )
                            )
                            showSaved = true
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "💾 保存配置",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NxBgPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NxBgInput)
                        .clickable { onTest?.invoke() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🧪 测试连接",
                        fontSize = 14.sp,
                        color = NxTextSecondary
                    )
                }
            }

            // Save indicator
            if (showSaved) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSaved = false
                }
                Text(
                    "✅ 配置已保存",
                    fontSize = 12.sp,
                    color = NxGreen
                )
            }

            // Test result
            testResult?.let {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (it.startsWith("✅")) NxGreen.copy(alpha = 0.1f)
                            else NxRed.copy(alpha = 0.1f)
                        )
                        .padding(12.dp)
                ) {
                    Text(it, fontSize = 12.sp, color = if (it.startsWith("✅")) NxGreen else NxRed)
                }
            }
        }

        // ===== Info Section =====
        SectionCard {
            Text(
                "💡 使用说明",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NxBlue
            )
            Spacer(Modifier.height(8.dp))
            InfoItem("支持所有 OpenAI 兼容的 API 接口")
            InfoItem("API Key 仅存储在本地设备，不会上传")
            InfoItem("流式输出可实时查看 AI 响应")
            InfoItem("对话会自动携带当前文件作为上下文")
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NxBgSecondary)
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(label, fontSize = 12.sp, color = NxTextSecondary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 13.sp,
                color = NxTextPrimary,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(NxGreen),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NxBgInput)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 13.sp, color = NxTextMuted)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun InfoItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", fontSize = 12.sp, color = NxGreen)
        Text(text, fontSize = 12.sp, color = NxTextSecondary)
    }
}
