package com.nxide.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nxide.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

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
    val terminalLines: List<TerminalLine> = DefaultProject.terminalLines,
    val sidebarOpen: Boolean = true,
    val aiPrompt: String = "",
    val aiMessages: List<AiMessage> = emptyList(),
    val showAiPanel: Boolean = false,
    val isAiThinking: Boolean = false,
    // New states
    val editorTabs: List<EditorTab> = listOf(
        EditorTab("MainActivity.kt", CodeLanguage.KOTLIN)
    ),
    val searchState: SearchState = SearchState(),
    val searchResults: List<SearchResult> = emptyList(),
    val globalSearchQuery: String = "",
    val globalSearchResults: List<SearchResult> = emptyList(),
    val showGlobalSearch: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val showSettings: Boolean = false,
    val contextMenuVisible: Boolean = false,
    val contextMenuPosition: Pair<Float, Float> = Pair(0f, 0f),
    val terminalHistory: List<String> = emptyList(),
    val terminalHistoryIndex: Int = -1,
)

class NxIdeViewModel : ViewModel() {
    private val _state = MutableStateFlow(NxIdeState())
    val state: StateFlow<NxIdeState> = _state.asStateFlow()

    fun setTab(tab: MainTab) {
        _state.update { it.copy(activeTab = tab) }
    }

    fun toggleBottomPanel(panel: BottomPanelType) {
        _state.update { current ->
            current.copy(
                activeBottomPanel = if (current.activeBottomPanel == panel) null else panel
            )
        }
    }

    fun setActiveFile(fileName: String) {
        _state.update { current ->
            val language = getAllFiles(current.files).find { it.name == fileName }?.language ?: CodeLanguage.TEXT
            val tabs = if (current.editorTabs.any { it.fileName == fileName }) {
                current.editorTabs
            } else {
                current.editorTabs + EditorTab(fileName, language)
            }
            current.copy(
                activeFile = fileName,
                editorTabs = tabs
            )
        }
    }

    fun closeTab(fileName: String) {
        _state.update { current ->
            val newTabs = current.editorTabs.filter { it.fileName != fileName }
            if (newTabs.isEmpty()) return@update current
            val newActive = if (current.activeFile == fileName) {
                newTabs.last().fileName
            } else {
                current.activeFile
            }
            current.copy(
                editorTabs = newTabs,
                activeFile = newActive
            )
        }
    }

