package me.doujiang.app.ui.screens.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.ConfigFileInfo
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigUiState(
    val configFiles: List<ConfigFileInfo> = emptyList(),
    val selectedFile: String = "",
    val configContent: String = "",
    val editedContent: String = "",
    val isLoading: Boolean = true,
    val isViewingFile: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showSavedSnackbar: Boolean = false
)

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfigFiles()
    }

    fun loadConfigFiles() {
        viewModelScope.launch {
            _uiState.value = ConfigUiState(isLoading = true)
            repository.getConfigFiles().onSuccess { files ->
                _uiState.value = _uiState.value.copy(
                    configFiles = files,
                    isLoading = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载文件列表失败"
                )
            }
        }
    }

    fun openFile(fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedFile = fileName,
                isViewingFile = true,
                error = null
            )
            repository.getConfigDetail(fileName).onSuccess { content ->
                _uiState.value = _uiState.value.copy(
                    configContent = content,
                    isLoading = false,
                    isEditing = false,
                    editedContent = ""
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isViewingFile = false,
                    error = e.message ?: "加载配置失败"
                )
            }
        }
    }

    fun goBackToList() {
        _uiState.value = _uiState.value.copy(
            isViewingFile = false,
            isEditing = false,
            selectedFile = "",
            configContent = "",
            error = null
        )
    }

    fun startEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = true,
            editedContent = _uiState.value.configContent
        )
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditing = false,
            editedContent = ""
        )
    }

    fun onEditContentChanged(content: String) {
        _uiState.value = _uiState.value.copy(editedContent = content)
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.saveConfig(
                name = _uiState.value.selectedFile,
                content = _uiState.value.editedContent
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    configContent = _uiState.value.editedContent,
                    isEditing = false,
                    isSaving = false,
                    showSavedSnackbar = true,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败"
                )
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(showSavedSnackbar = false)
    }
}
