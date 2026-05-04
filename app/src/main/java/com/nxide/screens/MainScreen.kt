package com.nxide.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        topBar = {
            NxTopBar(
                activeTab = state.activeTab,
                onTabClick = { viewModel.setTab(it) },
                onSwitchClick = { viewModel.toggleSidebar() },
                onMoreClick = { viewModel.toggleSettings() },
                onSearchClick = { viewModel.toggleGlobalSearch() }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Global search bar
            if (state.showGlobalSearch) {
                GlobalSearchBar(
                    query = state.globalSearchQuery,
                    results = state.globalSearchResults,
                    onQueryChange = { viewModel.setGlobalSearchQuery(it) },
                    onSearch = { viewModel.performGlobalSearch() },
                    onClose = { viewModel.toggleGlobalSearch() },
                    onResultClick = { fileName, _ ->
                        viewModel.setActiveFile(fileName)
                        viewModel.toggleGlobalSearch()
                    }
                )
            }

            // Settings dialog
            if (state.showSettings) {
                SettingsDialog(
                    settings = state.settings,
                    onDismiss = { viewModel.toggleSettings() },
                    onFontSizeChange = { viewModel.updateFontSize(it) },
                    onTabSizeChange = { viewModel.updateTabSize(it) },
                    onWordWrapToggle = { viewModel.toggleWordWrap() },
                    onLineNumbersToggle = { viewModel.toggleLineNumbers() },
                    onAutoIndentToggle = { viewModel.toggleAutoIndent() }
                )
            }

            // Main content
            Box(modifier = Modifier.weight(1f)) {
                when (state.activeTab) {
                    MainTab.PROJECT -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // File explorer
                            if (state.sidebarOpen) {
                                FileExplorer(
                                    projectName = state.projectName,
                                    files = state.files,
                                    activeFile = state.activeFile,
                                    onFileClick = { viewModel.setActiveFile(it) },
                                    onFolderClick = { viewModel.toggleFolder(it) },
                                    onNewFile = { folder, name -> viewModel.createNewFile(folder, name) },
                                    onNewFolder = { folder, name -> viewModel.createNewFolder(folder, name) },
                                    onDeleteFile = { viewModel.deleteFile(it) },
                                    onRenameFile = { old, new -> viewModel.renameFile(old, new) }
                                )
                                VerticalDivider(color = NxBorder)
                            }

                            // Code editor + AI panel
                            Row(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Editor tabs
                                    EditorTabBar(
                                        tabs = state.editorTabs,
                                        activeFile = state.activeFile,
                                        onTabClick = { viewModel.setActiveFile(it) },
                                        onTabClose = { viewModel.closeTab(it) }
                                    )

                                    // Search bar
                                    if (state.searchState.isVisible) {
                                        SearchBar(
                                            searchState = state.searchState,
                                            onQueryChange = { viewModel.setSearchQuery(it) },
                                            onReplaceChange = { viewModel.setReplaceText(it) },
                                            onNext = { viewModel.nextSearchResult() },
                                            onPrev = { viewModel.prevSearchResult() },
                                            onReplace = { viewModel.replaceCurrent() },
                                            onReplaceAll = { viewModel.replaceAll() },
                                            onToggleCase = { viewModel.toggleSearchCaseSensitive() },
                                            onToggleRegex = { viewModel.toggleSearchRegex() },
                                            onClose = { viewModel.toggleSearchBar() }
                                        )
                                    }

                                    // Code editor
                                    CodeEditor(
                                        activeFile = state.activeFile,
                                        code = state.fileContents[state.activeFile] ?: "// 选择文件",
                                        language = state.editorTabs.find { it.fileName == state.activeFile }?.language ?: CodeLanguage.TEXT,
                                        onCodeChange = { viewModel.setFileContent(state.activeFile, it) },
                                        settings = state.settings,
                                        searchState = state.searchState,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (state.showAiPanel) {
                                    VerticalDivider(color = NxBorder)
                                    AiPanel(
                                        messages = state.aiMessages,
                                        prompt = state.aiPrompt,
                                        isThinking = state.isAiThinking,
                                        onPromptChange = { viewModel.setAiPrompt(it) },
                                        onSend = { viewModel.sendAiMessage() },
                                        onClose = { viewModel.toggleAiPanel() }
                                    )
                                }
                            }
                        }
                    }
                    MainTab.RECENT -> {
                        TemplateScreen(
                            templates = state.templates,
                            selectedCategory = state.templateCategory,
                            onCategorySelect = { viewModel.setTemplateCategory(it) },
                            onTemplateSelect = { }
                        )
                    }
                }
            }

            // Bottom panel
            state.activeBottomPanel?.let { panel ->
                BottomPanelContainer(
                    panelType = panel,
                    logs = state.logs,
                    buildSteps = state.buildSteps,
                    isBuilding = state.isBuilding,
                    terminalLines = state.terminalLines,
                    onClearLogs = { viewModel.clearLogs() },
                    onStartBuild = { viewModel.startBuild() },
                    onResetBuild = { viewModel.resetBuild() },
                    onClearTerminal = { viewModel.clearTerminal() },
                    onExecuteCommand = { viewModel.executeTerminalCommand(it) }
                )
            }

            // Bottom toolbar
            BottomToolbar(
                activePanel = state.activeBottomPanel,
                onPanelClick = { viewModel.toggleBottomPanel(it) },
                onAiClick = { viewModel.toggleAiPanel() }
            )
        }
    }
}

