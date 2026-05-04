package com.nxide.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nxide.components.*
import com.nxide.data.*
import com.nxide.ui.theme.*
import com.nxide.viewmodel.NxIdeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NxIdeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val isNarrowScreen = configuration.screenWidthDp < 600

    // On narrow screens, default sidebar to closed
    LaunchedEffect(isNarrowScreen) {
        if (isNarrowScreen && state.sidebarOpen) {
            viewModel.toggleSidebar()
        }
    }

    Scaffold(
        topBar = {
            NxTopBar(
                activeTab = state.activeTab,
                onTabClick = { tab ->
                    if (tab == MainTab.SETTINGS) viewModel.toggleSettings()
                    else viewModel.setTab(tab)
                },
                onSwitchClick = { viewModel.toggleSidebar() },
                onMoreClick = { }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Main content - takes all available space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.showSettings || state.activeTab == MainTab.SETTINGS -> {
                        SettingsScreen(
                            config = state.aiConfig,
                            onSave = { viewModel.saveAiConfig(it) },
                            onTest = { viewModel.testAiConnection() },
                            testResult = state.testResult
                        )
                    }
                    state.activeTab == MainTab.PROJECT -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // File explorer - collapsible
                            if (state.sidebarOpen && !isNarrowScreen) {
                                FileExplorer(
                                    projectName = state.projectName,
                                    files = state.files,
                                    activeFile = state.activeFile,
                                    onFileClick = { viewModel.setActiveFile(it) },
                                    onFolderClick = { viewModel.toggleFolder(it) }
                                )
                                VerticalDivider(color = NxBorder)
                            }

                            // Code editor + AI panel
                            Row(modifier = Modifier.weight(1f)) {
                                CodeEditor(
                                    activeFile = state.activeFile,
                                    code = state.fileContents[state.activeFile] ?: "// 选择文件",
                                    language = state.files.flatMap { getAllFiles(it) }
                                        .find { it.name == state.activeFile }?.language ?: CodeLanguage.TEXT,
                                    onCodeChange = { viewModel.setFileContent(state.activeFile, it) },
                                    modifier = Modifier.weight(1f)
                                )

                                if (state.showAiPanel) {
                                    VerticalDivider(color = NxBorder)
                                    AiPanel(
                                        messages = state.aiMessages,
                                        prompt = state.aiPrompt,
                                        isThinking = state.isAiThinking,
                                        streamingContent = state.aiStreamingContent,
                                        onPromptChange = { viewModel.setAiPrompt(it) },
                                        onSend = { viewModel.sendAiMessage() },
                                        onStop = { viewModel.stopAiStream() },
                                        onClose = { viewModel.toggleAiPanel() }
                                    )
                                }
                            }
                        }
                    }
                    state.activeTab == MainTab.RECENT -> {
                        TemplateScreen(
                            templates = state.templates,
                            selectedCategory = state.templateCategory,
                            onCategorySelect = { viewModel.setTemplateCategory(it) },
                            onTemplateSelect = { }
                        )
                    }
                }
            }

            // Bottom panel - only show in project tab and when not in settings
            if (state.activeTab == MainTab.PROJECT && !state.showSettings) {
                state.activeBottomPanel?.let { panel ->
                    BottomPanelContainer(
                        panelType = panel,
                        logs = state.logs,
                        buildSteps = state.buildSteps,
                        isBuilding = state.isBuilding,
                        buildSummary = state.buildSummary,
                        terminalLines = state.terminalLines,
                        onClearLogs = { viewModel.clearLogs() },
                        onStartBuild = { viewModel.startBuild() },
                        onStopBuild = { viewModel.stopBuild() },
                        onResetBuild = { viewModel.resetBuild() },
                        onClearTerminal = { viewModel.clearTerminal() },
                        onExecuteCommand = { viewModel.executeTerminalCommand(it) }
                    )
                }
            }

            // Bottom toolbar - always visible except in settings
            if (!state.showSettings && state.activeTab != MainTab.SETTINGS) {
                BottomToolbar(
                    activePanel = state.activeBottomPanel,
                    onPanelClick = { viewModel.toggleBottomPanel(it) },
                    onAiClick = { viewModel.toggleAiPanel() }
                )
            }
        }
    }
}

private fun getAllFiles(node: FileNode): List<FileNode> {
    val result = mutableListOf<FileNode>()
    if (node.type == FileType.FILE) result.add(node)
    node.children.forEach { result.addAll(getAllFiles(it)) }
    return result
}
