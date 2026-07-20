package me.doujiang.app.ui.screens.tasks

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doujiang.app.data.model.CronTask
import me.doujiang.app.data.model.LogFileInfo
import me.doujiang.app.ui.components.*
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    viewModel: TasksViewModel = viewModel(),
    onViewTaskLogs: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<CronTask?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchMenu by remember { mutableStateOf(false) }

    // 显示错误
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (uiState.isSelectionMode) {
                    "已选择 ${uiState.selectedIds.size} 项"
                } else "定时任务",
                onBack = if (uiState.isSelectionMode) {
                    { viewModel.clearSelection() }
                } else null,
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { showBatchMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "批量操作")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("新建") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 状态筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "全部", "enabled" to "已启用", "disabled" to "已禁用").forEach { (value, label) ->
                    FilterChip(
                        selected = uiState.statusFilter == value,
                        onClick = { viewModel.setStatusFilter(value) },
                        label = { Text(label) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadTasks() },
                modifier = Modifier.weight(1f)
            ) {
                when {
                    uiState.isLoading && viewModel.filteredTasks.isEmpty() -> LoadingIndicator()
                    viewModel.filteredTasks.isEmpty() -> EmptyState(
                        icon = Icons.Default.Schedule,
                        title = "暂无定时任务",
                        subtitle = "点击右下角按钮创建新任务"
                    )
                    else -> {
                        val filtered by remember(uiState.tasks, uiState.statusFilter) {
                            derivedStateOf {
                                when (uiState.statusFilter) {
                                    "enabled" -> uiState.tasks.filter { it.isDisabled == 0 }
                                    "disabled" -> uiState.tasks.filter { it.isDisabled == 1 }
                                    else -> uiState.tasks
                                }
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        items(filtered, key = { (it.id?.toString() ?: it._id) ?: "" }) { task ->
                    val taskId = (task.id?.toString() ?: task._id) ?: ""
                    TaskCard(
                        task = task,
                        isSelected = uiState.selectedIds.contains(taskId),
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.toggleSelection(taskId)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelection(taskId)
                        },
                        onEdit = { viewModel.showEditDialog(task) },
                        onRun = { viewModel.runTaskAndShowLog(taskId, task.name) },
                        onStop = { viewModel.stopTask(taskId) },
                        onToggleDisable = {
                            if (task.isDisabled == 1) {
                                viewModel.enableTask(taskId)
                            } else {
                                viewModel.disableTask(taskId)
                            }
                        },
                        onTogglePin = {
                            if (task.isPinned == 1) {
                                viewModel.unpinTask(taskId)
                            } else {
                                viewModel.pinTask(taskId)
                            }
                        },
                        onViewLogs = { viewModel.showTaskLog(taskId, task.name, task.command) },
                        onDelete = { viewModel.deleteTask(taskId) }
                    )
                    }
                }
            }
        }
    }
    }
    }

    // 添加任务对话框
    if (uiState.showAddDialog) {
        TaskEditDialog(
            title = "新建任务",
            initialTask = null,
            onSave = { name, command, schedule ->
                viewModel.addTask(name, command, schedule)
            },
            onUploadScript = { fileName, content ->
                viewModel.uploadScript(fileName, content) { success, msg ->
                    val toastMsg = if (success) "脚本 $msg 上传成功" else "上传失败: $msg"
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    // 编辑任务对话框
    uiState.showEditDialog?.let { task ->
        TaskEditDialog(
            title = "编辑任务",
            initialTask = task,
            onSave = { name, command, schedule ->
                viewModel.updateTask(task._id ?: "", name, command, schedule)
            },
            onUploadScript = { fileName, content ->
                viewModel.uploadScript(fileName, content) { success, msg ->
                    val toastMsg = if (success) "脚本 $msg 上传成功" else "上传失败: $msg"
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 删除确认
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除任务",
            message = "确定要删除选中的 ${uiState.selectedIds.size} 个任务吗？",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteSelected()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // 批量操作菜单
    if (showBatchMenu) {
        AlertDialog(
            onDismissRequest = { showBatchMenu = false },
            title = { Text("批量操作") },
            text = {
                Column {
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.runSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("运行") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.stopSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text("停止") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.enableSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("启用") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.disableSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(8.dp)); Text("禁用") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.pinSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.PushPin, null); Spacer(Modifier.width(8.dp)); Text("置顶") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.unpinSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.PushPin, null); Spacer(Modifier.width(8.dp)); Text("取消置顶") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchMenu = false }) { Text("关闭") }
            }
        )
    }

    // 任务日志查看器
    if (uiState.logTaskId != null) {
        TaskLogDialog(
            taskName = uiState.logTaskName,
            logContent = uiState.logContent,
            logFiles = uiState.logFiles,
            selectedLogIndex = uiState.selectedLogIndex,
            logStatus = uiState.logStatus,
            isLoading = uiState.isLogLoading,
            error = uiState.logError,
            onSelectFile = { index, dir, file -> viewModel.selectLogFile(index, dir, file) },
            onShowLatest = { viewModel.showLatestLog() },
            onClose = { viewModel.closeLogViewer() },
            onPoll = { viewModel.fetchLogContent() }
        )
    }
}

@Composable
private fun TaskCard(
    task: CronTask,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onRun: () -> Unit = {},
    onStop: () -> Unit = {},
    onToggleDisable: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onViewLogs: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val statusColor = when (task.stateCode) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.outline
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = {
            if (isSelectionMode) onClick()
            else showMenu = true
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 状态指示器
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(0.dp)
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = statusColor
                ) {}
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.isPinned == 1) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(label = task.stateLabel, color = statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.schedule,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.lastExecuteTime != null) {
                    Text(
                        text = "上次: ${task.lastExecuteTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // 更多操作按钮
            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("启动") },
                            onClick = { showMenu = false; onRun() },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                        )
                        if (task.stateCode == 0) {
                            DropdownMenuItem(
                                text = { Text("停止") },
                                onClick = { showMenu = false; onStop() },
                                leadingIcon = { Icon(Icons.Default.Stop, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (task.isPinned == 1) "取消置顶" else "置顶") },
                            onClick = { showMenu = false; onTogglePin() },
                            leadingIcon = { Icon(
                                if (task.isPinned == 1) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = null
                            ) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (task.isDisabled == 1) "启用" else "禁用") },
                            onClick = { showMenu = false; onToggleDisable() },
                            leadingIcon = {
                                Icon(
                                    if (task.isDisabled == 1) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("查看日志") },
                            onClick = { showMenu = false; onViewLogs() },
                            leadingIcon = { Icon(Icons.Default.Article, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskEditDialog(
    title: String,
    initialTask: CronTask?,
    onSave: (name: String, command: String, schedule: String) -> Unit,
    onUploadScript: (fileName: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialTask?.name ?: "") }
    var command by remember { mutableStateOf(initialTask?.command ?: "") }
    var schedule by remember { mutableStateOf(initialTask?.schedule ?: "") }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 从 URI 读取文件名
    fun getFileName(uri: Uri): String {
        var name = "script.js"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: name
            }
        }
        return name
    }

    // 从 URI 读取文件内容
    fun readFileContent(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileName(uri)
            uploading = true
            uploadError = null
            scope.launch(Dispatchers.IO) {
                val content = readFileContent(uri)
                withContext(Dispatchers.Main) {
                    onUploadScript(fileName, content)
                    command = "task $fileName"
                    if (name.isBlank()) {
                        name = fileName.removeSuffix(".js")
                            .removeSuffix(".py").removeSuffix(".sh")
                            .removeSuffix(".ts")
                    }
                    if (schedule.isBlank()) {
                        schedule = "0 0 * * *"
                    }
                    uploading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("命令") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("定时规则 (Cron)") },
                    placeholder = { Text("例如: */5 * * * *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 上传脚本按钮
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf(
                            "application/javascript",
                            "text/javascript",
                            "text/x-python",
                            "text/x-shellscript",
                            "text/plain",
                            "*/*"
                        ))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploading
                ) {
                    if (uploading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("上传中…")
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("上传脚本文件")
                    }
                }

                if (uploadError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uploadError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, command, schedule) },
                enabled = name.isNotBlank() && command.isNotBlank() && !uploading
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 任务日志查看器对话框
 * - 标题栏：任务名 + 状态标签 + 日志文件切换下拉
 * - 内容区：等宽日志，可滚动
 * - 底部：关闭/后台运行按钮
 */
@Composable
private fun TaskLogDialog(
    taskName: String,
    logContent: String,
    logFiles: List<LogFileInfo>,
    selectedLogIndex: Int,
    logStatus: String?,
    isLoading: Boolean,
    error: String?,
    onSelectFile: (Int, String, String) -> Unit,
    onShowLatest: () -> Unit,
    onClose: () -> Unit,
    onPoll: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isLiveMode = selectedLogIndex == -1
    // 运行中：后端返回 running，或者还没返回状态（刚启动）且在看最新日志
    val isRunning = logStatus == "running" || (logStatus == null && isLiveMode)
    val showFileSelector = logFiles.isNotEmpty()

    // 日志更新时自动滚动到底部（等一帧让布局完成）
    LaunchedEffect(logContent) {
        if (logContent.isNotEmpty()) {
            withFrameMillis { }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun formatLogTime(ts: Double): String {
        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts.toLong()))
    }

    // 日志切换下拉状态
    var logDropdownExpanded by remember { mutableStateOf(false) }
    val currentLabel = if (selectedLogIndex >= 0 && selectedLogIndex < logFiles.size)
        formatLogTime(logFiles[selectedLogIndex].time)
    else "最新日志"

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(taskName, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    when {
                        isLoading && logContent.isEmpty() ->
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        isRunning && isLiveMode ->
                            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                                Text("运行中", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        !isRunning && isLiveMode ->
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape = MaterialTheme.shapes.small) {
                                Text(if (logStatus == "completed") "已完成" else "已结束",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                    }
                }
                if (showFileSelector) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)) {
                        Text("日志：", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            TextButton(onClick = { logDropdownExpanded = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text(currentLabel, maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = logDropdownExpanded,
                                onDismissRequest = { logDropdownExpanded = false },
                                modifier = Modifier.widthIn(max = 280.dp).heightIn(max = 300.dp)) {
                                DropdownMenuItem(text = { Text("最新日志") },
                                    onClick = { logDropdownExpanded = false
                                        if (!isLiveMode) onShowLatest() },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null,
                                        tint = if (isLiveMode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant) })
                                HorizontalDivider()
                                logFiles.forEachIndexed { i, file ->
                                    val isSelected = selectedLogIndex == i
                                    DropdownMenuItem(
                                        text = { Column {
                                            Text(file.filename.removeSuffix(".log"),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface)
                                            Text(formatLogTime(file.time),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }},
                                        onClick = { logDropdownExpanded = false
                                            if (!isSelected) onSelectFile(i, file.directory, file.filename) },
                                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant) })
                                }
                            }
                        }
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 380.dp).fillMaxWidth()) {
                when {
                    error != null ->
                        Text(error, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp))
                    isLoading && logContent.isEmpty() ->
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("加载中…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    logContent.isEmpty() ->
                        Text("暂无日志内容", modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        val lines = remember(logContent) { logContent.split('\n') }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(8.dp)
                        ) {
                            // 行号列
                            Text(
                                text = lines.indices.joinToString("\n") { "${it + 1}" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.widthIn(min = 28.dp).padding(end = 8.dp)
                            )
                            // 日志内容列
                            Text(
                                text = logContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isRunning && isLiveMode) {
                TextButton(onClick = onClose) { Text("后台运行") }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = onClose) { Text("关闭") }
        }
    )
}
