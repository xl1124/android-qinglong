package me.doujiang.app.ui.screens.settings

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.doujiang.app.data.local.AccountEntry
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.repository.QingLongRepository
import me.doujiang.app.ui.components.AppTopBar
import me.doujiang.app.ui.components.ConfirmDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateTo: ((String) -> Unit)? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSwitchAccount by remember { mutableStateOf(false) }
    var showLoginLogs by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0.0") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val localStorage = LocalStorage(context)

    LaunchedEffect(Unit) {
        val repo = QingLongRepository(LocalStorage(context))
        serverUrl = repo.getServerUrl()
        username = repo.getUsername()
    }

    Scaffold(
        topBar = { AppTopBar(title = "系统设置", onBack = null) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 账号信息
            item {
                SettingsSection(title = "账号信息") {
                    SettingsItem(icon = Icons.Default.Person, title = "当前用户", subtitle = username.ifEmpty { "未登录" })
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsItem(icon = Icons.Default.Dns, title = "服务器地址", subtitle = serverUrl.ifEmpty { "未配置" })
                }
            }

            // 功能
            item {
                SettingsSection(title = "功能") {
                    SettingsClickableItem(icon = Icons.Default.Description, title = "脚本管理", onClick = { onNavigateTo?.invoke("scripts") })
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsClickableItem(icon = Icons.Default.Inventory2, title = "依赖管理", onClick = { onNavigateTo?.invoke("dependencies") })
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsClickableItem(icon = Icons.Default.RssFeed, title = "订阅管理", onClick = { onNavigateTo?.invoke("subscriptions") })
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsClickableItem(icon = Icons.Default.SwapHoriz, title = "切换账号", onClick = { showSwitchAccount = true })
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    Surface(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Text("退出登录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 安全
            item {
                SettingsSection(title = "安全") {
                    SettingsClickableItem(icon = Icons.Default.History, title = "登录日志", onClick = { onNavigateTo?.invoke("login_logs") })
                }
            }

            // 主题
            item {
                SettingsSection(title = "主题") {
                    val currentThemeMode by localStorage.themeMode.collectAsState(initial = "system")
                    var expanded by remember { mutableStateOf(false) }
                    Surface(onClick = { expanded = true }, color = Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("深色模式", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    when (currentThemeMode) { "dark" -> "深色"; "light" -> "浅色"; else -> "跟随系统" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                DropdownMenuItem(text = { Text("跟随系统") }, onClick = {
                                    expanded = false; scope.launch { localStorage.saveThemeMode("system") }
                                })
                                DropdownMenuItem(text = { Text("浅色") }, onClick = {
                                    expanded = false; scope.launch { localStorage.saveThemeMode("light") }
                                })
                                DropdownMenuItem(text = { Text("深色") }, onClick = {
                                    expanded = false; scope.launch { localStorage.saveThemeMode("dark") }
                                })
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    val isDynamic by localStorage.dynamicColor.collectAsState(initial = true)
                    Surface(color = Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Text("动态取色", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isDynamic, onCheckedChange = {
                                scope.launch { localStorage.saveDynamicColor(it) }
                            })
                        }
                    }
                }
            }

            // 关于
            item {
                SettingsSection(title = "关于") {
                    SettingsItem(icon = Icons.Default.Info, title = "应用版本", subtitle = versionName)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsItem(icon = Icons.Default.Code, title = "技术栈", subtitle = "Kotlin + Jetpack Compose + Material 3")
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    SettingsClickableItem(icon = Icons.Default.BugReport, title = "系统日志", onClick = { showDebugDialog = true })
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showSwitchAccount) {
        val accountsState = remember { mutableStateOf<List<AccountEntry>>(emptyList()) }
        LaunchedEffect(showSwitchAccount) {
            if (showSwitchAccount) {
                accountsState.value = LocalStorage(context).getAccounts()
            }
        }
        AlertDialog(
            onDismissRequest = { showSwitchAccount = false },
            title = { Text("切换账号") },
            text = {
                if (accountsState.value.isEmpty()) {
                    Text("暂无其他账号，登录新账号后会自动保存")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(accountsState.value) { account ->
                            Surface(
                                onClick = {
                                    showSwitchAccount = false
                                    scope.launch {
                                        LocalStorage(context).switchAccount(account)
                                    }
                                    onLogout()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(account.username, style = MaterialTheme.typography.bodyMedium)
                                        Text(account.server, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchAccount = false
                    scope.launch {
                        LocalStorage(context).saveCurrentAccount()
                    }
                    onLogout()
                }) { Text("添加新账号") }
            },
            dismissButton = { TextButton(onClick = { showSwitchAccount = false }) { Text("取消") } }
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "退出登录",
            message = "确定要退出当前账号吗？",
            onConfirm = {
                showLogoutDialog = false
                scope.launch {
                    QingLongRepository(LocalStorage(context)).logout()
                }
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDebugDialog) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) { null }
        val debugInfo = buildString {
            appendLine("应用包名: ${context.packageName}")
            appendLine("版本名称: ${packageInfo?.versionName ?: "未知"}")
            appendLine("版本号: ${packageInfo?.longVersionCode ?: "未知"}")
            appendLine("服务器地址: ${serverUrl.ifEmpty { "未配置" }}")
            appendLine("当前用户: ${username.ifEmpty { "未登录" }}")
            appendLine("Android SDK: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("构建指纹: ${android.os.Build.FINGERPRINT}")
        }
        AlertDialog(
            onDismissRequest = { showDebugDialog = false },
            title = { Text("系统日志 / 调试信息") },
            text = {
                Text(
                    text = debugInfo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showDebugDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
        )
        Column(content = content)
    }
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

/**
 * 登录日志页面
 */
@Composable
fun LoginLogsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var logs by remember { mutableStateOf<List<me.doujiang.app.data.model.LoginLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val repo = QingLongRepository(LocalStorage(ctx))
        repo.getLoginLogs().onSuccess { loginLogs ->
            logs = loginLogs
        }
        isLoading = false
    }

    Scaffold(
        topBar = { AppTopBar(title = "登录日志", onBack = onBack) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无登录日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (log.status == 0) Icons.Default.CheckCircle
                                    else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (log.status == 0)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.ip ?: "未知 IP",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = log.time ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!log.address.isNullOrBlank()) {
                                Text(
                                    text = log.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



