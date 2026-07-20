package me.doujiang.app.ui.screens.subscriptions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.model.Subscription
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SubsUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Subscription? = null
)

class SubscriptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(SubsUiState())
    val uiState: StateFlow<SubsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getSubscriptions().onSuccess { list ->
                _uiState.value = _uiState.value.copy(subscriptions = list, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun showAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = true) }
    fun hideAddDialog() { _uiState.value = _uiState.value.copy(showAddDialog = false) }
    fun showEditDialog(sub: Subscription) { _uiState.value = _uiState.value.copy(showEditDialog = sub) }
    fun hideEditDialog() { _uiState.value = _uiState.value.copy(showEditDialog = null) }

    fun addSubscription(name: String, url: String, type: String, schedule: String) {
        viewModelScope.launch {
            repository.addSubscription(mapOf(
                "name" to name, "url" to url, "type" to type, "schedule" to schedule
            )).onSuccess {
                hideAddDialog(); load()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "添加失败: ${e.message}")
            }
        }
    }

    fun updateSubscription(id: Int, name: String, url: String, type: String, schedule: String) {
        viewModelScope.launch {
            repository.updateSubscription(mapOf(
                "id" to id.toString(), "name" to name, "url" to url, "type" to type, "schedule" to schedule
            )).onSuccess {
                hideEditDialog(); load()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "更新失败: ${e.message}")
            }
        }
    }

    fun runSub(id: Int) { singleOp(listOf(id), "运行") { repository.runSubscriptions(it) } }
    fun stopSub(id: Int) { singleOp(listOf(id), "停止") { repository.stopSubscriptions(it) } }
    fun enableSub(id: Int) { singleOp(listOf(id), "启用") { repository.enableSubscriptions(it) } }
    fun disableSub(id: Int) { singleOp(listOf(id), "禁用") { repository.disableSubscriptions(it) } }
    fun deleteSub(id: Int) { singleOp(listOf(id), "删除") { repository.deleteSubscriptions(it) } }

    private fun singleOp(ids: List<Int>, label: String, op: suspend (List<Int>) -> Result<Any>) {
        viewModelScope.launch {
            op(ids).onSuccess {
                _uiState.value = _uiState.value.copy(error = "$label 成功")
                load()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = "$label 失败: ${e.message ?: "未知错误"}")
            }
        }
    }
}
