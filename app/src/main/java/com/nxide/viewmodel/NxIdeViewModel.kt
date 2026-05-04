package com.nxide.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nxide.ai.AiConfig
import com.nxide.ai.AiConfigStore
import com.nxide.ai.AiService
import com.nxide.data.*
import com.nxide.terminal.BuildExecutor
import com.nxide.terminal.ProcessExecutor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NxIdeState(
    val activeTab: MainTab = MainTab.PROJECT,
    val activeBottomPanel: BottomPanelType? = null,
    val projectName: String = DefaultProject.projectName,
    val files: List<FileNode> = DefaultProject.files,
    val activeFile: String = "MainActivity.kt",
    val fileContents: Map<String, String> = mapOf(
        "MainActivity.kt" to SampleCode.MAIN_ACTIVITY,
        "Color.kt" to SampleCode.COLOR_KT,
        "Theme.kt" to SampleCode.THEME_KT,
        "Type.kt" to SampleCode.TYPE_KT,
        "AndroidManifest.xml" to SampleCode.ANDROID_MANIFEST,
        "strings.xml" to SampleCode.STRINGS_XML,
        "themes.xml" to SampleCode.THEMES_XML,
        "build.gradle.kts (app)" to SampleCode.APP_BUILD_GRADLE,
        "build.gradle.kts" to SampleCode.ROOT_BUILD_GRADLE,
        "settings.gradle.kts" to SampleCode.SETTINGS_GRADLE,
        "gradle.properties" to SampleCode.GRADLE_PROPERTIES,
    ),
    val templateCategory: TemplateCategory = TemplateCategory.ALL,
    val templates: List<ProjectTemplate> = DefaultProject.templates,
    val logs: List<LogEntry> = DefaultProject.defaultLogs,
    val buildSteps: List<BuildStep> = DefaultProject.buildSteps,
    val isBuilding: Boolean = false,
    val buildSummary: String = "点击 ▶ 运行 开始构建",
    val terminalLines: List<TerminalLine> = listOf(TerminalLine("$ ", isCommand = true)),
    val sidebarOpen: Boolean = true,
    val aiPrompt: String = "",
    val aiMessages: List<AiMessage> = emptyList(),
    val showAiPanel: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiStreamingContent: String = "",
    // Settings
    val aiConfig: AiConfig = AiConfig(),
    val showSettings: Boolean = false,
    val testResult: String? = null
)

class NxIdeViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(NxIdeState())
    val state: StateFlow<NxIdeState> = _state.asStateFlow()

    // --- Real modules ---
    private val processExecutor = ProcessExecutor()
    private val buildExecutor = BuildExecutor(processExecutor)
    private val aiConfigStore = AiConfigStore(application)

    private var aiStreamJob: Job? = null

    init {
        // Load AI config
        viewModelScope.launch {
            aiConfigStore.config.collect { config ->
                _state.update { it.copy(aiConfig = config) }
            }
        }

        // Collect terminal events
        viewModelScope.launch {
            processExecutor.events.collect { event ->
                when (event) {
                    is ProcessExecutor.OutputEvent.Stdout -> {
                        addTerminalLine(event.text, isCommand = false)
                    }
                    is ProcessExecutor.OutputEvent.Stderr -> {
                        addTerminalLine("[ERR] ${event.text}", isCommand = false)
                    }
                    is ProcessExecutor.OutputEvent.Exit -> {
                        if (event.code != 0) {
                            addTerminalLine("Process exited with code ${event.code}", isCommand = false)
                        }
                        addTerminalLine("$ ", isCommand = true)
                    }
                    is ProcessExecutor.OutputEvent.Error -> {
                        addTerminalLine("[ERROR] ${event.message}", isCommand = false)
                        addTerminalLine("$ ", isCommand = true)
                    }
                }
            }
        }

        // Collect build state
        viewModelScope.launch {
            buildExecutor.state.collect { buildState ->
                _state.update {
                    it.copy(
                        isBuilding = buildState.isBuilding,
                        buildSteps = buildState.steps.ifEmpty { it.buildSteps },
                        buildSummary = buildState.summary
                    )
                }
            }
        }
    }

    // ==================== Tab / UI ====================

    fun setTab(tab: MainTab) {
        _state.update { it.copy(activeTab = tab, showSettings = false) }
    }

    fun toggleSettings() {
        _state.update { it.copy(showSettings = !it.showSettings, activeTab = if (!it.showSettings) MainTab.SETTINGS else MainTab.PROJECT) }
    }

    fun toggleBottomPanel(panel: BottomPanelType) {
        _state.update { current ->
            current.copy(
                activeBottomPanel = if (current.activeBottomPanel == panel) null else panel
            )
        }
    }

    fun setActiveFile(fileName: String) {
        _state.update { it.copy(activeFile = fileName) }
    }

    fun setFileContent(fileName: String, content: String) {
        _state.update { current ->
            current.copy(fileContents = current.fileContents + (fileName to content))
        }
    }

    fun toggleFolder(folderName: String) {
        _state.update { current ->
            current.copy(files = toggleFolderRecursive(current.files, folderName))
        }
    }

    private fun toggleFolderRecursive(nodes: List<FileNode>, name: String): List<FileNode> {
        return nodes.map { node ->
            if (node.name == name && node.type == FileType.FOLDER) {
                node.copy(isExpanded = !node.isExpanded)
            } else if (node.children.isNotEmpty()) {
                node.copy(children = toggleFolderRecursive(node.children, name))
            } else {
                node
            }
        }
    }

    fun setTemplateCategory(category: TemplateCategory) {
        _state.update { it.copy(templateCategory = category) }
    }

    fun toggleSidebar() {
        _state.update { it.copy(sidebarOpen = !it.sidebarOpen) }
    }

    // ==================== Logs ====================

    fun addLog(level: LogLevel, tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _state.update { current ->
            current.copy(logs = current.logs + LogEntry(time = time, level = level, tag = tag, message = message))
        }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
    }

    // ==================== Build (Real) ====================

    fun startBuild() {
        val current = _state.value
        if (current.isBuilding) return

        // Show build panel
        _state.update { it.copy(activeBottomPanel = BottomPanelType.BUILD) }

        // Try to find project path - look for gradlew in common locations
        val projectPath = findProjectPath()

        if (projectPath != null) {
            buildExecutor.startBuild(projectPath)
        } else {
            // Fallback: simulate build if no real project found
            simulateBuild()
        }
    }

    private fun findProjectPath(): String? {
        // Check common locations for Android projects with gradlew
        val candidates = listOf(
            "/sdcard/NxProjects/current",
            "${getApplication<Application>().filesDir.parentFile?.parentFile?.absolutePath}/project",
            System.getenv("NX_PROJECT_PATH")
        )
        return candidates.firstOrNull { path ->
            path != null && java.io.File(path, "gradlew").canExecute()
        }
    }

    private fun simulateBuild() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBuilding = true,
                    buildSteps = DefaultProject.buildSteps.map { s -> s.copy(status = BuildStatus.PENDING) },
                    buildSummary = "⏳ 正在构建..."
                )
            }

            val durations = listOf(100L, 300L, 500L, 200L, 150L, 100L, 400L, 300L)
            val steps = DefaultProject.buildSteps

            for (i in steps.indices) {
                _state.update { current ->
                    current.copy(
                        buildSteps = current.buildSteps.map { s ->
                            if (s.id == steps[i].id) s.copy(status = BuildStatus.RUNNING) else s
                        }
                    )
                }
                kotlinx.coroutines.delay(durations[i] + kotlin.random.Random.nextInt(200).toLong())
                _state.update { current ->
                    current.copy(
                        buildSteps = current.buildSteps.map { s ->
                            if (s.id == steps[i].id) s.copy(
                                status = BuildStatus.SUCCESS,
                                duration = String.format("%.1fs", durations[i] / 1000.0)
                            ) else s
                        }
                    )
                }
            }

            _state.update {
                it.copy(
                    isBuilding = false,
                    buildSummary = "✅ BUILD SUCCESSFUL (模拟)"
                )
            }
            addLog(LogLevel.INFO, "Build", "BUILD SUCCESSFUL (未找到 gradlew，使用模拟构建)")
        }
    }

    fun stopBuild() {
        buildExecutor.stopBuild()
        processExecutor.kill()
    }

    fun resetBuild() {
        buildExecutor.reset()
        _state.update {
            it.copy(
                buildSteps = DefaultProject.buildSteps.map { s -> s.copy(status = BuildStatus.PENDING, duration = null) },
                isBuilding = false,
                buildSummary = "点击 ▶ 运行 开始构建"
            )
        }
    }

    // ==================== Terminal (Real) ====================

    fun clearTerminal() {
        _state.update { it.copy(terminalLines = listOf(TerminalLine("$ ", isCommand = true))) }
    }

    fun executeTerminalCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        // Add command to terminal display
        _state.update { current ->
            current.copy(
                terminalLines = current.terminalLines + TerminalLine("$ $trimmed", isCommand = true)
            )
        }

        when {
            trimmed.equals("clear", ignoreCase = true) -> clearTerminal()
            trimmed.startsWith("cd ") -> {
                // Handle cd by changing working directory
                val dir = trimmed.removePrefix("cd ").trim()
                addTerminalLine("(cd is handled internally; use absolute paths with other commands)", isCommand = false)
                addTerminalLine("$ ", isCommand = true)
            }
            else -> {
                // Execute real command
                viewModelScope.launch {
                    processExecutor.execute(trimmed)
                }
            }
        }
    }

    private fun addTerminalLine(text: String, isCommand: Boolean) {
        _state.update { current ->
            current.copy(terminalLines = current.terminalLines + TerminalLine(text, isCommand))
        }
    }

    // ==================== AI (Real) ====================

    fun setAiPrompt(prompt: String) {
        _state.update { it.copy(aiPrompt = prompt) }
    }

    fun toggleAiPanel() {
        _state.update { it.copy(showAiPanel = !it.showAiPanel) }
    }

    fun sendAiMessage() {
        val prompt = _state.value.aiPrompt.trim()
        if (prompt.isEmpty()) return

        val config = _state.value.aiConfig
        if (!config.isConfigured) {
            _state.update { current ->
                current.copy(
                    aiMessages = current.aiMessages + listOf(
                        AiMessage(role = MessageRole.USER, content = prompt),
                        AiMessage(role = MessageRole.AI, content = "⚠️ 请先在设置中配置 API Key\n\n点击顶部 ⚙️ 按钮进入设置")
                    ),
                    aiPrompt = ""
                )
            }
            return
        }

        // Add user message
        _state.update { current ->
            current.copy(
                aiMessages = current.aiMessages + AiMessage(role = MessageRole.USER, content = prompt),
                aiPrompt = "",
                isAiThinking = true,
                aiStreamingContent = ""
            )
        }

        // Cancel previous stream if any
        aiStreamJob?.cancel()

        aiStreamJob = viewModelScope.launch {
            val service = AiService(config)

            // Build messages with system prompt and context
            val messages = buildAiMessages(prompt, config)

            service.streamChat(
                messages = messages,
                onChunk = { chunk ->
                    _state.update { it.copy(aiStreamingContent = it.aiStreamingContent + chunk) }
                },
                onComplete = { fullContent ->
                    _state.update { current ->
                        current.copy(
                            aiMessages = current.aiMessages + AiMessage(
                                role = MessageRole.AI,
                                content = fullContent
                            ),
                            isAiThinking = false,
                            aiStreamingContent = ""
                        )
                    }
                },
                onError = { error ->
                    _state.update { current ->
                        current.copy(
                            aiMessages = current.aiMessages + AiMessage(
                                role = MessageRole.AI,
                                content = "❌ $error"
                            ),
                            isAiThinking = false,
                            aiStreamingContent = ""
                        )
                    }
                }
            )
        }
    }

    private fun buildAiMessages(userPrompt: String, config: AiConfig): List<AiService.ChatMessage> {
        val messages = mutableListOf<AiService.ChatMessage>()

        // System prompt
        messages.add(
            AiService.ChatMessage(
                role = "system",
                content = """你是 NX IDE 的 AI 编程助手。你帮助用户编写、调试和优化 Android/Kotlin 代码。
当前项目使用 Jetpack Compose + MVVM 架构。
回答要简洁、实用，代码用 markdown 代码块格式。"""
            )
        )

        // Add current file context
        val currentFile = _state.value.activeFile
        val currentContent = _state.value.fileContents[currentFile]
        if (currentContent != null) {
            messages.add(
                AiService.ChatMessage(
                    role = "system",
                    content = "用户当前正在编辑文件 `$currentFile`，内容如下：\n```\n$currentContent\n```"
                )
            )
        }

        // Add conversation history (last 10 messages)
        val recentMessages = _state.value.aiMessages.takeLast(10)
        for (msg in recentMessages) {
            messages.add(
                AiService.ChatMessage(
                    role = if (msg.role == MessageRole.USER) "user" else "assistant",
                    content = msg.content
                )
            )
        }

        // Add current user message
        messages.add(AiService.ChatMessage(role = "user", content = userPrompt))

        return messages
    }

    fun stopAiStream() {
        aiStreamJob?.cancel()
        val streaming = _state.value.aiStreamingContent
        if (streaming.isNotEmpty()) {
            _state.update { current ->
                current.copy(
                    aiMessages = current.aiMessages + AiMessage(
                        role = MessageRole.AI,
                        content = "$streaming\n\n_(已中断)_"
                    ),
                    isAiThinking = false,
                    aiStreamingContent = ""
                )
            }
        }
    }

    // ==================== Settings ====================

    fun saveAiConfig(config: AiConfig) {
        viewModelScope.launch {
            aiConfigStore.save(config)
            _state.update { it.copy(testResult = null) }
        }
    }

    fun testAiConnection() {
        val config = _state.value.aiConfig
        if (!config.isConfigured) {
            _state.update { it.copy(testResult = "❌ 请先填写 API Key") }
            return
        }

        _state.update { it.copy(testResult = "⏳ 测试中...") }

        viewModelScope.launch {
            val service = AiService(config)
            val result = service.chat(
                listOf(
                    AiService.ChatMessage("user", "Hi, reply with just 'OK'")
                )
            )
            result.fold(
                onSuccess = { response ->
                    _state.update { it.copy(testResult = "✅ 连接成功！模型响应: ${response.take(100)}") }
                },
                onFailure = { e ->
                    _state.update { it.copy(testResult = "❌ 连接失败: ${e.message}") }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiStreamJob?.cancel()
        processExecutor.kill()
    }
}
