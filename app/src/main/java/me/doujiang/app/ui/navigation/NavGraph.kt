package me.doujiang.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.doujiang.app.ui.screens.config.ConfigScreen
import me.doujiang.app.ui.screens.dependencies.DependenciesScreen
import me.doujiang.app.ui.screens.env.EnvScreen
import me.doujiang.app.ui.screens.main.MainScreen
import me.doujiang.app.ui.screens.login.LoginScreen
import me.doujiang.app.ui.screens.login.LoginViewModel
import me.doujiang.app.ui.screens.logs.LogsScreen
import me.doujiang.app.ui.screens.scripts.ScriptsScreen
import me.doujiang.app.ui.screens.settings.SettingsScreen
import me.doujiang.app.ui.screens.tasks.TasksScreen

@Composable
fun QingLongNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.checkSession()
            }

            if (uiState.sessionChecking) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.HOME) {
            MainScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TASKS) {
            TasksScreen(
                onBack = { navController.popBackStack() },
                onViewTaskLogs = { navController.navigate(Routes.LOGS) }
            )
        }

        composable(Routes.ENVIRONMENTS) {
            EnvScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CONFIG) {
            ConfigScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SCRIPTS) {
            ScriptsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DEPENDENCIES) {
            DependenciesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LOGS) {
            LogsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