    fun setFileContent(fileName: String, content: String) {
        _state.update { current ->
            val updatedTabs = current.editorTabs.map {
                if (it.fileName == fileName) it.copy(isModified = true) else it
            }
            current.copy(
                fileContents = current.fileContents + (fileName to content),
                editorTabs = updatedTabs
            )
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

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
    }

    fun startBuild() {
        val current = _state.value
        if (current.isBuilding) return

        _state.update {
            it.copy(
                isBuilding = true,
                buildSteps = DefaultProject.buildSteps.map { s -> s.copy(status = BuildStatus.PENDING) }
            )
        }

        viewModelScope.launch {
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
                delay(durations[i] + Random.nextInt(200).toLong())
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

            _state.update { it.copy(isBuilding = false) }
            addLog(LogLevel.INFO, "Build", "BUILD SUCCESSFUL - APK installed and running")
        }
    }

    fun resetBuild() {
        _state.update {
            it.copy(
                buildSteps = DefaultProject.buildSteps.map { s -> s.copy(status = BuildStatus.PENDING, duration = null) },
                isBuilding = false
            )
        }
    }

    fun addLog(level: LogLevel, tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _state.update { current ->
            current.copy(logs = current.logs + LogEntry(time = time, level = level, tag = tag, message = message))
        }
    }

    fun clearTerminal() {
        _state.update { it.copy(terminalLines = listOf(TerminalLine("$ ", isCommand = true))) }
    }

    fun executeTerminalCommand(command: String) {
        _state.update { current ->
            val newLines = current.terminalLines + TerminalLine("$ $command", isCommand = true)
            val newHistory = (current.terminalHistory + command).takeLast(100)
            current.copy(terminalLines = newLines, terminalHistory = newHistory, terminalHistoryIndex = -1)
        }

        val cmd = command.trim()
        val parts = cmd.split("\\s+".toRegex())
        val program = parts.firstOrNull()?.lowercase() ?: ""
        val args = parts.drop(1)

        when (program) {
            "clear" -> clearTerminal()
            "cls" -> clearTerminal()
            "ls" -> {
                val flag = args.firstOrNull() ?: ""
                when {
                    flag.startsWith("-") && "a" in flag -> {
                        addTerminalLine(".  ..  .git  .github  .gitignore  app  build.gradle.kts  gradle.properties  settings.gradle.kts")
                    }
                    flag.startsWith("-") && "l" in flag -> {
                        addTerminalLine("drwxr-xr-x  4 user  staff  128 May  4 12:00 app")
                        addTerminalLine("-rw-r--r--  1 user  staff  242 May  4 12:00 build.gradle.kts")
                        addTerminalLine("-rw-r--r--  1 user  staff  168 May  4 12:00 gradle.properties")
                        addTerminalLine("-rw-r--r--  1 user  staff  530 May  4 12:00 settings.gradle.kts")
                    }
                    else -> {
                        addTerminalLine("app  build.gradle.kts  settings.gradle.kts  gradle.properties")
                    }
                }
            }
            "pwd" -> addTerminalLine("/Users/dev/projects/nx-ide")
            "cd" -> {
                val dir = args.firstOrNull() ?: "~"
                when (dir) {
                    ".." -> addTerminalLine("")
                    "~" -> addTerminalLine("")
                    "." -> addTerminalLine("")
                    else -> {
                        if (dir.startsWith("/")) addTerminalLine("")
                        else addTerminalLine("")
                    }
                }
            }
            "mkdir" -> {
                if (args.isEmpty()) addTerminalLine("mkdir: missing operand")
                else addTerminalLine("")
            }
            "touch" -> {
                if (args.isEmpty()) addTerminalLine("touch: missing file operand")
                else addTerminalLine("")
            }
            "cat" -> {
                if (args.isEmpty()) addTerminalLine("cat: missing file operand")
                else {
                    val file = args.first()
                    val content = _state.value.fileContents[file]
                    if (content != null) {
                        content.split("\n").forEach { addTerminalLine(it) }
                    } else {
                        addTerminalLine("cat: $file: No such file or directory")
                    }
                }
            }
            "echo" -> addTerminalLine(args.joinToString(" "))
            "date" -> {
                val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault())
                addTerminalLine(sdf.format(Date()))
            }
            "whoami" -> addTerminalLine("dev")
            "hostname" -> addTerminalLine("nx-ide.local")
            "uname" -> {
                when {
                    args.contains("-a") -> addTerminalLine("Darwin nx-ide.local 23.4.0 Darwin Kernel Version 23.4.0 x86_64")
                    else -> addTerminalLine("Darwin")
                }
            }
            "gradle", "./gradlew" -> {
                val task = args.firstOrNull() ?: "tasks"
                when {
                    task == "tasks" || task == "tasks --all" -> {
                        addTerminalLine("")
                        addTerminalLine("------------------------------------------------------------")
                        addTerminalLine("All tasks runnable from root project 'NX-IDE'")
                        addTerminalLine("------------------------------------------------------------")
                        addTerminalLine("")
                        addTerminalLine("Build tasks")
                        addTerminalLine("-----------")
                        addTerminalLine("assemble - Assembles the outputs of this project.")
                        addTerminalLine("build - Assembles and tests this project.")
                        addTerminalLine("clean - Deletes the build directory.")
                        addTerminalLine("connectedAndroidTest - Installs and runs instrumentation tests.")
                        addTerminalLine("")
                        addTerminalLine("Install tasks")
                        addTerminalLine("-------------")
                        addTerminalLine("installDebug - Installs the Debug build.")
                        addTerminalLine("installRelease - Installs the Release build.")
                        addTerminalLine("uninstallAll - Uninstall all applications.")
                    }
                    task.contains("assembleDebug") || task.contains("build") -> {
                        addTerminalLine("")
                        addTerminalLine("> Task :app:preBuild UP-TO-DATE")
                        addTerminalLine("> Task :app:preDebugBuild UP-TO-DATE")
                        addTerminalLine("> Task :app:generateDebugBuildConfig UP-TO-DATE")
                        addTerminalLine("> Task :app:compileDebugKotlin")
                        addTerminalLine("> Task :app:processDebugResources")
                        addTerminalLine("> Task :app:packageDebug")
                        addTerminalLine("")
                        addTerminalLine("BUILD SUCCESSFUL in 3s", isCommand = true)
                        addTerminalLine("8 actionable tasks: 4 executed, 4 up-to-date")
                    }
                    task.contains("clean") -> {
                        addTerminalLine("")
                        addTerminalLine("> Task :clean UP-TO-DATE")
                        addTerminalLine("")
                        addTerminalLine("BUILD SUCCESSFUL in 0s", isCommand = true)
                    }
                    else -> addTerminalLine("Unknown task: $task")
                }
            }
            "adb" -> {
                val subcmd = args.firstOrNull() ?: ""
                when (subcmd) {
                    "devices" -> {
                        addTerminalLine("List of devices attached")
                        addTerminalLine("emulator-5554   device product:sdk_gphone64_x86_64 model:sdk_gphone64_x86_64 transport_id:1")
                    }
                    "install" -> {
                        addTerminalLine("Performing Streamed Install")
                        addTerminalLine("Success")
                    }
                    "shell" -> {
                        val shellCmd = args.drop(1).joinToString(" ")
                        when {
                            shellCmd.startsWith("am start") -> addTerminalLine("Starting: Intent { cmp=com.nxide/.MainActivity }")
                            shellCmd.startsWith("pm list") -> addTerminalLine("package:com.nxide")
                            shellCmd == "getprop ro.build.version.sdk" -> addTerminalLine("34")
                            else -> addTerminalLine("shell: $shellCmd")
                        }
                    }
                    "version" -> {
                        addTerminalLine("Android Debug Bridge version 1.0.41")
                        addTerminalLine("Installed as /Users/dev/Library/Android/sdk/platform-tools/adb")
                    }
                    else -> addTerminalLine("Android Debug Bridge version 1.0.41")
                }
            }
            "git" -> {
                val subcmd = args.firstOrNull() ?: ""
                when (subcmd) {
                    "status" -> {
                        addTerminalLine("On branch main")
                        addTerminalLine("Your branch is up to date with 'origin/main'.")
                        addTerminalLine("nothing to commit, working tree clean")
                    }
                    "log" -> {
                        addTerminalLine("commit a1b2c3d (HEAD -> main, origin/main)")
                        addTerminalLine("Author: dev <dev@nxide.com>")
                        addTerminalLine("Date:   Mon May 4 12:00:00 2026 +0800")
                        addTerminalLine("")
                        addTerminalLine("    Initial commit: NX IDE project")
                    }
                    "branch" -> {
                        addTerminalLine("* main")
                        addTerminalLine("  develop")
                        addTerminalLine("  feature/ai-assistant")
                    }
                    "diff" -> addTerminalLine("")
                    "add" -> addTerminalLine("")
                    "commit" -> {
                        val msg = args.drop(1).joinToString(" ")
                        addTerminalLine("[main a1b2c3d] $msg")
                        addTerminalLine(" 1 file changed, 0 insertions(+), 0 deletions(-)")
                    }
                    "push" -> {
                        addTerminalLine("Enumerating objects: 5, done.")
                        addTerminalLine("Counting objects: 100% (5/5), done.")
                        addTerminalLine("Writing objects: 100% (3/3), 256 bytes | 256.00 KiB/s, done.")
                        addTerminalLine("Total 3 (delta 2), reused 0 (delta 0)")
                        addTerminalLine("To github.com:user/nx-ide.git")
                        addTerminalLine("   b2c3d4e..a1b2c3d  main -> main")
                    }
                    "pull" -> {
                        addTerminalLine("Already up to date.")
                    }
                    else -> {
                        addTerminalLine("On branch main")
                        addTerminalLine("nothing to commit, working tree clean")
                    }
                }
            }
            "grep" -> {
                if (args.size < 2) {
                    addTerminalLine("Usage: grep <pattern> <file>")
                } else {
                    val pattern = args[0]
                    val file = args[1]
                    val content = _state.value.fileContents[file]
                    if (content != null) {
                        content.split("\n").forEachIndexed { index, line ->
                            if (line.contains(pattern, ignoreCase = true)) {
                                addTerminalLine("${index + 1}: $line")
                            }
                        }
                    } else {
                        addTerminalLine("grep: $file: No such file or directory")
                    }
                }
            }
            "find" -> {
                if (args.isEmpty()) {
                    addTerminalLine("Usage: find <path> -name <pattern>")
                } else {
                    val nameIdx = args.indexOf("-name")
                    if (nameIdx != -1 && nameIdx + 1 < args.size) {
                        val pattern = args[nameIdx + 1].removeSurrounding("*")
                        getAllFiles(_state.value.files).forEach { file ->
                            if (file.name.contains(pattern, ignoreCase = true)) {
                                addTerminalLine("./app/src/main/java/com/nxide/${file.name}")
                            }
                        }
                    } else {
                        addTerminalLine("./app")
                        addTerminalLine("./build.gradle.kts")
                        addTerminalLine("./settings.gradle.kts")
                    }
                }
            }
            "wc" -> {
                if (args.isEmpty()) {
                    addTerminalLine("wc: missing file operand")
                } else {
                    val file = args.first()
                    val content = _state.value.fileContents[file]
                    if (content != null) {
                        val lines = content.lines().size
                        val words = content.split("\\s+".toRegex()).size
                        val chars = content.length
                        addTerminalLine("  $lines  $words  $chars  $file")
                    } else {
                        addTerminalLine("wc: $file: No such file or directory")
                    }
                }
            }
            "head" -> {
                val n = if (args.contains("-n") && args.indexOf("-n") + 1 < args.size) {
                    args[args.indexOf("-n") + 1].toIntOrNull() ?: 10
                } else 10
                val file = args.last()
                val content = _state.value.fileContents[file]
                if (content != null) {
                    content.lines().take(n).forEach { addTerminalLine(it) }
                } else {
                    addTerminalLine("head: $file: No such file or directory")
                }
            }
            "tail" -> {
                val n = if (args.contains("-n") && args.indexOf("-n") + 1 < args.size) {
                    args[args.indexOf("-n") + 1].toIntOrNull() ?: 10
                } else 10
                val file = args.last()
                val content = _state.value.fileContents[file]
                if (content != null) {
                    content.lines().takeLast(n).forEach { addTerminalLine(it) }
                } else {
                    addTerminalLine("tail: $file: No such file or directory")
                }
            }
            "history" -> {
                _state.value.terminalHistory.forEachIndexed { index, cmd ->
                    addTerminalLine("  ${index + 1}  $cmd")
                }
            }
            "help" -> {
                addTerminalLine("NX IDE Terminal - Available commands:")
                addTerminalLine("")
                addTerminalLine("  File System:   ls, cd, pwd, mkdir, touch, cat, head, tail, wc, find, grep")
                addTerminalLine("  Build:         gradle, ./gradlew")
                addTerminalLine("  Android:       adb (devices, install, shell)")
                addTerminalLine("  Version Ctrl:  git (status, log, branch, diff, commit, push, pull)")
                addTerminalLine("  System:        echo, date, whoami, hostname, uname, history, clear, help")
            }
            "" -> {}
            else -> addTerminalLine("bash: ${command.trim()}: command not found. Type 'help' for available commands.")
        }
    }

