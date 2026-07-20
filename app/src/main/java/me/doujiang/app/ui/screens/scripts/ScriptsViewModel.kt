package me.doujiang.app.ui.screens.scripts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.ScriptFile
import me.doujiang.app.data.model.ScriptUpdateRequest
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ScriptsUiState(
    val files: List<ScriptFile> = emptyList(),
    val currentPath: String = "",
    val currentContent: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isViewingContent: Boolean = false,
    val isEditing: Boolean = false,
    val editContent: String = "",
    val currentFile: ScriptFile? = null
)

class ScriptsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    init { loadScripts() }

    fun loadScripts(path: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getScripts(path).onSuccess { files ->
                _uiState.value = _uiState.value.copy(files = files, currentPath = path ?: "", isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun enterDir(dir: ScriptFile) = loadScripts(dir.path)
    fun goBack() {
        val p = _uiState.value.currentPath
        if (p.isEmpty()) return
        val parent = p.substringBeforeLast("/", "")
        loadScripts(parent.ifEmpty { null })
    }
    fun viewFile(file: ScriptFile) {
        if (file.content != null) {
            _uiState.value = _uiState.value.copy(
                isViewingContent = true,
                currentFile = file,
                currentContent = file.content
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoading = true)
            viewModelScope.launch {
                repository.getScriptDetail(path = null, file = file.title)
                    .onSuccess { content ->
                        _uiState.value = _uiState.value.copy(
                            isViewingContent = true, currentFile = file,
                            currentContent = content.ifEmpty { "（空文件）" }, isLoading = false
                        )
                    }
                    .onFailure { e1 ->
                        // 方式2: 用 OkHttp 直连 GET 文件内容
                        try {
                            val serverUrl = repository.getServerUrl()
                            val token = repository.getToken()
                            val client = OkHttpClient.Builder()
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .addInterceptor { chain ->
                                    chain.proceed(chain.request().newBuilder()
                                        .addHeader("Authorization", "Bearer $token").build())
                                }
                                .build()
                            val encodedFile = URLEncoder.encode(file.title, "UTF-8")
                            // 先尝试 scripts 接口获取内容
                            val listUrl = "${serverUrl.trimEnd('/')}/api/scripts?path=$encodedFile"
                            val listBody = withContext(Dispatchers.IO) {
                                client.newCall(Request.Builder().url(listUrl).get().build()).execute()
                                    .body?.string() ?: "{}"
                            }
                            val listJson = JsonParser.parseString(listBody).asJsonObject
                            if (listJson.get("code")?.asInt == 200) {
                                val data = listJson.getAsJsonArray("data")
                                if (data != null && data.size() > 0) {
                                    val item = data[0].asJsonObject
                                    val c = item.get("content")?.asString
                                    if (!c.isNullOrBlank()) {
                                        _uiState.value = _uiState.value.copy(
                                            isViewingContent = true, currentFile = file,
                                            currentContent = c, isLoading = false
                                        )
                                        return@launch
                                    }
                                }
                            }
                            // scripts 接口没内容，直接 GET 文件路径
                            val rawUrl = "${serverUrl.trimEnd('/')}/api/scripts/$encodedFile"
                            val rawBody = withContext(Dispatchers.IO) {
                                client.newCall(Request.Builder().url(rawUrl).get().build()).execute()
                                    .body?.string() ?: "{}"
                            }
                            val rawJson = JsonParser.parseString(rawBody)
                            if (rawJson.isJsonObject) {
                                val data = rawJson.asJsonObject.get("data")
                                if (data != null && data.isJsonPrimitive) {
                                    _uiState.value = _uiState.value.copy(
                                        isViewingContent = true, currentFile = file,
                                        currentContent = data.asString.ifEmpty { "（空文件）" },
                                        isLoading = false
                                    )
                                    return@launch
                                }
                            }
                            // 兜底: 把原始响应当内容
                            _uiState.value = _uiState.value.copy(
                                isViewingContent = true, currentFile = file,
                                currentContent = rawBody.take(4096),
                                isLoading = false
                            )
                        } catch (e2: Exception) {
                            _uiState.value = _uiState.value.copy(
                                isViewingContent = true, currentFile = file,
                                currentContent = "加载失败:\n方式1: ${e1.message}\n方式2: ${e2.message}",
                                isLoading = false
                            )
                        }
                    }
            }
        }
    }
    fun deleteScript(file: ScriptFile) {
        viewModelScope.launch {
            repository.deleteScript(
                filename = file.title,
                path = file.parent ?: ""
            ).onSuccess {
                loadScripts(_uiState.value.currentPath.ifEmpty { null })
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "删除失败")
            }
        }
    }

    fun uploadScript(fileName: String, content: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.uploadScript(fileName = fileName, content = content).onSuccess {
                loadScripts(_uiState.value.currentPath.ifEmpty { null })
                onResult(true, fileName)
            }.onFailure { e ->
                onResult(false, e.message ?: "上传失败")
            }
        }
    }

    fun closeContent() { _uiState.value = _uiState.value.copy(isViewingContent = false, currentFile = null, currentContent = null, isEditing = false, editContent = "") }

    fun onEditContentChanged(newContent: String) {
        _uiState.value = _uiState.value.copy(editContent = newContent)
    }

    fun startEditing() {
        val content = _uiState.value.currentContent
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editContent = content?.let { if (it == "（空文件）") "" else it } ?: ""
        )
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(isEditing = false, editContent = "")
    }

    fun saveScript() {
        val file = _uiState.value.currentFile ?: return
        val content = _uiState.value.editContent
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.updateScript(ScriptUpdateRequest(
                filename = file.title,
                path = file.parent ?: "",
                content = content
            )).onSuccess {
                _uiState.value = _uiState.value.copy(
                    currentContent = content,
                    isEditing = false,
                    editContent = "",
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "保存失败: ${e.message ?: "未知错误"}"
                )
            }
        }
    }
}
