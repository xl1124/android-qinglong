package me.doujiang.app.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.LoadingIndicator

@Composable
fun ConfigScreen(
    onBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (uiState.isViewingFile) {
                AppTopBar(
                    title = if (uiState.isEditing) "编辑: ${uiState.selectedFile}" else uiState.selectedFile,
                    onBack = { viewModel.goBackToList() },
                    actions = {
                        if (uiState.isEditing) {
                            TextButton(onClick = { viewModel.saveConfig() }, enabled = !uiState.isSaving) {
                                Text("保存")
                            }
                            TextButton(onClick = { viewModel.cancelEdit() }) { Text("取消") }
                        } else {
                            IconButton(onClick = { viewModel.startEdit() }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                        }
                    }
                )
            } else {
                AppTopBar(title = "配置文件", onBack = null)
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))

            uiState.error != null && !uiState.isViewingFile -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.error ?: "未知错误", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadConfigFiles() }) { Text("重试") }
                    }
                }
            }

            uiState.isViewingFile -> {
                // 文件内容查看/编辑模式
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = uiState.editedContent,
                            onValueChange = { viewModel.onEditContentChanged(it) },
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            maxLines = Int.MAX_VALUE
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = uiState.configContent.ifEmpty { "# 暂无配置内容" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            else -> {
                // 配置文件列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column {
                                Text(
                                    "选择配置文件",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                                )
                                uiState.configFiles.forEach { file ->
                                    Surface(
                                        onClick = { viewModel.openFile(file.value) },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(text = file.title, modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 保存成功提示
    if (uiState.showSavedSnackbar) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissSnackbar()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = { TextButton(onClick = { viewModel.dismissSnackbar() }) { Text("关闭") } }
        ) { Text("配置保存成功") }
    }
}
