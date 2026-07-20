package me.doujiang.app.data.api

import me.doujiang.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 青龙面板 API 接口定义
 */
interface QingLongApi {

    // ==================== 系统 ====================

    @GET("api/system")
    suspend fun getSystemInfo(): Response<QingLongResponse<SystemInfo>>

    // ==================== 用户/登录 ====================

    @PUT("api/user/init")
    suspend fun initAccount(@Body body: Map<String, String>): Response<QingLongResponse<Any>>

    @POST("api/user/login")
    suspend fun login(@Body body: LoginRequest): Response<QingLongResponse<LoginData>>

    @PUT("api/user/two-factor/login")
    suspend fun twoFactorLogin(@Body body: TwoFactorRequest): Response<QingLongResponse<LoginData>>

    @PUT("api/user")
    suspend fun updateAccount(@Body body: Map<String, String>): Response<QingLongResponse<Any>>

    @GET("api/user/login-log")
    suspend fun getLoginLogs(): Response<QingLongResponse<List<LoginLog>>>

    // ==================== 定时任务 (crons) ====================

    @GET("api/crons")
    suspend fun getTasks(): Response<QingLongResponse<CronListData>>

    @POST("api/crons")
    suspend fun addTask(@Body body: TaskRequest): Response<QingLongResponse<CronTask>>

    @PUT("api/crons")
    suspend fun updateTask(@Body body: TaskRequest): Response<QingLongResponse<CronTask>>

    @HTTP(method = "DELETE", path = "api/crons", hasBody = true)
    suspend fun deleteTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/run")
    suspend fun runTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/stop")
    suspend fun stopTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/enable")
    suspend fun enableTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/disable")
    suspend fun disableTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/pin")
    suspend fun pinTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/crons/unpin")
    suspend fun unpinTasks(@Body body: List<Int>): Response<QingLongResponse<Any>>

    // ==================== 任务日志 ====================

    @GET("api/crons/{id}/log")
    suspend fun getCronLog(@Path("id") id: Int): Response<LogDetailResponse>

    @GET("api/crons/{id}/logs")
    suspend fun getCronLogs(@Path("id") id: Int): Response<QingLongResponse<List<LogFileInfo>>>

    @GET("api/logs")
    suspend fun getLogTree(): Response<QingLongResponse<List<LogTreeFile>>>

    @GET("api/logs/detail")
    suspend fun getLogFileDetail(
        @Query("path") path: String,
        @Query("file") file: String
    ): Response<LogDetailResponse>

    // ==================== 环境变量 (envs) ====================

    @GET("api/envs")
    suspend fun getEnvironments(): Response<QingLongResponse<List<Environment>>>

    @POST("api/envs")
    suspend fun addEnvironments(@Body body: EnvRequest): Response<QingLongResponse<List<Environment>>>

    @POST("api/envs")
    suspend fun addEnvironmentsRaw(@Body body: okhttp3.RequestBody): Response<QingLongResponse<List<Environment>>>

    @PUT("api/envs")
    suspend fun updateEnvironment(@Body body: Environment): Response<QingLongResponse<Environment>>

    @PUT("api/envs")
    suspend fun updateEnvironmentRaw(@Body body: okhttp3.RequestBody): Response<QingLongResponse<Environment>>

