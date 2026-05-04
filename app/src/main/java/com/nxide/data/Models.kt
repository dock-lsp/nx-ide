package com.nxide.data

/**
 * 文件树节点
 */
data class FileNode(
    val name: String,
    val type: FileType,
    val language: CodeLanguage = CodeLanguage.TEXT,
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val content: String = ""
)

enum class FileType { FILE, FOLDER }

enum class CodeLanguage(val displayName: String, val extension: String) {
    KOTLIN("Kotlin", ".kt"),
    XML("XML", ".xml"),
    GRADLE("Gradle KTS", ".kts"),
    PROPERTIES("Properties", ".properties"),
    TEXT("Text", "")
}

/**
 * 项目模板
 */
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val icon: String,
    val color: Long,
    val tags: List<String>
)

enum class TemplateCategory(val displayName: String) {
    ALL("全部"),
    BASIC("基础"),
    GOOGLE("Google"),
    ARCHITECTURE("架构"),
    UI_COMPONENTS("UI组件"),
    DATA("数据"),
    SYSTEM("系统")
}

/**
 * 日志条目
 */
data class LogEntry(
    val id: Long = System.currentTimeMillis(),
    val time: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel(val label: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    DEBUG("DEBUG")
}

/**
 * 构建步骤
 */
data class BuildStep(
    val id: Int,
    val name: String,
    val status: BuildStatus = BuildStatus.PENDING,
    val duration: String? = null
)

enum class BuildStatus {
    PENDING, RUNNING, SUCCESS, ERROR
}

/**
 * AI 对话消息
 */
data class AiMessage(
    val id: Long = System.currentTimeMillis(),
    val role: MessageRole,
    val content: String
)

enum class MessageRole { USER, AI }

/**
 * 底部面板类型
 */
enum class BottomPanelType(val label: String, val icon: String) {
    LAYOUT("Layout", "📐"),
    BUILD("Build", "🔨"),
    LOGCAT("Logcat", "📋"),
    FILES("Files", "📁"),
    TERMINAL("Terminal", "💻")
}

/**
 * 主标签页
 */
enum class MainTab(val label: String, val icon: String) {
    PROJECT("项目", "📁"),
    RECENT("最近", "📋"),
    SETTINGS("设置", "⚙️")
}

/**
 * 终端命令
 */
data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false
)
