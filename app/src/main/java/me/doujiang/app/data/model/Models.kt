package me.doujiang.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 青龙面板 API 通用响应
 */
data class QingLongResponse<T>(
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null
)

/**
 * 登录请求
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 双因素认证请求
 */
data class TwoFactorRequest(
    val username: String,
    val password: String,
    val code: String
)

/**
 * 登录响应
 */
data class LoginData(
    val token: String? = null,
    @SerializedName("token_type")
    val tokenType: String? = null,
    @SerializedName("lastip")
    val lastIp: String? = null,
    @SerializedName("lastaddr")
    val lastAddr: String? = null,
    @SerializedName("lastlogon")
    val lastLogon: Long? = null,
    @SerializedName("is_two_factor")
    val isTwoFactor: String? = null
)

/**
 * 系统信息
 */
data class SystemInfo(
    val version: String? = null,
    @SerializedName("versionNew")
    val versionNew: String? = null,
    val type: String? = null,
    @SerializedName("nodeVersion")
    val nodeVersion: String? = null,
    val isInitialized: Boolean = false
)

/**
 * 定时任务
 */
data class CronTask(
    val _id: String? = null,
    val id: Int? = null,
    val name: String = "",
    val command: String = "",
    val schedule: String = "",
    val status: Int? = null,
    @SerializedName("isDisabled")
    val isDisabled: Int = 0,
    @SerializedName("isPinned")
    val isPinned: Int = 0,
    @SerializedName("isSystem")
    val isSystem: Int = 0,
    @SerializedName("last_execution_time")
    val lastExecuteTime: String? = null,
    @SerializedName("lastRunningTime")
    val lastRunningTime: Long? = null,
    @SerializedName("nextRunTime")
    val nextRunTime: Long? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    /**
     * 青龙官方 CrontabStatus: 0=running, 1=idle, 2=disabled, 3=queued
     */
    val stateCode: Int
        get() = when {
            isDisabled == 1 -> 2    // 已禁用(面板手动禁用)
            status == 0 -> 0         // 运行中
            status == 3 -> 3         // 队列中
            else -> 1                // 空闲
        }

    val stateLabel: String
        get() = when (stateCode) {
            0 -> "运行中"
            1 -> "空闲"
            2 -> "已禁用"
            3 -> "队列中"
            else -> "未知"
        }
}

/**
 * 定时任务列表响应 (官方 API 返回 data: {data: [...], total: N})
 */
data class CronListData(
    val data: List<CronTask> = emptyList(),
    val total: Int = 0
)

/**
 * 创建/更新任务请求
 */
data class TaskRequest(
    val name: String,
    val command: String,
    val schedule: String,
    val _id: String? = null
)

/**
 * 环境变量
 */
data class Environment(
    val _id: String? = null,
    val id: Int? = null,
    val name: String = "",
    val value: String = "",
    val remarks: String? = null,
    @SerializedName("status")
    val isDisabled: Int = 0,
    val timestamp: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    val statusCode: Int
        get() = if (isDisabled == 0) 0 else 1

    val statusLabel: String
        get() = if (isDisabled == 0) "已启用" else "已禁用"
}

/**
 * 环境变量创建请求
 */
data class EnvRequest(
    val name: String,
    val value: String,
    val remarks: String? = null
)

/**
 * 依赖 (匹配 Q1 的 Dependence 模型)
 * - id: 数字类型
 * - name: 依赖名称
 * - type: 数字 (0=nodejs, 1=python3, 2=linux)
 * - status: 数字状态码
 * - log: 日志数组
 * - remark: 备注
 */
data class Dependency(
    val _id: String? = null,
    val id: Int? = null,
    val name: String = "",
    val type: Int = 0,
    val status: Int = 7,
    val log: List<String>? = null,
    val remark: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    val title: String get() = name

    val statusLabel: String
        get() = when (status) {
            0 -> "安装中"
            1 -> "已安装"
            2 -> "安装失败"
            3 -> "卸载中"
            4 -> "已卸载"
            5 -> "卸载失败"
            6 -> "队列中"
            7 -> "已取消"
            else -> "未知"
        }

    val typeLabel: String
        get() = when (type) {
            0 -> "Node.js"
            1 -> "Python3"
            2 -> "Linux"
            else -> "未知"
        }

    companion object {
        const val TYPE_NODEJS = 0
        const val TYPE_PYTHON = 1
        const val TYPE_LINUX = 2
    }
}

