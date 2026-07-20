package me.doujiang.app.ui.screens.env

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doujiang.app.data.model.Environment
import me.doujiang.app.ui.components.*
import me.doujiang.app.ui.theme.StatusDisabled
import me.doujiang.app.ui.theme.StatusRunning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvScreen(
    onBack: () -> Unit,
    viewModel: EnvViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var deleteEnvTarget by remember { mutableStateOf<Environment?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 显示错误提示
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
                } else "环境变量",
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
                    } else {
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "批量导入")
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
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            uiState.environments.isEmpty() -> EmptyState(
                icon = Icons.Default.Code,
                title = "暂无环境变量",
                subtitle = "点击右下角按钮添加",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.environments, key = { it.id?.toString() ?: it._id?.ifBlank { null } ?: "${it.name}_${it.value}" }) { env ->
                    EnvCard(
                        env = env,
                        isSelected = uiState.selectedIds.contains(env._id),
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.toggleSelection(env._id ?: "")
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(env._id ?: "") },
                        onEdit = { viewModel.showEditEnv(env) },
                        onToggleState = {
                            if (env.isDisabled == 0) viewModel.disableEnv(env)
                            else viewModel.enableEnv(env)
                        },
                        onDelete = { deleteEnvTarget = env }
                    )
                }
            }
        }
    }

    // 添加对话框
    if (uiState.showAddDialog) {
        EnvEditDialog(
            title = "新建环境变量",
            initialEnv = null,
            onSave = { name, value, remarks ->
                viewModel.addEnv(name, value, remarks)
            },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    // 编辑对话框
    uiState.showEditEnv?.let { env ->
        EnvEditDialog(
            title = "编辑环境变量",
            initialEnv = env,
            onSave = { name, value, remarks ->
                viewModel.updateEnv(env.copy(name = name, value = value, remarks = remarks))
            },
            onDismiss = { viewModel.hideEditEnv() }
        )
    }

    // 单个删除确认
    deleteEnvTarget?.let { env ->
        ConfirmDialog(
            title = "删除环境变量",
            message = "确定要删除 \"${env.name}\" 吗？",
            onConfirm = {
                viewModel.deleteEnv(env)
                deleteEnvTarget = null
            },
            onDismiss = { deleteEnvTarget = null }
        )
    }

    // 批量删除确认
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除环境变量",
            message = "确定要删除选中的 ${uiState.selectedIds.size} 个变量吗？",
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
                        onClick = { showBatchMenu = false; viewModel.enableSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("启用") }
                    TextButton(
                        onClick = { showBatchMenu = false; viewModel.disableSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(8.dp)); Text("禁用") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchMenu = false }) { Text("关闭") }
            }
        )
    }

    // 导入对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("快捷导入") },
            text = {
                Column {
                    Text("将 export 格式文本粘贴到下方：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("export 格式文本") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val envs = parseExportFormat(importText)
                        viewModel.addEnv(
                            envs.firstOrNull()?.name ?: "",
                            envs.firstOrNull()?.value ?: "",
                            envs.firstOrNull()?.remarks ?: ""
                        )
                        showImportDialog = false
                        importText = ""
                    },
                    enabled = importText.isNotBlank()
                ) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvCard(
    env: Environment,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleState: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = env.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (env.isDisabled == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = env.statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (env.isDisabled == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = env.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!env.remarks.isNullOrBlank()) {
                    Text(
                        text = "备注: ${env.remarks}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (!isSelectionMode) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多",
                            modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showMenu,
                        onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (env.isDisabled == 0) "禁用" else "启用") },
                            onClick = { showMenu = false; onToggleState() },
                            leadingIcon = { Icon(
                                if (env.isDisabled == 0) Icons.Default.Cancel
                                else Icons.Default.CheckCircle,
                                contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
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
private fun EnvEditDialog(
    title: String,
    initialEnv: Environment?,
    onSave: (name: String, value: String, remarks: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialEnv?.name ?: "") }
    var value by remember { mutableStateOf(initialEnv?.value ?: "") }
    var remarks by remember { mutableStateOf(initialEnv?.remarks ?: "") }
    var showValue by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("变量名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("变量值") },
                    trailingIcon = {
                        IconButton(onClick = { showValue = !showValue }) {
                            Icon(
                                if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showValue) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("备注") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, value, remarks) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 解析 export 格式的环境变量导入
 * export KEY="value"
 */
private fun parseExportFormat(text: String): List<me.doujiang.app.data.model.EnvRequest> {
    val pattern = Regex("export\\s+(\\w+)=\"([^\"]+)\"")
    return pattern.findAll(text).map { match ->
        me.doujiang.app.data.model.EnvRequest(
            name = match.groupValues[1],
            value = match.groupValues[2],
            remarks = null
        )
    }.toList()
}