@Composable
private fun EditorTabBar(
    tabs: List<EditorTab>,
    activeFile: String,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    if (tabs.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(NxBgSecondary)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.fileName == activeFile
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(if (isActive) NxBgPrimary else NxBgSecondary)
                    .clickable { onTabClick(tab.fileName) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        tab.fileName.endsWith(".kt") -> "🟣"
                        tab.fileName.endsWith(".xml") -> "🔵"
                        tab.fileName.endsWith(".kts") -> "🟢"
                        else -> "📄"
                    },
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    tab.fileName,
                    fontSize = 12.sp,
                    color = if (isActive) NxTextPrimary else NxTextMuted,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
                if (tab.isModified) {
                    Spacer(Modifier.width(4.dp))
                    Text("●", fontSize = 10.sp, color = NxOrange)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "×",
                    fontSize = 14.sp,
                    color = NxTextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onTabClose(tab.fileName) }
                )
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(NxGreen)
    )
}

@Composable
private fun SearchBar(
    searchState: SearchState,
    onQueryChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onToggleCase: () -> Unit,
    onToggleRegex: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NxBgSecondary)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Search row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Search input
            BasicTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 12.sp, color = NxTextPrimary),
                cursorBrush = SolidColor(NxGreen),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NxBgInput)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (searchState.query.isEmpty()) {
                            Text("搜索...", fontSize = 12.sp, color = NxTextMuted)
                        }
                        inner()
                    }
                )
            )

            // Case sensitive toggle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (searchState.isCaseSensitive) NxGreen.copy(alpha = 0.2f) else NxBgInput)
                    .clickable { onToggleCase() },
                contentAlignment = Alignment.Center
            ) {
                Text("Aa", fontSize = 10.sp, color = if (searchState.isCaseSensitive) NxGreen else NxTextMuted)
            }

            // Regex toggle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (searchState.useRegex) NxGreen.copy(alpha = 0.2f) else NxBgInput)
                    .clickable { onToggleRegex() },
                contentAlignment = Alignment.Center
            ) {
                Text(".*", fontSize = 10.sp, color = if (searchState.useRegex) NxGreen else NxTextMuted)
            }

            // Match count
            if (searchState.query.isNotEmpty()) {
                Text(
                    "${searchState.currentMatch}/${searchState.matchCount}",
                    fontSize = 11.sp,
                    color = NxTextMuted
                )
            }

            // Navigation
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NxBgInput)
                    .clickable { onPrev() },
                contentAlignment = Alignment.Center
            ) {
                Text("▲", fontSize = 10.sp, color = NxTextMuted)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NxBgInput)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text("▼", fontSize = 10.sp, color = NxTextMuted)
            }

            // Close
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 14.sp, color = NxTextMuted)
            }
        }

        // Replace row
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicTextField(
                value = searchState.replaceText,
                onValueChange = onReplaceChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 12.sp, color = NxTextPrimary),
                cursorBrush = SolidColor(NxGreen),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NxBgInput)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (searchState.replaceText.isEmpty()) {
                            Text("替换...", fontSize = 12.sp, color = NxTextMuted)
                        }
                        inner()
                    }
                }
            )

            SmallTextButton("替换", onReplace)
            SmallTextButton("全部替换", onReplaceAll)
        }
    }
}