/**
 * 文件/脚本
 */
data class ScriptFile(
    val title: String = "",
    @SerializedName("key")
    val path: String? = null,
    val content: String? = null,
    @SerializedName("type")
    val fileType: String? = null,
    val size: Int = -1,
    val time: String? = null,
    val parent: String? = null,
    val children: List<ScriptFile>? = null
) {
    val isDir: Boolean get() = fileType == "directory"
    val dir: Boolean get() = isDir
}

/**
 * 系统配置
 */
data class SystemConfig(
    val theme: String? = null,
    val logs: LogConfig? = null,
    val auth: AuthConfig? = null
)

data class LogConfig(
    val expire: Int? = null,
    val path: String? = null
)

data class AuthConfig(
    val token: String? = null,
    @SerializedName("loginLog")
    val loginLog: LoginLogConfig? = null
)

data class LoginLogConfig(
    val enabled: Boolean = true,
    val expiration: Int = 30
)

/**
 * 登录日志
 */
data class LoginLog(
    val id: String? = null,
    val ip: String? = null,
    val address: String? = null,
    val time: String? = null,
    val status: Int? = null,
    val type: String? = null,
    val platform: String? = null
)

/**
 * 配置内容
 */
data class ConfigContent(
    val content: String? = null
)

/**
 * 依赖日志
 */
data class DependenceLog(
    val title: String? = null,
    val log: String? = null,
    val type: String? = null,
    val status: Int? = null,
    val time: String? = null
)

/**
 * 配置文件列表项
 */
data class ConfigFileInfo(
    val title: String = "",
    val value: String = ""
)

/**
 * 脚本更新请求 (PUT /api/scripts)
 * 青龙 API 要求使用 filename 字段而非 title
 */
data class ScriptUpdateRequest(
    val filename: String,
    val path: String? = null,
    val content: String
)

/**
 * 脚本详情查询响应
 */
data class ScriptDetailResponse(
    val content: String? = null,
    val title: String? = null,
    val path: String? = null
)

/**
 * 任务日志文件信息 (GET /api/crons/:id/logs)
 * time 使用 Double 因为 Node.js birthtimeMs 可能含小数
 */
data class LogFileInfo(
    val filename: String = "",
    val directory: String = "",
    val time: Double = 0.0
)

/**
 * 日志目录树节点 (GET /api/logs 返回的 IFile 结构)
 * 匹配 Q1 的 readDirs 返回格式
 */
data class LogTreeFile(
    val title: String = "",
    val type: String = "",          // "directory" 或 "file"
    val parent: String = "",
    val createTime: Long = 0,
    val size: Long = 0,
    val children: List<LogTreeFile>? = null
) {
    val isDirectory: Boolean get() = type == "directory"
}

/**
 * 订阅管理
 */
data class Subscription(
    val _id: String? = null,
    val id: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val url: String? = null,
    val schedule: String? = null,
    val whitelist: String? = null,
    val blacklist: String? = null,
    val dependences: String? = null,
    val branch: String? = null,
    val extensions: String? = null,
    val alias: String? = null,
    val command: String? = null,
    val subBefore: String? = null,
    val subAfter: String? = null,
    val proxy: String? = null,
    val autoAddCron: Int = 0,
    val autoDelCron: Int = 0,
    @SerializedName("is_disabled")
    val isDisabled: Int = 0,
    val status: Int? = null,
    val pid: Int? = null,
    val log_path: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val stateCode: Int get() = when {
        isDisabled == 1 -> 2
        status == 1 -> 0
        status == 3 -> 3
        else -> 1
    }
    val stateLabel: String get() = when (stateCode) {
        0 -> "运行中"
        1 -> "空闲"
        2 -> "已禁用"
        3 -> "队列中"
        else -> "未知"
    }
}

/**
 * 任务日志详情响应 (GET /api/crons/:id/log)
 * 青龙 API 返回 data + logStatus 两个字段
 */
data class LogDetailResponse(
    val code: Int = 0,
    val data: String? = null,
    val logStatus: String? = null
)