    private fun addTerminalLine(text: String, isCommand: Boolean = false) {
        _state.update { current ->
            current.copy(terminalLines = current.terminalLines + TerminalLine(text, isCommand))
        }
    }

    fun setAiPrompt(prompt: String) {
        _state.update { it.copy(aiPrompt = prompt) }
    }

    fun toggleAiPanel() {
        _state.update { it.copy(showAiPanel = !it.showAiPanel) }
    }

    fun sendAiMessage() {
        val prompt = _state.value.aiPrompt.trim()
        if (prompt.isEmpty()) return

        _state.update { current ->
            current.copy(
                aiMessages = current.aiMessages + AiMessage(role = MessageRole.USER, content = prompt),
                aiPrompt = "",
                isAiThinking = true
            )
        }

        viewModelScope.launch {
            delay(1000L + Random.nextInt(1500))

            val response = generateAiResponse(prompt)
            _state.update { current ->
                current.copy(
                    aiMessages = current.aiMessages + AiMessage(role = MessageRole.AI, content = response),
                    isAiThinking = false
                )
            }
        }
    }

    private fun generateAiResponse(prompt: String): String {
        return when {
            "导航" in prompt || "底部" in prompt -> """我来帮你添加底部导航栏。首先在 Scaffold 的 bottomBar 参数中添加 NavigationBar 组件：

```kotlin
NavigationBar {
    items.forEachIndexed { index, item ->
        NavigationBarItem(
            icon = { Icon(item.icon, null) },
            label = { Text(item.title) },
            selected = selectedItem == index,
            onClick = { selectedItem = index }
        )
    }
}
```

需要我进一步完善导航逻辑吗？"""

            "主题" in prompt || "配色" in prompt -> """建议使用 Material3 的动态配色方案：

```kotlin
val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (darkTheme) dynamicDarkColorScheme(context)
    else dynamicLightColorScheme(context)
} else {
    if (darkTheme) darkColorScheme(primary = Green40)
    else lightColorScheme(primary = Green80)
}
```

这样可以在 Android 12+ 设备上自动适配系统主题色。"""

            "数据库" in prompt || "Room" in prompt -> """添加 Room 数据库需要以下步骤：

1. **添加依赖** - 在 build.gradle.kts 中添加 Room 相关依赖
2. **创建 Entity** - 定义数据表结构
3. **创建 DAO** - 定义数据库操作接口
4. **创建 Database** - 数据库实例

需要我生成完整的数据库代码吗？"""

            "网络" in prompt || "Retrofit" in prompt || "API" in prompt -> """推荐使用 Retrofit + OkHttp 进行网络请求：

```kotlin
// 1. 定义 API 接口
interface ApiService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): User
}

// 2. 创建 Retrofit 实例
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val api = retrofit.create(ApiService::class.java)
```

需要添加拦截器、缓存或错误处理吗？"""

            "列表" in prompt || "RecyclerView" in prompt || "LazyColumn" in prompt -> """在 Compose 中推荐使用 LazyColumn 替代 RecyclerView：

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(items) { item ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(item.title, modifier = Modifier.padding(16.dp))
        }
    }
}
```

需要添加下拉刷新或分页加载吗？"""

            "测试" in prompt -> """我可以为你生成以下测试：

- **单元测试**: ViewModel 逻辑测试
- **UI 测试**: Compose 界面测试
- **集成测试**: 数据库操作测试

请选择需要的测试类型。"""

            "代码" in prompt || "写" in prompt || "生成" in prompt -> """好的，我来为你生成代码。基于当前项目结构：

```kotlin
@Composable
fun NewFeature() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "新功能",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 在这里添加更多组件
    }
}
```

你可以将这段代码添加到项目中。需要我进一步完善吗？"""

            "优化" in prompt || "重构" in prompt -> """我建议采用以下优化方案：

1. **MVVM 架构** - 使用 ViewModel + StateFlow 管理状态
2. **依赖注入** - 使用 Hilt 管理依赖关系
3. **Repository 模式** - 统一数据访问层
4. **UseCase 模式** - 封装业务逻辑，保持 ViewModel 简洁

需要我详细展开某个方面吗？"""

            "动画" in prompt || "animate" in prompt -> """Compose 提供了强大的动画 API：

```kotlin
// 1. animateXxxAsState - 简单动画
val color by animateColorAsState(
    targetValue = if (isSelected) Green40 else Gray40,
    animationSpec = tween(durationMillis = 300)
)

// 2. Animatable - 细粒度控制
val alpha = remember { Animatable(0f) }
LaunchedEffect(Unit) {
    alpha.animateTo(1f, animationSpec = tween(500))
}

// 3. updateTransition - 多属性动画
val transition = updateTransition(targetState = isSelected, label = "selection")
val scale by transition.animateFloat { if (it) 1.1f else 1f }
```

需要哪种动画效果？"""

            else -> """好的，我来帮你处理这个问题。

根据你的项目结构，我建议：
1. 先分析当前代码的架构和需求
2. 确定需要修改或新增的部分
3. 逐步实现改动并测试

请告诉我更具体的需求，我可以提供更有针对性的帮助。比如：
- 🎨 UI 组件开发
- 📦 数据层设计
- 🔧 架构优化
- 🧪 测试编写"""
        }
    }

