package me.doujiang.app.ui.screens.logs

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
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.EmptyState
import me.doujiang.app.ui.components.LoadingIndicator

data class LogEntry(
    val name: String,
    val size: String,
    val time: String
)

@Composable
fun LogsScreen(
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<LogEntry?>(null) }
    var logContent by remember { mutableStateOf("") }

    // 模拟加载
    LaunchedEffect(Unit) {
        // 实际调用 repository 获取日志列表
        isLoading = false
        logs = emptyList() // 连接后从 API 获取
    }

    if (selectedLog != null) {
        // 日志详情
        Scaffold(
            topBar = {
                AppTopBar(
                    title = selectedLog!!.name,
                    onBack = { selectedLog = null }
                )
            }
        ) { padding ->
            Text(
                text = logContent.ifEmpty { "# ${selectedLog!!.name}\n# 大小: ${selectedLog!!.size}\n# 时间: ${selectedLog!!.time}\n\n暂无日志内容" },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        }
    } else {
        Scaffold(
            topBar = {
                AppTopBar(title = "任务日志", onBack = onBack)
            }
        ) { padding ->
            when {
                isLoading -> LoadingIndicator(modifier = Modifier.padding(padding))
                logs.isEmpty() -> EmptyState(
                    icon = Icons.Default.Article,
                    title = "暂无日志",
                    subtitle = "运行任务后日志将在此显示",
                    modifier = Modifier.padding(padding)
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Surface(
                            onClick = {
                                selectedLog = log
                                logContent = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${log.time} | ${log.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
