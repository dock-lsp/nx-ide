package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.BottomPanelType
import com.nxide.ui.theme.*

@Composable
fun BottomToolbar(
    activePanel: BottomPanelType?,
    onPanelClick: (BottomPanelType) -> Unit,
    onAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(NxBgSecondary)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left panels - scrollable row
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.weight(1f)
        ) {
            BottomPanelType.values().forEach { panel ->
                val isActive = panel == activePanel
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) NxBgTertiary else NxBorder.copy(alpha = 0f))
                        .clickable { onPanelClick(panel) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${panel.icon} ${panel.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) NxGreen else NxTextSecondary
                    )
                }
            }
        }

        // AI button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(NxGreen.copy(alpha = 0.1f), NxBlue.copy(alpha = 0.1f))
                    )
                )
                .clickable { onAiClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖 AI", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NxGreen)
        }
    }
}