    fun toggleSidebar() {
        _state.update { it.copy(sidebarOpen = !it.sidebarOpen) }
    }

    // Search & Replace
    fun setSearchQuery(query: String) {
        _state.update { current ->
            val content = current.fileContents[current.activeFile] ?: ""
            val count = countOccurrences(content, query, current.searchState.isCaseSensitive)
            current.copy(
                searchState = current.searchState.copy(
                    query = query,
                    matchCount = count,
                    currentMatch = if (count > 0) 1 else 0
                )
            )
        }
    }

    fun setReplaceText(text: String) {
        _state.update { it.copy(searchState = it.searchState.copy(replaceText = text)) }
    }

    fun toggleSearchCaseSensitive() {
        _state.update { current ->
            val newCaseSensitive = !current.searchState.isCaseSensitive
            val content = current.fileContents[current.activeFile] ?: ""
            val count = countOccurrences(content, current.searchState.query, newCaseSensitive)
            current.copy(
                searchState = current.searchState.copy(
                    isCaseSensitive = newCaseSensitive,
                    matchCount = count,
                    currentMatch = if (count > 0) 1 else 0
                )
            )
        }
    }

    fun toggleSearchRegex() {
        _state.update { it.copy(searchState = it.searchState.copy(useRegex = !it.searchState.useRegex)) }
    }

