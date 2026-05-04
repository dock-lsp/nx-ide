package com.nxide.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxide.data.BottomPanelType
import com.nxide.data.BuildStep
import com.nxide.data.LogEntry
import com.nxide.data.TerminalLine
import com.nxide.ui.theme.NxBgPrimary
import com.nxide.ui.theme.NxBorder
import com.nxide.components.panels.*

@Composable
fun BottomPanelContainer(
    panelType: BottomPanelType,
    logs: List<LogEntry>,
    buildSteps: List<BuildStep>,
    isBuilding: Boolean,
    buildSummary: String = "点击 ▶ 运行 开始构建",
    terminalLines: List<TerminalLine>,
    onClearLogs: () -> Unit,
    onStartBuild: () -> Unit,
    onStopBuild: () -> Unit = {},
    onResetBuild: () -> Unit,
    onClearTerminal: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 180.dp, max = 260.dp)
            .background(NxBgPrimary)
    ) {
        HorizontalDivider(color = NxBorder, thickness = 2.dp)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (panelType) {
                BottomPanelType.LAYOUT -> LayoutPanel()
                BottomPanelType.BUILD -> BuildPanel(
                    steps = buildSteps,
                    isBuilding = isBuilding,
                    buildSummary = buildSummary,
                    onStartBuild = onStartBuild,
                    onStopBuild = onStopBuild,
                    onResetBuild = onResetBuild
                )
                BottomPanelType.LOGCAT -> LogcatPanel(
                    logs = logs,
                    onClear = onClearLogs
                )
                BottomPanelType.FILES -> FilesPanel()
                BottomPanelType.TERMINAL -> TerminalPanel(
                    lines = terminalLines,
                    onClear = onClearTerminal,
                    onExecute = onExecuteCommand
                )
            }
        }
    }
}
