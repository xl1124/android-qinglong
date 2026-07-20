package me.doujiang.app.ui.screens.dependencies

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doujiang.app.data.model.Dependency
import me.doujiang.app.ui.components.*
import me.doujiang.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependenciesScreen(
    onBack: () -> Unit,
    viewModel: DependenciesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchMenu by remember { mutableStateOf(false) }
    var deleteDepTarget by remember { mutableStateOf<Dependency?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (uiState.isSelectionMode) {
                    "已选择 ${uiState.selectedIds.size} 项"
                } else "依赖管理",
                onBack = if (uiState.isSelectionMode) {
                    { viewModel.clearSelection() }
                } else onBack,
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
            // 类型筛选
            FilterChipRow(
                selectedType = uiState.selectedType,
                onTypeSelected = { viewModel.setType(it) }
            )

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.dependencies.isEmpty() -> EmptyState(
                    icon = Icons.Default.Inventory2,
                    title = "暂无依赖",
                    subtitle = "点击右下角按钮添加依赖",
                    modifier = Modifier.weight(1f)
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.dependencies, key = { it.id ?: it._id ?: "${it.name}_${it.type}" }) { dep ->
                        DependencyCard(
                            dep = dep,
                            isSelected = uiState.selectedIds.contains(dep._id),
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSelection(dep._id ?: "")
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(dep._id ?: "") },
                            onViewLog = { viewModel.showLog(dep) },
                            onReinstall = { viewModel.reinstallSingle(dep) },
                            onDelete = { deleteDepTarget = dep }
                        )
                    }
                }
            }
        }
    }

    // 添加对话框
    if (uiState.showAddDialog) {
        AddDependencyDialog(
            onAdd = { name, type -> viewModel.addDependency(name, type) },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    // 单个删除确认
    deleteDepTarget?.let { dep ->
        ConfirmDialog(
            title = "删除依赖",
            message = "确定要删除 \"${dep.title}\" 吗？",
            onConfirm = {
                viewModel.deleteSingle(dep)
                deleteDepTarget = null
            },
            onDismiss = { deleteDepTarget = null }
        )
    }

    // 批量删除确认
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除依赖",
            message = "确定要删除选中的 ${uiState.selectedIds.size} 个依赖吗？",
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
                        onClick = { showBatchMenu = false; viewModel.reinstallSelected() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("重新安装") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchMenu = false }) { Text("关闭") }
            }
        )
    }

    // 日志对话框（支持轮询）
    uiState.showLogDialog?.let { dep ->
        val isPolling = uiState.pendingDepLogId != null
        if (isPolling) {
            LaunchedEffect(Unit) {
                while (uiState.pendingDepLogId != null) {
                    viewModel.fetchDepLog()
                    delay(2000)
                }
            }
        }
        AlertDialog(
            onDismissRequest = {
                viewModel.hideLog()
                viewModel.clearPendingDepLog()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${dep.title} 安装日志", modifier = Modifier.weight(1f))
                    if (isPolling) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }
            },
            text = {
                val logText = dep.log?.joinToString("\n") ?: "暂无日志"
                val logScroll = rememberScrollState()
                Box(modifier = Modifier.heightIn(max = 360.dp).fillMaxWidth()) {
                    Text(
                        text = logText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        modifier = Modifier.verticalScroll(logScroll).fillMaxWidth().padding(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.hideLog()
                    viewModel.clearPendingDepLog()
                }) { Text("关闭") }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FilterChipRow(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    val types = listOf(
        -1 to "全部",
        Dependency.TYPE_NODEJS to "Node.js",
        Dependency.TYPE_PYTHON to "Python3",
        Dependency.TYPE_LINUX to "Linux"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (value, label) ->
            FilterChip(
                selected = if (selectedType == -1) value == -1 else selectedType == value,
                onClick = { onTypeSelected(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DependencyCard(
    dep: Dependency,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onViewLog: () -> Unit,
    onReinstall: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val statusColor = when (dep.status) {
        0 -> MaterialTheme.colorScheme.primary          // 安装中
        1 -> MaterialTheme.colorScheme.primary          // 已安装
        2 -> MaterialTheme.colorScheme.error            // 安装失败
        3 -> MaterialTheme.colorScheme.primary          // 卸载中
        4 -> MaterialTheme.colorScheme.outline          // 已卸载
        5 -> MaterialTheme.colorScheme.error            // 卸载失败
        6 -> MaterialTheme.colorScheme.primary          // 队列中
        7 -> MaterialTheme.colorScheme.outline          // 已取消
        else -> MaterialTheme.colorScheme.outline
    }

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
                        text = dep.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(label = dep.statusLabel, color = statusColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dep.typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSelectionMode) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("重新安装") },
                            onClick = { showMenu = false; onReinstall() },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("查看日志") },
                            onClick = { showMenu = false; onViewLog() },
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
@OptIn(ExperimentalMaterial3Api::class)
private fun AddDependencyDialog(
    onAdd: (name: String, type: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(Dependency.TYPE_NODEJS) }
    var showTypeMenu by remember { mutableStateOf(false) }
    val types = listOf(
        Dependency.TYPE_NODEJS to "Node.js",
        Dependency.TYPE_PYTHON to "Python3",
        Dependency.TYPE_LINUX to "Linux"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加依赖") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("依赖名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = { showTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = types.first { it.first == selectedType }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        types.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = value
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, selectedType) },
                enabled = name.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
