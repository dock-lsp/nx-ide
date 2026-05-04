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
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "NX IDE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NxTextPrimary
                )
            }
        },
        actions = {
            // Sidebar toggle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NxBgInput)
                    .clickable { onSwitchClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("☰", fontSize = 16.sp, color = NxTextSecondary)
            }
            Spacer(Modifier.width(6.dp))

            // Tab buttons - compact
            Row(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                MainTab.values().forEach { tab ->
                    if (tab == MainTab.SETTINGS) return@forEach // Skip settings tab, use icon instead
                    val isActive = tab == activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) NxBgTertiary else NxBorder.copy(alpha = 0.3f))
                            .clickable { onTabClick(tab) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${tab.icon} ${tab.label}",
                            fontSize = 12.sp,
                            color = if (isActive) NxGreen else NxTextSecondary,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // Settings button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NxBgInput)
                    .clickable { onTabClick(MainTab.SETTINGS) },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙️", fontSize = 16.sp)
            }

            Spacer(Modifier.width(4.dp))

            // More button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NxBgInput)
                    .clickable { onMoreClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("⋮", fontSize = 16.sp, color = NxTextSecondary)
            }
            Spacer(Modifier.width(4.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NxBgSecondary
        )
    )
}
