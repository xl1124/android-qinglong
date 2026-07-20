package me.doujiang.app.data.repository

import me.doujiang.app.data.api.QingLongApi
import me.doujiang.app.data.api.RetrofitClient
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 所有仓库的集中入口
 */
class QingLongRepository(private val localStorage: LocalStorage) {

    private suspend fun getAuthenticatedApi(): QingLongApi {
        val serverUrl = localStorage.getServerUrl()
        val token = localStorage.getToken()
        return RetrofitClient.getInstance().getAuthenticatedApi(serverUrl, token)
    }

    // ==================== 认证 ====================

    suspend fun login(serverUrl: String, username: String, password: String): Result<LoginData> {
        return try {
            val api = RetrofitClient.getInstance().getApi(serverUrl)
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                localStorage.saveLoginInfo(serverUrl, data.token ?: "", username)
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun twoFactorLogin(serverUrl: String, username: String, password: String, code: String): Result<LoginData> {
        return try {
            val api = RetrofitClient.getInstance().getApi(serverUrl)
            val response = api.twoFactorLogin(TwoFactorRequest(username, password, code))
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                localStorage.saveLoginInfo(serverUrl, data.token ?: "", username)
                Result.success(data)
            } else {
                Result.failure(Exception(response.body()?.message ?: "双因素认证失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkLoggedIn(): Boolean = localStorage.isLoggedIn()

    suspend fun getServerUrl(): String = localStorage.getServerUrl()

    suspend fun getToken(): String = localStorage.getToken()

    suspend fun getUsername(): String = localStorage.getUsername()

    suspend fun logout() {
        localStorage.clearLoginInfo()
    }

    // ==================== 系统 ====================

    suspend fun getSystemInfo(): Result<SystemInfo> = apiCall {
        getAuthenticatedApi().getSystemInfo()
    }

    // ==================== 定时任务 ====================

    suspend fun getTasks(): Result<List<CronTask>> {
        return apiCall { getAuthenticatedApi().getTasks() }.map { it.data }
    }

    suspend fun addTask(request: TaskRequest): Result<CronTask> = apiCall {
        getAuthenticatedApi().addTask(request)
    }

    suspend fun updateTask(request: TaskRequest): Result<CronTask> = apiCall {
        getAuthenticatedApi().updateTask(request)
    }

    private fun toIntList(ids: List<String>): List<Int> =
        ids.mapNotNull { it.toIntOrNull() }

    suspend fun deleteTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().deleteTasks(toIntList(ids))
    }

    suspend fun runTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().runTasks(toIntList(ids))
    }

    suspend fun stopTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().stopTasks(toIntList(ids))
    }

    suspend fun enableTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().enableTasks(toIntList(ids))
    }

    suspend fun disableTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().disableTasks(toIntList(ids))
    }

    suspend fun pinTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().pinTasks(toIntList(ids))
    }

    suspend fun unpinTasks(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().unpinTasks(toIntList(ids))
    }

    // ==================== 任务日志 ====================

    data class CronLogResult(
        val content: String,
        val logStatus: String?
    )

