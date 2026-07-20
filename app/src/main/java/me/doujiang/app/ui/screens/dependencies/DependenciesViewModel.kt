package me.doujiang.app.ui.screens.dependencies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.Dependency
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DependenciesUiState(
    val dependencies: List<Dependency> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedType: Int = -1,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showAddDialog: Boolean = false,
    val showLogDialog: Dependency? = null,
    val pendingDepLogId: Int? = null,  // 新建依赖后用于轮询日志
    val isLogLoading: Boolean = false,
)

class DependenciesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(DependenciesUiState())
    val uiState: StateFlow<DependenciesUiState> = _uiState.asStateFlow()

    init { loadDependencies() }

    fun loadDependencies(type: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Q1 的 type 参数是名称字符串: "nodejs", "python3", "linux"
            val typeStr = when (type) {
                -1, null -> null
                0 -> "nodejs"
                1 -> "python3"
                2 -> "linux"
                else -> null
            }
            repository.getDependencies(typeStr).onSuccess {
                _uiState.value = _uiState.value.copy(dependencies = it, isLoading = false, selectedType = type ?: -1)
            }.onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun setType(type: Int) = loadDependencies(type)
    fun toggleSelection(id: String) { val c = _uiState.value.selectedIds.toMutableSet(); if (c.contains(id)) c.remove(id) else c.add(id); _uiState.value = _uiState.value.copy(selectedIds = c, isSelectionMode = c.isNotEmpty()) }
    fun clearSelection() { _uiState.value = _uiState.value.copy(selectedIds = emptySet(), isSelectionMode = false) }
    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showLog(dep: Dependency) { _uiState.value = _uiState.value.copy(showLogDialog = dep) }
    fun hideLog() { _uiState.value = _uiState.value.copy(showLogDialog = null) }

    fun addDependency(name: String, type: Int) {
        viewModelScope.launch {
            repository.addDependencies(listOf(Dependency(name = name, type = type))).onSuccess { created ->
                hideAddDialog()
                val depId = created.firstOrNull()?.id
                if (depId != null) {
                    _uiState.value = _uiState.value.copy(
                        showLogDialog = created.first(),
                        pendingDepLogId = depId,
                        isLogLoading = true
                    )
                }
                loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType)
            }
        }
    }

    /**
     * 轮询获取依赖安装日志（新建后由 LaunchedEffect 定时调用）
     */
    fun fetchDepLog() {
        val depId = _uiState.value.pendingDepLogId ?: return
        viewModelScope.launch {
            repository.getDependencyDetail(depId).onSuccess { dep ->
                _uiState.value = _uiState.value.copy(
                    showLogDialog = dep.copy(log = dep.log),
                    isLogLoading = false
                )
                // 安装完成后停止轮询
                if (dep.status == 1 || dep.status == 2 || dep.status >= 4) {
                    _uiState.value = _uiState.value.copy(pendingDepLogId = null)
                }
            }.onFailure { /* 忽略 */ }
        }
    }

    fun setPendingDepLog(id: Int) {
        _uiState.value = _uiState.value.copy(pendingDepLogId = id, isLogLoading = true)
    }

    fun clearPendingDepLog() {
        _uiState.value = _uiState.value.copy(pendingDepLogId = null)
    }

    fun reinstallSingle(dep: Dependency) {
        val id = dep.id ?: return
        viewModelScope.launch {
            repository.reinstallDependencies(listOf(id.toString())).onSuccess {
                _uiState.value = _uiState.value.copy(error = "重新安装: ${dep.title}")
                loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType)
            }
        }
    }

    fun deleteSingle(dep: Dependency) {
        val id = dep.id ?: return
        viewModelScope.launch {
            repository.deleteDependencies(listOf(id)).onSuccess {
                _uiState.value = _uiState.value.copy(error = "已删除: ${dep.title}")
                loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "删除失败($id): ${e.message}")
                loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType)
            }
        }
    }

    fun deleteSelected() { viewModelScope.launch {
        val ids = _uiState.value.selectedIds.mapNotNull { it.toIntOrNull() }
        repository.deleteDependencies(ids).onSuccess { clearSelection(); loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType) }
    } }

    fun reinstallSelected() { viewModelScope.launch {
        val ids = _uiState.value.selectedIds.mapNotNull { it.toIntOrNull() }
        repository.reinstallDependencies(ids.map { it.toString() }).onSuccess { clearSelection(); loadDependencies(if (_uiState.value.selectedType < 0) null else _uiState.value.selectedType) }
    } }
}
