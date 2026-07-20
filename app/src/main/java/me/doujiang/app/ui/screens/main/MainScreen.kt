package me.doujiang.app.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.doujiang.app.ui.screens.config.ConfigScreen
import me.doujiang.app.ui.screens.dependencies.DependenciesScreen
import me.doujiang.app.ui.screens.env.EnvScreen
import me.doujiang.app.ui.screens.logs.LogsScreen
import me.doujiang.app.ui.screens.scripts.ScriptsScreen
import me.doujiang.app.ui.screens.settings.LoginLogsScreen
import me.doujiang.app.ui.screens.settings.SettingsScreen
import me.doujiang.app.ui.screens.subscriptions.SubscriptionsScreen
import me.doujiang.app.ui.screens.tasks.TasksScreen

/**
 * 底部导航栏 Tab 定义
 */
private data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val content: @Composable (onNavigate: (String) -> Unit) -> Unit
)

@Composable
fun MainScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    // 子屏幕状态（脚本/依赖管理等，避免 NavHost 路由跳转导致 tab 重置）
    var subScreen by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val onNavigate: (String) -> Unit = { route ->
        if (route == "scripts" || route == "dependencies" || route == "logs" || route == "login_logs" || route == "subscriptions") {
            subScreen = route
        } else {
            navController.navigate(route)
        }
    }

    // 始终显示底部导航栏，子屏幕只替换内容区
    val tabs = remember(subScreen) {
        listOf(
            BottomTab("定时任务", Icons.Default.Schedule) { _ ->
                TasksScreen(onBack = {}, onViewTaskLogs = { onNavigate("logs") })
            },
            BottomTab("环境变量", Icons.Default.Code) { _ ->
                EnvScreen(onBack = {})
            },
            BottomTab("配置文件", Icons.Default.Settings) { _ ->
                ConfigScreen(onBack = {})
            },
            BottomTab("系统设置", Icons.Default.Person) { _ ->
                SettingsScreen(
                    onBack = {},
                    onLogout = onLogout,
                    onNavigateTo = onNavigate
                )
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            tabs[selectedTab].content { }
        }
    }

    // 子屏幕弹窗（底栏和主页面可见，弹窗浮于上方）
    if (subScreen != null) {
        Dialog(onDismissRequest = { subScreen = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.background
            ) {
                when (subScreen) {
                    "scripts" -> ScriptsScreen(onBack = { subScreen = null })
                    "dependencies" -> DependenciesScreen(onBack = { subScreen = null })
                    "logs" -> LogsScreen(onBack = { subScreen = null })
                    "login_logs" -> LoginLogsScreen(onBack = { subScreen = null })
                    "subscriptions" -> SubscriptionsScreen(onBack = { subScreen = null })
                }
            }
        }
    }
}