    @HTTP(method = "DELETE", path = "api/envs", hasBody = true)
    suspend fun deleteEnvironments(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/envs/enable")
    suspend fun enableEnvironments(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @PUT("api/envs/disable")
    suspend fun disableEnvironments(@Body body: List<Int>): Response<QingLongResponse<Any>>

    // ==================== 依赖 (dependencies) ====================

    @GET("api/dependencies")
    suspend fun getDependencies(
        @Query("type") type: String? = null
    ): Response<QingLongResponse<List<Dependency>>>

    @POST("api/dependencies")
    suspend fun addDependencies(@Body body: List<Dependency>): Response<QingLongResponse<Any>>

    @POST("api/dependencies")
    suspend fun addDependenciesRaw(@Body body: okhttp3.RequestBody): Response<QingLongResponse<List<Dependency>>>

    @PUT("api/dependencies/reinstall")
    suspend fun reinstallDependencies(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @HTTP(method = "DELETE", path = "api/dependencies", hasBody = true)
    suspend fun deleteDependencies(@Body body: List<Int>): Response<QingLongResponse<Any>>

    @GET("api/dependencies/{id}")
    suspend fun getDependencyDetail(@Path("id") id: Int): Response<QingLongResponse<Dependency>>

    // ==================== 脚本 (scripts) ====================

    @GET("api/scripts")
    suspend fun getScripts(
        @Query("path") path: String? = null
    ): Response<QingLongResponse<List<ScriptFile>>>

    @GET("api/scripts")
    suspend fun getScriptFiles(@Query("path") path: String? = null): Response<QingLongResponse<List<ScriptFile>>>

    @GET("api/scripts/detail")
    suspend fun getScriptDetail(
        @Query("path") path: String? = null,
        @Query("file") file: String
    ): Response<QingLongResponse<String>>

    @Multipart
    @POST("api/scripts")
    suspend fun uploadScript(
        @Part("filename") filename: okhttp3.RequestBody,
        @Part("content") content: okhttp3.RequestBody,
        @Part("path") path: okhttp3.RequestBody? = null
    ): Response<QingLongResponse<Any>>

    @PUT("api/scripts")
    suspend fun updateScript(@Body body: ScriptUpdateRequest): Response<QingLongResponse<Any>>

    @HTTP(method = "DELETE", path = "api/scripts", hasBody = true)
    suspend fun deleteScript(@Body body: Map<String, String>): Response<QingLongResponse<Any>>

    // ==================== 配置文件 ====================

    @GET("api/configs/detail")
    suspend fun getConfig(
        @Query("path") path: String = "config.sh"
    ): Response<QingLongResponse<String>>

    @GET("api/configs/files")
    suspend fun getConfigFiles(): Response<QingLongResponse<List<ConfigFileInfo>>>

    @POST("api/configs/save")
    suspend fun saveConfig(@Body body: Map<String, String>): Response<QingLongResponse<Any>>

    // ==================== 系统配置 ====================

    @GET("api/system/config")
    suspend fun getSystemConfig(): Response<QingLongResponse<SystemConfig>>

    @GET("api/system/config")
    suspend fun checkToken(
        @Header("Authorization") token: String
    ): Response<QingLongResponse<SystemConfig>>

    // ==================== 订阅管理 ====================

    @GET("api/subscriptions")
    suspend fun getSubscriptions(): Response<QingLongResponse<List<Subscription>>>

    @POST("api/subscriptions")
    suspend fun addSubscription(@Body body: Map<String, String?>): Response<QingLongResponse<Subscription>>

    @PUT("api/subscriptions")
    suspend fun updateSubscription(@Body body: Map<String, String?>): Response<QingLongResponse<Subscription>>

    @HTTP(method = "DELETE", path = "api/subscriptions", hasBody = true)
    suspend fun deleteSubscriptions(@Body body: Map<String, List<Int>>): Response<QingLongResponse<Any>>

    @PUT("api/subscriptions/run")
    suspend fun runSubscriptions(@Body body: Map<String, List<Int>>): Response<QingLongResponse<Any>>

    @PUT("api/subscriptions/stop")
    suspend fun stopSubscriptions(@Body body: Map<String, List<Int>>): Response<QingLongResponse<Any>>

    @PUT("api/subscriptions/enabled")
    suspend fun enableSubscriptions(@Body body: Map<String, List<Int>>): Response<QingLongResponse<Any>>

    @PUT("api/subscriptions/disabled")
    suspend fun disableSubscriptions(@Body body: Map<String, List<Int>>): Response<QingLongResponse<Any>>

    // ==================== 通用文件 ====================

    @GET
    suspend fun getFileContent(@Url url: String): Response<QingLongResponse<ScriptFile>>
}
