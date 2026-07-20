package me.doujiang.app.ui.screens.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import me.doujiang.app.data.model.Subscription
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.EmptyState
import me.doujiang.app.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(title = "订阅管理", onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建") }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
            uiState.subscriptions.isEmpty() && uiState.error == null -> EmptyState(
                icon = Icons.Default.RssFeed,
                title = "暂无订阅",
                subtitle = "点击右下角按钮添加订阅",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.subscriptions, key = { it.id ?: it.hashCode() }) { sub ->
                    SubCard(
                        sub = sub,
                        onRun = { viewModel.runSub(sub.id ?: return@SubCard) },
                        onStop = { viewModel.stopSub(sub.id ?: return@SubCard) },
                        onEnable = { viewModel.enableSub(sub.id ?: return@SubCard) },
                        onDisable = { viewModel.disableSub(sub.id ?: return@SubCard) },
                        onEdit = { viewModel.showEditDialog(sub) },
                        onDelete = { viewModel.deleteSub(sub.id ?: return@SubCard) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        SubDialog(
            title = "新建订阅",
            initial = null,
            onSave = { name, url, type, schedule -> viewModel.addSubscription(name, url, type, schedule) },
            onDismiss = { viewModel.hideAddDialog() }
        )
    }

    uiState.showEditDialog?.let { sub ->
        SubDialog(
            title = "编辑订阅",
            initial = sub,
            onSave = { name, url, type, schedule ->
                viewModel.updateSubscription(sub.id ?: return@SubDialog, name, url, type, schedule)
            },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }

    // 错误提示
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            kotlinx.coroutines.delay(3000)
        }
    }
}

@Composable
private fun SubCard(
    sub: Subscription,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (sub.stateCode) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.outline
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = { showMenu = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = statusColor
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sub.name ?: sub.alias ?: "未命名",
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (!sub.url.isNullOrBlank()) {
                    Text(sub.url, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (sub.stateCode == 0) {
                        DropdownMenuItem(text = { Text("停止") }, onClick = { showMenu = false; onStop() },
                            leadingIcon = { Icon(Icons.Default.Stop, null) })
                    } else {
                        DropdownMenuItem(text = { Text("运行") }, onClick = { showMenu = false; onRun() },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                    }
                    if (sub.isDisabled == 1) {
                        DropdownMenuItem(text = { Text("启用") }, onClick = { showMenu = false; onEnable() },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                    } else {
                        DropdownMenuItem(text = { Text("禁用") }, onClick = { showMenu = false; onDisable() },
                            leadingIcon = { Icon(Icons.Default.Cancel, null) })
                    }
                    DropdownMenuItem(text = { Text("编辑") }, onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("删除") }, onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubDialog(
    title: String,
    initial: Subscription?,
    onSave: (name: String, url: String, type: String, schedule: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: initial?.alias ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "public-repo") }
    var schedule by remember { mutableStateOf(initial?.schedule ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("订阅地址") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(value = type, onValueChange = {}, readOnly = true,
                        label = { Text("类型") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("public-repo", "private-repo", "file").forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = schedule, onValueChange = { schedule = it },
                    label = { Text("定时规则 (cron)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, url, type, schedule) }, enabled = name.isNotBlank() && url.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
