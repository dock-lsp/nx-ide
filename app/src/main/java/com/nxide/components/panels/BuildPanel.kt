package com.nxide.components.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.BuildStatus
import com.nxide.data.BuildStep
import com.nxide.ui.theme.*

@Composable
fun BuildPanel(
    steps: List<BuildStep>,
    isBuilding: Boolean,
    buildSummary: String = "点击 ▶ 运行 开始构建",
    onStartBuild: () -> Unit,
    onStopBuild: () -> Unit = {},
    onResetBuild: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NxBgSecondary)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔨 Build Output", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NxTextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isBuilding) {
                    SmallButton("⏹ 停止", onStopBuild)
                } else {
                    SmallButton("▶ 运行", onStartBuild)
                }
                SmallButton("🗑️ 清除", onResetBuild)
            }
        }

        HorizontalDivider(color = NxBorder)

        // Steps
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(steps) { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        when (step.status) {
                            BuildStatus.PENDING -> "⏳"
                            BuildStatus.RUNNING -> "🔄"
                            BuildStatus.SUCCESS -> "✅"
                            BuildStatus.ERROR -> "❌"
                        },
                        fontSize = 12.sp
                    )
                    Text(
                        step.name,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when (step.status) {
                            BuildStatus.PENDING -> NxTextMuted
                            BuildStatus.RUNNING -> NxBlue
                            BuildStatus.SUCCESS -> NxGreen
                            BuildStatus.ERROR -> NxRed
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    step.duration?.let {
                        Text(it, fontSize = 11.sp, color = NxTextMuted, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NxBgSecondary)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                buildSummary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = when {
                    isBuilding -> NxBlue
                    buildSummary.contains("SUCCESS") -> NxGreen
                    buildSummary.contains("FAILED") || buildSummary.contains("错误") -> NxRed
                    else -> NxTextMuted
                }
            )
        }
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(NxBgInput)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = if (enabled) NxTextSecondary else NxTextMuted
        )
    }
}
