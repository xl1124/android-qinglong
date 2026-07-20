package me.doujiang.app.data.api

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 动态 baseUrl 的 Retrofit 客户端
 * 青龙面板地址由用户在登录时指定
 */
class RetrofitClient {

    private var baseUrl: String = ""
    private var retrofit: Retrofit? = null
    private var api: QingLongApi? = null

    private val lenientGson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * 构建规范化的 baseUrl
     */
    fun buildBaseUrl(input: String): String {
        var url = input.trim()

        // 移除尾部斜杠
        url = url.trimEnd('/')

        // 如果没有协议前缀，默认添加 http://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        return "$url/"
    }

    /**
     * 获取或创建 API 实例
     */
    fun getApi(serverUrl: String): QingLongApi {
        val normalizedUrl = buildBaseUrl(serverUrl)
        if (normalizedUrl != baseUrl || api == null) {
            baseUrl = normalizedUrl
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(lenientGson))
                .build()
            api = retrofit!!.create(QingLongApi::class.java)
        }
        return api!!
    }

    /**
     * 设置认证 Token 拦截器
     */
    fun getAuthenticatedClient(token: String): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * 创建带认证的 API 实例
     */
    fun getAuthenticatedApi(serverUrl: String, token: String): QingLongApi {
        val normalizedUrl = buildBaseUrl(serverUrl)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(getAuthenticatedClient(token))
            .addConverterFactory(GsonConverterFactory.create(lenientGson))
            .build()
            .create(QingLongApi::class.java)
    }

    fun getBaseUrl(): String = baseUrl

    companion object {
        @Volatile
        private var instance: RetrofitClient? = null

        fun getInstance(): RetrofitClient {
            return instance ?: synchronized(this) {
                instance ?: RetrofitClient().also { instance = it }
            }
        }
    }
}