    fun toggleSearchBar() {
        _state.update { current ->
            val newVisible = !current.searchState.isVisible
            current.copy(
                searchState = current.searchState.copy(
                    isVisible = newVisible,
                    query = if (newVisible) current.searchState.query else "",
                    matchCount = 0,
                    currentMatch = 0
                )
            )
        }
    }

    fun nextSearchResult() {
        _state.update { current ->
            if (current.searchState.matchCount == 0) return@update current
            val next = (current.searchState.currentMatch % current.searchState.matchCount) + 1
            current.copy(searchState = current.searchState.copy(currentMatch = next))
        }
    }

    fun prevSearchResult() {
        _state.update { current ->
            if (current.searchState.matchCount == 0) return@update current
            val prev = if (current.searchState.currentMatch <= 1) current.searchState.matchCount
            else current.searchState.currentMatch - 1
            current.copy(searchState = current.searchState.copy(currentMatch = prev))
        }
    }

    fun replaceCurrent() {
        _state.update { current ->
            val content = current.fileContents[current.activeFile] ?: return@update current
            val query = current.searchState.query
            if (query.isEmpty()) return@update current
            val idx = findNthOccurrence(content, query, current.searchState.currentMatch - 1, current.searchState.isCaseSensitive)
            if (idx >= 0) {
                val newContent = content.substring(0, idx) + current.searchState.replaceText +
                    content.substring(idx + query.length)
                val count = countOccurrences(newContent, query, current.searchState.isCaseSensitive)
                current.copy(
                    fileContents = current.fileContents + (current.activeFile to newContent),
                    searchState = current.searchState.copy(
                        matchCount = count,
                        currentMatch = if (count > 0) minOf(current.searchState.currentMatch, count) else 0
                    ),
                    editorTabs = current.editorTabs.map {
                        if (it.fileName == current.activeFile) it.copy(isModified = true) else it
                    }
                )
            } else current
        }
    }

