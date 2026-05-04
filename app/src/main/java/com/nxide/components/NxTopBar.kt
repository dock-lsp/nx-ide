package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nxide.data.MainTab
import com.nxide.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NxTopBar(
    activeTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    onSwitchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "NX IDE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NxTextPrimary
                )
            }
        },
        actions = {
            // Tab buttons
            Row(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MainTab.values().forEach { tab ->
                    val isActive = tab == activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) NxBgTertiary else NxBorder.copy(alpha = 0.3f))
                            .clickable { onTabClick(tab) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${tab.icon} ${tab.label}",
                            fontSize = 13.sp,
                            color = if (isActive) NxGreen else NxTextSecondary,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSwitchClick) {
                Icon(Icons.Default.SwapHoriz, "切换", tint = NxTextSecondary)
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, "更多", tint = NxTextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NxBgSecondary
        )
    )
}
