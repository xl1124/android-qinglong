package me.doujiang.app.ui.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.CronTask
import me.doujiang.app.data.model.LogFileInfo
import me.doujiang.app.data.model.TaskRequest
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TasksUiState(
    val tasks: List<CronTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: CronTask? = null,
    // 日志查看器状态
    val logTaskId: Int? = null,
    val logTaskName: String = "",
    val logContent: String = "",
    val logFiles: List<LogFileInfo> = emptyList(),
    val selectedLogIndex: Int = -1,       // -1=使用最新日志
    val logStatus: String? = null,
    val isLogLoading: Boolean = false,
    val logError: String? = null,
    val statusFilter: String = "all"  // "all", "enabled", "disabled"
)

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    private var logPollingJob: Job? = null

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getTasks().onSuccess { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _uiState.value = _uiState.value.copy(selectedIds = current, isSelectionMode = current.isNotEmpty())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet(), isSelectionMode = false)
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }

    fun showEditDialog(task: CronTask) { _uiState.value = _uiState.value.copy(showEditDialog = task) }
    fun hideEditDialog() { _uiState.value = _uiState.value.copy(showEditDialog = null) }

    fun uploadScript(fileName: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.uploadScript(fileName = fileName, content = content).onSuccess {
                onResult(true, fileName)
            }.onFailure { e ->
                onResult(false, e.message ?: "上传失败")
            }
        }
    }

    fun addTask(name: String, command: String, schedule: String) {
        viewModelScope.launch {
            repository.addTask(TaskRequest(name, command, schedule)).onSuccess {
                hideAddDialog(); loadTasks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "添加任务失败")
            }
        }
    }

    fun updateTask(id: String, name: String, command: String, schedule: String) {
        viewModelScope.launch {
            repository.updateTask(TaskRequest(name, command, schedule, _id = id)).onSuccess {
                hideEditDialog(); loadTasks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "更新任务失败")
            }
        }
    }

    fun deleteSelected() = batchOp { repository.deleteTasks(it) }
    fun runSelected() = batchOp { repository.runTasks(it) }
    fun stopSelected() = batchOp { repository.stopTasks(it) }
    fun enableSelected() = batchOp { repository.enableTasks(it) }
    fun disableSelected() = batchOp { repository.disableTasks(it) }
    fun pinSelected() = batchOp { repository.pinTasks(it) }
    fun unpinSelected() = batchOp { repository.unpinTasks(it) }

    fun deleteTask(id: String) { singleOp(id, "删除") { repository.deleteTasks(it) } }
    fun runTask(id: String) { singleOp(id, "运行") { repository.runTasks(it) } }
    fun stopTask(id: String) { singleOp(id, "停止") { repository.stopTasks(it) } }
    fun enableTask(id: String) { singleOp(id, "启用") { repository.enableTasks(it) } }
    fun disableTask(id: String) { singleOp(id, "禁用") { repository.disableTasks(it) } }
    fun pinTask(id: String) { singleOp(id, "置顶") { repository.pinTasks(it) } }
    fun unpinTask(id: String) { singleOp(id, "取消置顶") { repository.unpinTasks(it) } }

    fun setStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }

    val filteredTasks: List<CronTask>
        get() {
            val state = _uiState.value
            return when (state.statusFilter) {
                "enabled" -> state.tasks.filter { it.isDisabled == 0 }
                "disabled" -> state.tasks.filter { it.isDisabled == 1 }
                else -> state.tasks
            }
        }

    /**
     * 运行任务并打开实时日志查看器
     */
    fun runTaskAndShowLog(id: String, taskName: String) {
        val numericId = id.toIntOrNull()
        if (numericId == null) {
            _uiState.value = _uiState.value.copy(
                error = "运行失败: 无效的任务ID($id)，不是数字"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            logTaskId = numericId,
            logTaskName = taskName,
            logContent = "",
            logFiles = emptyList(),
            selectedLogIndex = -1,
            logStatus = null,
            isLogLoading = true,
            logError = null
        )
        viewModelScope.launch {
            repository.runTasks(listOf(id)).onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    logError = "启动失败: ${e.message ?: "未知错误"}"
                )
            }
            // 刷新任务列表，使状态从「空闲」更新为「运行中」
            loadTasks()
            // 日志查看器打开期间，轮询日志内容和任务状态
            logPollingJob?.cancel()
            logPollingJob = viewModelScope.launch {
                while (_uiState.value.logTaskId != null) {
                    fetchLatestLog()
                    delay(1500)
                    loadTasks()
                    delay(1500)
                }
            }
        }
    }

    /**
     * 计算日志目录名（匹配 Q1 的 getUniqPath 算法）
     * 例如: command="task jd_script.js", id=42 → "jd_script_42"
     */
    private fun computeUniqPath(command: String, id: Int): String {
        val suffix = "_$id"
        val items = command.split(" +".toRegex())
        var str = items[0]
        // 如果第一个命令是 "task"，取第二个参数作为脚本名
        if (items.size > 1 && items[0] == "task") {
            str = items[1]
        }
        // 去掉扩展名
        val dotIndex = str.lastIndexOf('.')
        if (dotIndex != -1) {
            str = str.substring(0, dotIndex)
        }
        // 处理子目录
        val slashIndex = str.lastIndexOf('/')
        if (slashIndex != -1) {
            val parentPath = str.substring(0, slashIndex)
            val lastSlash = parentPath.lastIndexOf('/')
            val dirName = if (lastSlash != -1) parentPath.substring(lastSlash + 1) else parentPath
            str = "${dirName}_${str.substring(slashIndex + 1)}"
        }
        return "$str$suffix"
    }

    /**
     * 查看历史日志：获取最新日志 + 从日志树中找出该任务的日志文件
     */
    fun showTaskLog(id: String, taskName: String, command: String = "") {
        val numericId = id.toIntOrNull()
        if (numericId == null) {
            _uiState.value = _uiState.value.copy(
                error = "查看日志失败: 无效的任务ID"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            logTaskId = numericId,
            logTaskName = taskName,
            logContent = "",
            logFiles = emptyList(),
            selectedLogIndex = -1,
            logStatus = null,
            isLogLoading = true,
            logError = null
        )
        val uniqPath = computeUniqPath(command, numericId)

        viewModelScope.launch {
            var latestContent: String? = null
            var latestStatus: String? = null
            var errMsg: String? = null

            // 1. 获取最新日志
            repository.getCronLog(numericId).onSuccess { result ->
                latestContent = result.content
                latestStatus = result.logStatus
            }.onFailure { e ->
                errMsg = "获取最新日志失败: ${e.message}"
            }

            // 2. 获取日志文件列表
            var logFiles = emptyList<LogFileInfo>()
            var treeErr: String? = null

            // 2a. 优先尝试 GET /api/crons/{id}/logs (Q1 新版本接口)
            repository.getCronLogs(numericId).onSuccess { files ->
                logFiles = files
            }.onFailure { e ->
                treeErr = "crons/logs: ${e.message}"
            }

            // 2b. 如果 crons/logs 失败, 尝试 GET /api/logs (Q2 方式)
            if (logFiles.isEmpty()) {
                repository.getLogTree().onSuccess { tree ->
                    val dirNode = tree.find { it.title == uniqPath && it.isDirectory }
                    if (dirNode != null && dirNode.children != null) {
                        logFiles = dirNode.children
                            .filter { !it.isDirectory }
                            .sortedByDescending { it.createTime }
                            .map {
                                LogFileInfo(
                                    filename = it.title,
                                    directory = uniqPath,
                                    time = it.createTime.toDouble()
                                )
                            }
                    }
                    if (logFiles.isEmpty()) {
                        treeErr = (treeErr ?: "") + "; 树存在但未找到目录[$uniqPath]或目录下无文件"
                    }
                }.onFailure { e ->
                    treeErr = (treeErr ?: "") + "; logs: ${e.message}"
                }
            }

            _uiState.value = _uiState.value.copy(
                logContent = latestContent ?: "",
                logStatus = latestStatus,
                logFiles = logFiles,
                isLogLoading = false,
                logError = when {
                    latestContent == null && logFiles.isEmpty() ->
                        errMsg ?: "未能获取到日志(${treeErr ?: "未知"})"
                    logFiles.isEmpty() && treeErr != null ->
                        "日志列表获取失败($treeErr)，但已显示最新日志"
                    else -> null
                }
            )
        }
    }

    /**
     * 切换到指定的日志文件
     */
    fun selectLogFile(index: Int, directory: String, filename: String) {
        _uiState.value = _uiState.value.copy(
            selectedLogIndex = index,
            logContent = "",
            logStatus = null,
            isLogLoading = true,
            logError = null
        )
        viewModelScope.launch {
            repository.getLogFileDetail(directory, filename).onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    logContent = result.content,
                    logStatus = result.logStatus,
                    isLogLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLogLoading = false,
                    logError = "获取日志内容失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 回到最新日志（清除文件选择）
     */
    fun showLatestLog() {
        val taskId = _uiState.value.logTaskId ?: return
        _uiState.value = _uiState.value.copy(
            selectedLogIndex = -1,
            logContent = "",
            logStatus = null,
            isLogLoading = true,
            logError = null
        )
        viewModelScope.launch {
            repository.getCronLog(taskId).onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    logContent = result.content,
                    logStatus = result.logStatus,
                    isLogLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLogLoading = false,
                    logError = "获取日志失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 在调用协程中同步拉取最新日志
     */
    private suspend fun fetchLatestLog() {
        val taskId = _uiState.value.logTaskId ?: return
        if (_uiState.value.selectedLogIndex >= 0) return // 已选历史文件时不轮询
        repository.getCronLog(taskId).onSuccess { result ->
            _uiState.value = _uiState.value.copy(
                logContent = result.content,
                logStatus = result.logStatus,
                isLogLoading = false,
                logError = null
            )
        }.onFailure { e ->
            if (_uiState.value.logStatus == null) {
                _uiState.value = _uiState.value.copy(isLogLoading = false)
            }
        }
    }

    /**
     * Composable 手动刷新/轮询入口（对外保持 public）
     */
    fun fetchLogContent() {
        viewModelScope.launch { fetchLatestLog() }
    }

    /**
     * 关闭日志查看器
     */
    fun closeLogViewer() {
        logPollingJob?.cancel()
        logPollingJob = null
        _uiState.value = _uiState.value.copy(
            logTaskId = null,
            logTaskName = "",
            logContent = "",
            logFiles = emptyList(),
            selectedLogIndex = -1,
            logStatus = null,
            isLogLoading = false,
            logError = null
        )
    }

    private fun singleOp(id: String, label: String, op: suspend (List<String>) -> Result<Any>) {
        val numericId = id.toIntOrNull()
        if (numericId == null) {
            _uiState.value = _uiState.value.copy(
                error = "$label 失败: 无效的任务ID($id)，不是数字"
            )
            return
        }
        viewModelScope.launch {
            op(listOf(id)).onSuccess {
                _uiState.value = _uiState.value.copy(
                    error = "$label 成功(ID: $numericId)"
                )
                loadTasks()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = "$label 失败(ID: $numericId): ${e.message ?: "未知错误"}"
                )
            }
        }
    }

    private fun batchOp(op: suspend (List<String>) -> Result<Any>) {
        viewModelScope.launch {
            op(_uiState.value.selectedIds.toList()).onSuccess {
                clearSelection(); loadTasks()
            }
        }
    }
}