    fun replaceAll() {
        _state.update { current ->
            val content = current.fileContents[current.activeFile] ?: return@update current
            val query = current.searchState.query
            if (query.isEmpty()) return@update current
            val newContent = if (current.searchState.isCaseSensitive) {
                content.replace(query, current.searchState.replaceText)
            } else {
                content.replace(Regex(Regex.escape(query), RegexOption.IGNORE_CASE), current.searchState.replaceText)
            }
            current.copy(
                fileContents = current.fileContents + (current.activeFile to newContent),
                searchState = current.searchState.copy(matchCount = 0, currentMatch = 0),
                editorTabs = current.editorTabs.map {
                    if (it.fileName == current.activeFile) it.copy(isModified = true) else it
                }
            )
        }
    }

    private fun countOccurrences(text: String, query: String, caseSensitive: Boolean): Int {
        if (query.isEmpty()) return 0
        return if (caseSensitive) {
            text.windowed(query.length, 1) { if (it == query) 1 else 0 }.sum()
        } else {
            text.lowercase().windowed(query.lowercase().length, 1) { if (it == query.lowercase()) 1 else 0 }.sum()
        }
    }

    private fun findNthOccurrence(text: String, query: String, n: Int, caseSensitive: Boolean): Int {
        if (query.isEmpty()) return -1
        var count = 0
        var start = 0
        val searchText = if (caseSensitive) text else text.lowercase()
        val searchQuery = if (caseSensitive) query else query.lowercase()
        while (start <= searchText.length - searchQuery.length) {
            val idx = searchText.indexOf(searchQuery, start)
            if (idx == -1) return -1
            if (count == n) return idx
            count++
            start = idx + 1
        }
        return -1
    }

