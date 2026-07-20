package me.doujiang.app.ui.screens.scripts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.doujiang.app.data.model.ScriptFile
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.EmptyState
import me.doujiang.app.ui.components.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptsScreen(
    onBack: () -> Unit,
    viewModel: ScriptsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }

    // 从 URI 读取文件名和内容
    fun getFileName(uri: Uri): String {
        var name = "script.js"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: "script.js"
            }
        }
        return name
    }

    suspend fun readFileContent(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        }
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                uploading = true
                try {
                    val fileName = getFileName(uri)
                    val content = readFileContent(uri)
                    viewModel.uploadScript(fileName, content) { success, msg ->
                        val toastMsg = if (success) "脚本 $msg 上传成功" else "上传失败: $msg"
                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    uploading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (uiState.isViewingContent) {
                    uiState.currentFile?.title ?: "脚本内容"
                } else if (uiState.currentPath.isEmpty()) "脚本管理" else uiState.currentPath,
                onBack = if (uiState.isViewingContent) {
                    { viewModel.closeContent() }
                } else if (uiState.currentPath.isNotEmpty()) {
                    { viewModel.goBack() }
                } else onBack,
                actions = {
                    if (uiState.isViewingContent && !uiState.isEditing) {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                    if (uiState.isEditing) {
                        IconButton(onClick = { viewModel.saveScript() }) {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isViewingContent && !uploading) {
                ExtendedFloatingActionButton(
                    onClick = { uploadLauncher.launch(arrayOf("*/*")) },
                    icon = { Icon(Icons.Default.Upload, contentDescription = null) },
                    text = { Text("上传") }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))

            uiState.isViewingContent -> {
                // 查看文件内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // 文件信息栏
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.currentFile?.title ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.isEditing) {
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { viewModel.cancelEditing() }) {
                                    Text("取消", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // 内容（编辑模式或只读模式）
                    if (uiState.isEditing) {
                        val editText = uiState.editContent
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { viewModel.onEditContentChanged(it) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Text(
                            text = uiState.currentContent ?: "暂无内容",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        )
                    }
                }
            }

            uiState.files.isEmpty() -> EmptyState(
                icon = Icons.Default.Description,
                title = "暂无脚本",
                subtitle = "连接青龙面板后将在此显示脚本文件",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 面包屑导航
                if (uiState.currentPath.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { viewModel.goBack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ArrowBackIos, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("返回上级目录")
                        }
                    }
                }

                items(uiState.files, key = { it.path ?: it.title }) { file ->
                    ScriptFileItem(
                        file = file,
                        onClick = {
                            if (file.isDir || file.dir) {
                                viewModel.enterDir(file)
                            } else {
                                viewModel.viewFile(file)
                            }
                        },
                        onDelete = { viewModel.deleteScript(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScriptFileItem(
    file: ScriptFile,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val isDir = file.isDir || file.dir

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (isDir) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.size > 0) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isDir) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Icon(
                imageVector = if (isDir) Icons.Default.ChevronRight else Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
