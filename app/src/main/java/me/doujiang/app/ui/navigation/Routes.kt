package me.doujiang.app.ui.navigation

/**
 * 导航路由定义
 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TASKS = "tasks"
    const val ENVIRONMENTS = "environments"
    const val CONFIG = "config"
    const val SCRIPTS = "scripts"
    const val DEPENDENCIES = "dependencies"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val EDITOR = "editor/{type}/{id}"

    fun taskDetail(taskId: String) = "task_detail/$taskId"
    fun editor(type: String, id: String) = "editor/$type/$id"
}
