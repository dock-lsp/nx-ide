package com.nxide.terminal

import com.nxide.data.BuildStatus
import com.nxide.data.BuildStep
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * 真实构建执行器 —— 执行 Gradle 构建并解析输出
 */
class BuildExecutor(private val processExecutor: ProcessExecutor) {

    data class BuildState(
        val isBuilding: Boolean = false,
        val steps: List<BuildStep> = emptyList(),
        val logs: List<String> = emptyList(),
        val summary: String = "点击 ▶ 运行 开始构建",
        val success: Boolean? = null
    )

    private val _state = MutableStateFlow(BuildState())
    val state: StateFlow<BuildState> = _state.asStateFlow()

    private var buildJob: Job? = null

    /**
     * 执行 Gradle 构建
     * @param projectPath 项目根目录（需包含 gradlew）
     * @param tasks Gradle 任务列表，如 ["assembleDebug"]
     */
    fun startBuild(
        projectPath: String,
        tasks: List<String> = listOf("assembleDebug")
    ) {
        if (_state.value.isBuilding) return

        buildJob?.cancel()
        buildJob = CoroutineScope(Dispatchers.Main).launch {
            _state.update {
                BuildState(
                    isBuilding = true,
                    summary = "⏳ 正在构建...",
                    logs = emptyList()
                )
            }

            val gradlew = File(projectPath, "gradlew")
            val command = if (gradlew.exists() && gradlew.canExecute()) {
                "${gradlew.absolutePath} ${tasks.joinToString(" ")} --no-daemon --console=plain"
            } else {
                // 尝试系统 gradle
                "gradle ${tasks.joinToString(" ")} --no-daemon --console=plain"
            }

            val buildLogs = mutableListOf<String>()
            val detectedSteps = mutableListOf<BuildStep>()
            var stepCounter = 0
            var buildSuccess: Boolean? = null
            var startTime = System.currentTimeMillis()

            // 收集构建输出
            val collectJob = launch {
                processExecutor.events.collect { event ->
                    when (event) {
                        is ProcessExecutor.OutputEvent.Stdout -> {
                            buildLogs.add(event.text)
                            _state.update { it.copy(logs = buildLogs.toList()) }

                            // 解析 Gradle 任务输出
                            val taskMatch = TASK_REGEX.find(event.text)
                            if (taskMatch != null) {
                                val taskName = taskMatch.groupValues[1]
                                val status = taskMatch.groupValues[2]
                                stepCounter++
                                val step = BuildStep(
                                    id = stepCounter,
                                    name = taskName,
                                    status = when {
                                        status.contains("UP-TO-DATE") || status.contains("NO-SOURCE") -> BuildStatus.SUCCESS
                                        status.contains("FROM-CACHE") -> BuildStatus.SUCCESS
                                        else -> BuildStatus.SUCCESS
                                    },
                                    duration = null
                                )
                                detectedSteps.add(step)
                                _state.update { it.copy(steps = detectedSteps.toList()) }
                            }

                            // 检测构建结果
                            if (event.text.contains("BUILD SUCCESSFUL")) {
                                buildSuccess = true
                                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                                _state.update {
                                    it.copy(
                                        isBuilding = false,
                                        success = true,
                                        summary = "✅ BUILD SUCCESSFUL - Total: ${String.format("%.1fs", elapsed)}"
                                    )
                                }
                            } else if (event.text.contains("BUILD FAILED")) {
                                buildSuccess = false
                                _state.update {
                                    it.copy(
                                        isBuilding = false,
                                        success = false,
                                        summary = "❌ BUILD FAILED"
                                    )
                                }
                            }
                        }
                        is ProcessExecutor.OutputEvent.Stderr -> {
                            buildLogs.add("[ERR] ${event.text}")
                            _state.update { it.copy(logs = buildLogs.toList()) }
                        }
                        is ProcessExecutor.OutputEvent.Exit -> {
                            if (buildSuccess == null) {
                                buildSuccess = event.code == 0
                                _state.update {
                                    it.copy(
                                        isBuilding = false,
                                        success = buildSuccess,
                                        summary = if (buildSuccess == true) "✅ BUILD SUCCESSFUL" else "❌ BUILD FAILED (exit code: ${event.code})"
                                    )
                                }
                            }
                        }
                        is ProcessExecutor.OutputEvent.Error -> {
                            buildLogs.add("[ERROR] ${event.message}")
                            _state.update {
                                it.copy(
                                    logs = buildLogs.toList(),
                                    isBuilding = false,
                                    success = false,
                                    summary = "❌ 构建错误: ${event.message}"
                                )
                            }
                        }
                    }
                }
            }

            // 执行构建
            processExecutor.execute(projectPath.let { "cd $it && $command" })

            collectJob.cancel()
        }
    }

    /**
     * 停止当前构建
     */
    fun stopBuild() {
        buildJob?.cancel()
        processExecutor.kill()
        _state.update {
            it.copy(
                isBuilding = false,
                summary = "⏹ 构建已停止"
            )
        }
    }

    /**
     * 重置构建状态
     */
    fun reset() {
        buildJob?.cancel()
        _state.update { BuildState() }
    }

    companion object {
        // 匹配 Gradle 任务输出: > Task :app:compileDebugKotlin UP-TO-DATE
        private val TASK_REGEX = Regex("""> Task (\S+)\s+(.*)""")
    }
}
