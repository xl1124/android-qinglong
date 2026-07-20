package me.doujiang.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.ModuleCard
import me.doujiang.app.ui.components.ConfirmDialog
import me.doujiang.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("定时精灵")
                        if (uiState.serverUrl.isNotEmpty()) {
                            Text(
                                text = uiState.serverUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "退出登录")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 用户信息卡片
                if (uiState.username.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                uiState.systemInfo?.let { info ->
                                    Text(
                                        text = "青龙 ${info.version ?: "未知"} | ${info.type ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 功能模块网格
                Text(
                    text = "功能模块",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ModuleCard(
                    title = "定时任务",
                    subtitle = "管理 Cron 定时任务",
                    icon = Icons.Default.Schedule,
                    onClick = { onNavigateTo(Routes.TASKS) }
                )

                ModuleCard(
                    title = "环境变量",
                    subtitle = "管理环境变量配置",
                    icon = Icons.Default.Code,
                    onClick = { onNavigateTo(Routes.ENVIRONMENTS) }
                )

                ModuleCard(
                    title = "配置文件",
                    subtitle = "查看和编辑面板配置",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigateTo(Routes.CONFIG) }
                )

                ModuleCard(
                    title = "脚本管理",
                    subtitle = "管理面板中的脚本文件",
                    icon = Icons.Default.Description,
                    onClick = { onNavigateTo(Routes.SCRIPTS) }
                )

                ModuleCard(
                    title = "依赖管理",
                    subtitle = "Node.js/Python/Linux 依赖",
                    icon = Icons.Default.Inventory2,
                    onClick = { onNavigateTo(Routes.DEPENDENCIES) }
                )

                ModuleCard(
                    title = "任务日志",
                    subtitle = "查看运行日志文件",
                    icon = Icons.Default.Article,
                    onClick = { onNavigateTo(Routes.LOGS) }
                )

                ModuleCard(
                    title = "系统设置",
                    subtitle = "常规设置和登录日志",
                    icon = Icons.Default.Tune,
                    onClick = { onNavigateTo(Routes.SETTINGS) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 退出确认
    if (showLogoutDialog) {
        ConfirmDialog(
            title = "退出登录",
            message = "确定要退出当前账号吗？",
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}