    // Global search
    fun setGlobalSearchQuery(query: String) {
        _state.update { it.copy(globalSearchQuery = query) }
    }

    fun performGlobalSearch() {
        _state.update { current ->
            val query = current.globalSearchQuery
            if (query.isEmpty()) return@update current.copy(globalSearchResults = emptyList())
            val results = mutableListOf<SearchResult>()
            current.fileContents.forEach { (fileName, content) ->
                content.lines().forEachIndexed { lineIndex, line ->
                    var start = 0
                    while (true) {
                        val idx = line.indexOf(query, start, ignoreCase = true)
                        if (idx == -1) break
                        results.add(SearchResult(fileName, lineIndex + 1, line.trim(), idx, idx + query.length))
                        start = idx + 1
                    }
                }
            }
            current.copy(globalSearchResults = results)
        }
    }

    fun toggleGlobalSearch() {
        _state.update { it.copy(showGlobalSearch = !it.showGlobalSearch) }
    }

    // Settings
    fun toggleSettings() {
        _state.update { it.copy(showSettings = !it.showSettings) }
    }

    fun updateSettings(settings: AppSettings) {
        _state.update { it.copy(settings = settings) }
    }

    fun updateFontSize(size: Int) {
        _state.update { it.copy(settings = it.settings.copy(fontSize = size)) }
    }

    fun updateTabSize(size: Int) {
        _state.update { it.copy(settings = it.settings.copy(tabSize = size)) }
    }

    fun toggleWordWrap() {
        _state.update { it.copy(settings = it.settings.copy(wordWrap = !it.settings.wordWrap)) }
    }

    fun toggleLineNumbers() {
        _state.update { it.copy(settings = it.settings.copy(showLineNumbers = !it.settings.showLineNumbers)) }
    }

    fun toggleAutoIndent() {
        _state.update { it.copy(settings = it.settings.copy(autoIndent = !it.settings.autoIndent)) }
    }

