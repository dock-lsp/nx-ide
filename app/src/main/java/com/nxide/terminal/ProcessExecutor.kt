package com.nxide.terminal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * 真实进程执行器 —— 用于终端命令执行和构建系统
 */
class ProcessExecutor {

    sealed class OutputEvent {
        data class Stdout(val text: String) : OutputEvent()
        data class Stderr(val text: String) : OutputEvent()
        data class Exit(val code: Int) : OutputEvent()
        data class Error(val message: String) : OutputEvent()
    }

    private var currentProcess: Process? = null
    private var currentJob: Job? = null

    private val _events = MutableSharedFlow<OutputEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<OutputEvent> = _events

    @Volatile
    private var isRunning = false

    /**
     * 执行单条命令，输出通过 events Flow 发射
     */
    suspend fun execute(
        command: String,
        workingDir: File? = null,
        env: Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (isRunning) {
            _events.emit(OutputEvent.Error("上一个命令还在执行中"))
            return@withContext
        }

        isRunning = true
        currentJob = coroutineContext[Job]

        try {
            val cmdParts = arrayOf("/system/bin/sh", "-c", command)
            val pb = ProcessBuilder(*cmdParts)

            workingDir?.let { pb.directory(it) }
            pb.environment().putAll(env)
            pb.redirectErrorStream(false)

            val process = pb.start()
            currentProcess = process

            // 并行读取 stdout 和 stderr
            val stdoutJob = launch {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        _events.emit(OutputEvent.Stdout(line!!))
                    }
                }
            }

            val stderrJob = launch {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        _events.emit(OutputEvent.Stderr(line!!))
                    }
                }
            }

            stdoutJob.join()
            stderrJob.join()

            val exitCode = process.waitFor()
            _events.emit(OutputEvent.Exit(exitCode))
        } catch (e: CancellationException) {
            currentProcess?.destroyForcibly()
            _events.emit(OutputEvent.Exit(-1))
        } catch (e: Exception) {
            _events.emit(OutputEvent.Error(e.message ?: "未知错误"))
        } finally {
            currentProcess = null
            currentJob = null
            isRunning = false
        }
    }

    /**
     * 启动交互式 shell 会话
     */
    suspend fun startInteractiveShell(
        workingDir: File? = null
    ) = withContext(Dispatchers.IO) {
        if (isRunning) {
            _events.emit(OutputEvent.Error("已有进程在运行"))
            return@withContext
        }

        isRunning = true
        try {
            val pb = ProcessBuilder("/system/bin/sh", "-i")
            workingDir?.let { pb.directory(it) }
            pb.environment()["TERM"] = "xterm-256color"
            pb.redirectErrorStream(false)

            val process = pb.start()
            currentProcess = process

            val stdoutJob = launch {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        _events.emit(OutputEvent.Stdout(line!!))
                    }
                }
            }

            val stderrJob = launch {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        _events.emit(OutputEvent.Stderr(line!!))
                    }
                }
            }

            stdoutJob.join()
            stderrJob.join()
            val exitCode = process.waitFor()
            _events.emit(OutputEvent.Exit(exitCode))
        } catch (e: CancellationException) {
            currentProcess?.destroyForcibly()
        } catch (e: Exception) {
            _events.emit(OutputEvent.Error(e.message ?: "Shell 启动失败"))
        } finally {
            currentProcess = null
            isRunning = false
        }
    }

    /**
     * 向当前运行的进程写入输入
     */
    suspend fun sendInput(text: String) = withContext(Dispatchers.IO) {
        currentProcess?.let { process ->
            try {
                val writer = OutputStreamWriter(process.outputStream)
                writer.write(text + "\n")
                writer.flush()
            } catch (e: Exception) {
                _events.emit(OutputEvent.Error("写入失败: ${e.message}"))
            }
        }
    }

    /**
     * 终止当前进程
     */
    fun kill() {
        currentJob?.cancel()
        currentProcess?.destroyForcibly()
    }

    fun isProcessRunning() = isRunning
}
