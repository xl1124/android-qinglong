package me.doujiang.app.ui.screens.env

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.Environment
import me.doujiang.app.data.model.EnvRequest
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EnvUiState(
    val environments: List<Environment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditEnv: Environment? = null
)

class EnvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(EnvUiState())
    val uiState: StateFlow<EnvUiState> = _uiState.asStateFlow()

    init { loadEnvs() }
    fun loadEnvs() { viewModelScope.launch { _uiState.value = _uiState.value.copy(isLoading = true, error = null); repository.getEnvironments().onSuccess { _uiState.value = _uiState.value.copy(environments = it, isLoading = false) }.onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) } } }
    fun toggleSelection(id: String) { val c = _uiState.value.selectedIds.toMutableSet(); if (c.contains(id)) c.remove(id) else c.add(id); _uiState.value = _uiState.value.copy(selectedIds = c, isSelectionMode = c.isNotEmpty()) }
    fun clearSelection() { _uiState.value = _uiState.value.copy(selectedIds = emptySet(), isSelectionMode = false) }
    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showEditEnv(env: Environment) { _uiState.value = _uiState.value.copy(showEditEnv = env) }
    fun hideEditEnv() { _uiState.value = _uiState.value.copy(showEditEnv = null) }

    fun addEnv(name: String, value: String, remarks: String) {
        viewModelScope.launch {
            repository.addEnvironments(EnvRequest(name, value, remarks)).onSuccess {
                hideAddDialog(); loadEnvs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "保存失败")
            }
        }
    }

    fun updateEnv(env: Environment) {
        viewModelScope.launch {
            repository.updateEnvironment(env).onSuccess {
                hideEditEnv(); loadEnvs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "更新失败")
            }
        }
    }

    // 单个操作
    fun enableEnv(env: Environment) {
        val id = env.id ?: return
        viewModelScope.launch {
            repository.enableEnvironments(listOf(id.toString())).onSuccess {
                _uiState.value = _uiState.value.copy(error = "已启用: ${env.name}")
                loadEnvs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "启用失败: ${e.message}")
            }
        }
    }

    fun disableEnv(env: Environment) {
        val id = env.id ?: return
        viewModelScope.launch {
            repository.disableEnvironments(listOf(id.toString())).onSuccess {
                _uiState.value = _uiState.value.copy(error = "已禁用: ${env.name}")
                loadEnvs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "禁用失败: ${e.message}")
            }
        }
    }

    fun deleteEnv(env: Environment) {
        val id = env.id ?: return
        viewModelScope.launch {
            repository.deleteEnvironments(listOf(id.toString())).onSuccess {
                _uiState.value = _uiState.value.copy(error = "已删除: ${env.name}")
                loadEnvs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    // 批量操作
    fun deleteSelected() = batchOp { repository.deleteEnvironments(it) }
    fun enableSelected() = batchOp { repository.enableEnvironments(it) }
    fun disableSelected() = batchOp { repository.disableEnvironments(it) }

    private fun batchOp(op: suspend (List<String>) -> Result<Any>) {
        viewModelScope.launch { op(_uiState.value.selectedIds.toList()).onSuccess { clearSelection(); loadEnvs() } }
    }
}