    // File operations
    fun createNewFile(parentFolder: String, fileName: String) {
        _state.update { current ->
            val language = when {
                fileName.endsWith(".kt") -> CodeLanguage.KOTLIN
                fileName.endsWith(".xml") -> CodeLanguage.XML
                fileName.endsWith(".kts") -> CodeLanguage.GRADLE
                fileName.endsWith(".properties") -> CodeLanguage.PROPERTIES
                else -> CodeLanguage.TEXT
            }
            val newNode = FileNode(fileName, FileType.FILE, language, content = "")
            val updatedFiles = addFileToFolder(current.files, parentFolder, newNode)
            current.copy(
                files = updatedFiles,
                fileContents = current.fileContents + (fileName to "// $fileName\n")
            )
        }
    }

    fun createNewFolder(parentFolder: String, folderName: String) {
        _state.update { current ->
            val newNode = FileNode(folderName, FileType.FOLDER)
            val updatedFiles = addFileToFolder(current.files, parentFolder, newNode)
            current.copy(files = updatedFiles)
        }
    }

    fun deleteFile(fileName: String) {
        _state.update { current ->
            val updatedFiles = removeFileFromTree(current.files, fileName)
            val updatedContents = current.fileContents - fileName
            val updatedTabs = current.editorTabs.filter { it.fileName != fileName }
            val newActive = if (current.activeFile == fileName) {
                updatedTabs.lastOrNull()?.fileName ?: ""
            } else current.activeFile
            current.copy(
                files = updatedFiles,
                fileContents = updatedContents,
                editorTabs = updatedTabs,
                activeFile = newActive
            )
        }
    }

    fun renameFile(oldName: String, newName: String) {
        _state.update { current ->
            val updatedFiles = renameFileInTree(current.files, oldName, newName)
            val content = current.fileContents[oldName] ?: ""
            val updatedContents = current.fileContents - oldName + (newName to content)
            val language = when {
                newName.endsWith(".kt") -> CodeLanguage.KOTLIN
                newName.endsWith(".xml") -> CodeLanguage.XML
                newName.endsWith(".kts") -> CodeLanguage.GRADLE
                newName.endsWith(".properties") -> CodeLanguage.PROPERTIES
                else -> CodeLanguage.TEXT
            }
            val updatedTabs = current.editorTabs.map {
                if (it.fileName == oldName) EditorTab(newName, language, it.isModified) else it
            }
            val newActive = if (current.activeFile == oldName) newName else current.activeFile
            current.copy(
                files = updatedFiles,
                fileContents = updatedContents,
                editorTabs = updatedTabs,
                activeFile = newActive
            )
        }
    }

    private fun addFileToFolder(nodes: List<FileNode>, folderName: String, newFile: FileNode): List<FileNode> {
        return nodes.map { node ->
            if (node.name == folderName && node.type == FileType.FOLDER) {
                node.copy(children = node.children + newFile, isExpanded = true)
            } else if (node.children.isNotEmpty()) {
                node.copy(children = addFileToFolder(node.children, folderName, newFile))
            } else {
                node
            }
        }
    }

    private fun removeFileFromTree(nodes: List<FileNode>, fileName: String): List<FileNode> {
        return nodes.filter { it.name != fileName }.map { node ->
            if (node.children.isNotEmpty()) {
                node.copy(children = removeFileFromTree(node.children, fileName))
            } else node
        }
    }

    private fun renameFileInTree(nodes: List<FileNode>, oldName: String, newName: String): List<FileNode> {
        return nodes.map { node ->
            when {
                node.name == oldName -> node.copy(name = newName)
                node.children.isNotEmpty() -> node.copy(children = renameFileInTree(node.children, oldName, newName))
                else -> node
            }
        }
    }

    private fun getAllFiles(node: FileNode): List<FileNode> {
        val result = mutableListOf<FileNode>()
        if (node.type == FileType.FILE) result.add(node)
        node.children.forEach { result.addAll(getAllFiles(it)) }
        return result
    }

    private fun getAllFiles(nodes: List<FileNode>): List<FileNode> {
        val result = mutableListOf<FileNode>()
        nodes.forEach { result.addAll(getAllFiles(it)) }
        return result
    }
}