    suspend fun getCronLog(id: Int): Result<CronLogResult> {
        return try {
            val api = getAuthenticatedApi()
            val response = api.getCronLog(id)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(CronLogResult(body.data ?: "", body.logStatus))
            } else {
                Result.failure(Exception("获取日志失败 (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCronLogs(id: Int): Result<List<LogFileInfo>> = apiCall {
        getAuthenticatedApi().getCronLogs(id)
    }

    suspend fun getLogTree(): Result<List<LogTreeFile>> = apiCall {
        getAuthenticatedApi().getLogTree()
    }.map { it ?: emptyList() }

    suspend fun getLogFileDetail(directory: String, filename: String): Result<CronLogResult> {
        return try {
            val api = getAuthenticatedApi()
            val response = api.getLogFileDetail(directory, filename)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(CronLogResult(body.data ?: "", body.logStatus))
            } else {
                Result.failure(Exception("获取日志详情失败 (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 环境变量 ====================

    suspend fun getEnvironments(): Result<List<Environment>> = apiCall {
        getAuthenticatedApi().getEnvironments()
    }

    suspend fun addEnvironments(env: EnvRequest): Result<List<Environment>> {
        return try {
            // 与 Q2 完全一致: [{"name":"...","remarks":"...","value":"..."}]
            val arr = com.google.gson.JsonArray()
            val obj = com.google.gson.JsonObject()
            obj.addProperty("name", env.name)
            obj.addProperty("remarks", env.remarks ?: "")
            obj.addProperty("value", env.value)
            arr.add(obj)
            val body = arr.toString().toRequestBody("application/json".toMediaType())
            val api = getAuthenticatedApi()
            val response = api.addEnvironmentsRaw(body)
            if (response.isSuccessful && response.body()?.code == 200) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val errMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("HTTP ${response.code()}: $errMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEnvironment(env: Environment): Result<Environment> {
        return try {
            // 与 Q2 完全一致: {"id":<num>,"name":"...","remarks":"...","value":"..."}
            val obj = com.google.gson.JsonObject()
            if (env.id != null) {
                obj.addProperty("id", env.id)
            } else if (env._id != null) {
                obj.addProperty("_id", env._id)
            }
            obj.addProperty("name", env.name)
            obj.addProperty("remarks", env.remarks ?: "")
            obj.addProperty("value", env.value)
            val body = obj.toString().toRequestBody("application/json".toMediaType())
            val response = getAuthenticatedApi().updateEnvironmentRaw(body)
            if (response.isSuccessful && response.body()?.code == 200) {
                Result.success(response.body()?.data ?: env)
            } else {
                val errMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("HTTP ${response.code()}: $errMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEnvironments(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().deleteEnvironments(toIntList(ids))
    }

    suspend fun enableEnvironments(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().enableEnvironments(toIntList(ids))
    }

    suspend fun disableEnvironments(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().disableEnvironments(toIntList(ids))
    }

    // ==================== 依赖 ====================

    suspend fun getDependencies(type: String? = null): Result<List<Dependency>> = apiCall {
        getAuthenticatedApi().getDependencies(type)
    }

    suspend fun addDependencies(deps: List<Dependency>): Result<List<Dependency>> {
        return try {
            // Q1 expects: [{"name":"...","type":0,"remark":"..."}]
            val arr = com.google.gson.JsonArray()
            for (dep in deps) {
                val obj = com.google.gson.JsonObject()
                obj.addProperty("name", dep.name)
                obj.addProperty("type", dep.type)
                obj.addProperty("remark", dep.remark ?: "")
                arr.add(obj)
            }
            val body = arr.toString().toRequestBody("application/json".toMediaType())
            val response = getAuthenticatedApi().addDependenciesRaw(body)
            if (response.isSuccessful && response.body()?.code == 200) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val errMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("HTTP ${response.code()}: $errMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reinstallDependencies(ids: List<String>): Result<Any> = apiCall {
        getAuthenticatedApi().reinstallDependencies(toIntList(ids))
    }

    suspend fun deleteDependencies(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().deleteDependencies(ids)
    }

    suspend fun getDependencyDetail(id: Int): Result<Dependency> = apiCall {
        getAuthenticatedApi().getDependencyDetail(id)
    }

    // ==================== 脚本 ====================

    suspend fun uploadScript(fileName: String, content: String, path: String? = null): Result<Any> {
        return try {
            val api = getAuthenticatedApi()
            val filenamePart = fileName.toRequestBody("text/plain".toMediaType())
            val contentPart = content.toRequestBody("text/plain".toMediaType())
            val pathPart = path?.toRequestBody("text/plain".toMediaType())
            val response = api.uploadScript(filenamePart, contentPart, pathPart)
            if (response.isSuccessful && response.body()?.code == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "上传失败 (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScripts(path: String? = null): Result<List<ScriptFile>> = apiCall {
        getAuthenticatedApi().getScriptFiles(path)
    }

    suspend fun updateScript(request: ScriptUpdateRequest): Result<Any> = apiCall {
        getAuthenticatedApi().updateScript(request)
    }

    suspend fun getScriptDetail(path: String?, file: String): Result<String> = apiCall {
        getAuthenticatedApi().getScriptDetail(path, file)
    }

    suspend fun deleteScript(filename: String, path: String = ""): Result<Any> = apiCall {
        getAuthenticatedApi().deleteScript(mapOf("filename" to filename, "path" to path))
    }

    // ==================== 配置文件 ====================

    suspend fun getConfigDetail(path: String = "config.sh"): Result<String> = apiCall {
        getAuthenticatedApi().getConfig(path)
    }

    suspend fun getConfigFiles(): Result<List<ConfigFileInfo>> = apiCall {
        getAuthenticatedApi().getConfigFiles()
    }

    suspend fun saveConfig(name: String, content: String): Result<Any> = apiCall {
        getAuthenticatedApi().saveConfig(mapOf("name" to name, "content" to content))
    }

    // ==================== 系统配置 ====================

    suspend fun getSystemConfig(): Result<SystemConfig> = apiCall {
        getAuthenticatedApi().getSystemConfig()
    }

    // ==================== 订阅管理 ====================

    suspend fun getSubscriptions(): Result<List<Subscription>> = apiCall {
        getAuthenticatedApi().getSubscriptions()
    }

    suspend fun addSubscription(body: Map<String, String?>): Result<Subscription> = apiCall {
        getAuthenticatedApi().addSubscription(body)
    }

    suspend fun updateSubscription(body: Map<String, String?>): Result<Subscription> = apiCall {
        getAuthenticatedApi().updateSubscription(body)
    }

    suspend fun deleteSubscriptions(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().deleteSubscriptions(mapOf("ids" to ids))
    }

    suspend fun runSubscriptions(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().runSubscriptions(mapOf("ids" to ids))
    }

    suspend fun stopSubscriptions(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().stopSubscriptions(mapOf("ids" to ids))
    }

    suspend fun enableSubscriptions(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().enableSubscriptions(mapOf("ids" to ids))
    }

    suspend fun disableSubscriptions(ids: List<Int>): Result<Any> = apiCall {
        getAuthenticatedApi().disableSubscriptions(mapOf("ids" to ids))
    }

    // ==================== 登录日志 ====================

    suspend fun getLoginLogs(): Result<List<LoginLog>> = apiCall {
        getAuthenticatedApi().getLoginLogs()
    }

    // ==================== 工具 ====================

    /**
     * 解析 export 格式的环境变量导入
     * export KEY="value"
     */
    fun parseExportFormat(text: String, defaultRemark: String = ""): List<EnvRequest> {
        val pattern = Regex("export\\s+(\\w+)=\"([^\"]+)\"")
        return pattern.findAll(text).map { match ->
            EnvRequest(
                name = match.groupValues[1],
                value = match.groupValues[2],
                remarks = defaultRemark
            )
        }.toList()
    }

    /**
     * 通用 API 调用包装
     */
    private suspend fun <T> apiCall(call: suspend () -> retrofit2.Response<QingLongResponse<T>>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    Result.success(body.data)
                } else if (body?.code == 200 && body.data == null) {
                    @Suppress("UNCHECKED_CAST")
                    Result.success(body.message as? T ?: true as T)
                } else {
                    Result.failure(Exception(body?.message ?: "请求失败 (${body?.code})"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