@Composable
private fun SmallTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NxBgInput)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = NxTextSecondary)
    }
}

@Composable
private fun GlobalSearchBar(
    query: String,
    results: List<com.nxide.data.SearchResult>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    onResultClick: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NxBgSecondary)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔍", fontSize = 14.sp)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 13.sp, color = NxTextPrimary),
                cursorBrush = SolidColor(NxGreen),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NxBgInput)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (query.isEmpty()) {
                            Text("全局搜索 (在所有文件中搜索)...", fontSize = 13.sp, color = NxTextMuted)
                        }
                        inner()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NxGreen)
                    .clickable { onSearch() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("搜索", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 18.sp, color = NxTextMuted)
            }
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "找到 ${results.size} 个结果",
                fontSize = 11.sp,
                color = NxTextMuted
            )
            Spacer(Modifier.height(4.dp))
            results.take(20).forEach { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onResultClick(result.fileName, result.lineNumber) }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        result.fileName,
                        fontSize = 11.sp,
                        color = NxGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(140.dp)
                    )
                    Text(
                        ":${result.lineNumber}",
                        fontSize = 11.sp,
                        color = NxTextMuted,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        result.lineContent,
                        fontSize = 11.sp,
                        color = NxTextSecondary,
                        maxLines = 1
                    )
                }
            }
            if (results.size > 20) {
                Text(
                    "... 还有 ${results.size - 20} 个结果",
                    fontSize = 11.sp,
                    color = NxTextMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        } else if (query.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("未找到结果", fontSize = 12.sp, color = NxTextMuted)
        }
    }
    HorizontalDivider(color = NxBorder)
}

@Composable
private fun SettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onTabSizeChange: (Int) -> Unit,
    onWordWrapToggle: () -> Unit,
    onLineNumbersToggle: () -> Unit,
    onAutoIndentToggle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NxBgSecondary,
        titleContentColor = NxTextPrimary,
        textContentColor = NxTextSecondary,
        title = { Text("⚙️ 设置", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Font size
                SettingSlider("字体大小", settings.fontSize, 8, 24) { onFontSizeChange(it) }

                // Tab size
                SettingSlider("Tab 宽度", settings.tabSize, 2, 8) { onTabSizeChange(it) }

                // Toggles
                SettingToggle("显示行号", settings.showLineNumbers) { onLineNumbersToggle() }
                SettingToggle("自动换行", settings.wordWrap) { onWordWrapToggle() }
                SettingToggle("自动缩进", settings.autoIndent) { onAutoIndentToggle() }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NxGreen)
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("完成", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NxBgPrimary)
            }
        }
    )
}

@Composable
private fun SettingSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = NxTextPrimary)
            Text("$value", fontSize = 13.sp, color = NxGreen, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min - 1,
            colors = SliderDefaults.colors(
                thumbColor = NxGreen,
                activeTrackColor = NxGreen,
                inactiveTrackColor = NxBgInput
            )
        )
    }
}

@Composable
private fun SettingToggle(label: String, value: Boolean, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = NxTextPrimary)
        Switch(
            checked = value,
            onCheckedChange = { onChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = NxGreen,
                checkedTrackColor = NxGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = NxTextMuted,
                uncheckedTrackColor = NxBgInput
            )
        )
    }
}
