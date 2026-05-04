package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.AiMessage
import com.nxide.data.MessageRole
import com.nxide.ui.theme.*

@Composable
fun AiPanel(
    messages: List<AiMessage>,
    prompt: String,
    isThinking: Boolean,
    streamingContent: String = "",
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(messages.size, streamingContent.length) {
        val totalItems = messages.size + if (streamingContent.isNotEmpty() || isThinking) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .width(380.dp)
            .fillMaxHeight()
            .background(NxBgSecondary)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(NxGreen.copy(alpha = 0.1f), NxBlue.copy(alpha = 0.1f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖 AI 助手", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NxGreen)
                if (isThinking) {
                    Spacer(Modifier.width(8.dp))
                    Text("思考中...", fontSize = 11.sp, color = NxBlue)
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 20.sp, color = NxTextMuted)
            }
        }

        HorizontalDivider(color = NxBorder)

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && streamingContent.isEmpty()) {
                item {
                    WelcomeContent(onPromptChange)
                }
            }

            items(messages) { msg ->
                MessageBubble(msg)
            }

            // Streaming content (not yet committed to messages)
            if (streamingContent.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AiAvatar()
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NxBgInput)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                streamingContent + "▌",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NxTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Thinking indicator
            if (isThinking && streamingContent.isEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AiAvatar()
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NxBgInput)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("思考中 ●●●", fontSize = 13.sp, color = NxGreen)
                        }
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NxBgPrimary)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary),
                cursorBrush = SolidColor(NxGreen),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NxBgInput)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (prompt.isEmpty()) {
                            Text("输入你的问题...", fontSize = 13.sp, color = NxTextMuted)
                        }
                        innerTextField()
                    }
                }
            )

            if (isThinking) {
                // Stop button during streaming
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NxRed.copy(alpha = 0.2f))
                        .clickable { onStop() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("⏹", fontSize = 13.sp, color = NxRed)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NxGreen)
                        .clickable { onSend() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("发送", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(onPromptChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(NxGreen, NxBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 20.sp)
        }

        Column {
            Text("AI 助手", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NxTextPrimary)
            Text(
                "我可以帮你编写代码、调试问题、优化架构。试试这些：",
                fontSize = 13.sp,
                color = NxTextMuted,
                lineHeight = 18.sp
            )
        }

        val suggestions = listOf(
            "💡 为 MainActivity 添加底部导航栏",
            "🎨 优化 Compose 主题配色方案",
            "📦 添加 Room 数据库支持",
            "🔧 重构代码为 MVVM 架构",
            "🧪 生成单元测试代码",
            "📱 添加响应式布局适配",
        )
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NxBgInput)
                    .clickable { onPromptChange(suggestion.drop(2).trim()) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(suggestion, fontSize = 13.sp, color = NxTextSecondary)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.role == MessageRole.USER) Arrangement.End else Arrangement.Start
    ) {
        if (msg.role == MessageRole.AI) {
            AiAvatar()
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (msg.role == MessageRole.USER) NxGreen else NxBgInput)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                msg.content,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = if (msg.role == MessageRole.USER) NxBgPrimary else NxTextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun AiAvatar() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(NxGreen, NxBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("🤖", fontSize = 14.sp)
    }
}
