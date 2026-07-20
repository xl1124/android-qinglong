package me.doujiang.app.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import me.doujiang.app.data.local.LocalStorage
import me.doujiang.app.data.repository.QingLongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTwoFactorRequired: Boolean = false,
    val twoFactorCode: String = "",
    val loginSuccess: Boolean = false,
    val sessionChecking: Boolean = true
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QingLongRepository(LocalStorage(application))

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) { _uiState.value = _uiState.value.copy(serverUrl = url, error = null) }
    fun updateUsername(username: String) { _uiState.value = _uiState.value.copy(username = username, error = null) }
    fun updatePassword(password: String) { _uiState.value = _uiState.value.copy(password = password, error = null) }
    fun updateTwoFactorCode(code: String) { _uiState.value = _uiState.value.copy(twoFactorCode = code, error = null) }

    fun login() {
        val s = _uiState.value
        if (s.serverUrl.isBlank()) { _uiState.value = s.copy(error = "请输入服务器地址"); return }
        if (s.username.isBlank()) { _uiState.value = s.copy(error = "请输入用户名"); return }
        if (s.password.isBlank()) { _uiState.value = s.copy(error = "请输入密码"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.login(s.serverUrl, s.username, s.password)
                    .onSuccess { data ->
                        if (data.isTwoFactor == "1") {
                            _uiState.value = _uiState.value.copy(isLoading = false, isTwoFactorRequired = true)
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "登录失败")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "网络错误: ${e.message ?: "未知"}")
            }
        }
    }

    fun twoFactorLogin() {
        val s = _uiState.value
        if (s.twoFactorCode.isBlank()) { _uiState.value = s.copy(error = "请输入验证码"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.twoFactorLogin(s.serverUrl, s.username, s.password, s.twoFactorCode)
                    .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true) }
                    .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "认证失败") }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "网络错误: ${e.message ?: "未知"}")
            }
        }
    }

    fun checkSession() {
        viewModelScope.launch {
            val loggedIn = repository.checkLoggedIn()
            if (loggedIn) {
                _uiState.value = _uiState.value.copy(loginSuccess = true, sessionChecking = false)
            } else {
                val url = repository.getServerUrl()
                _uiState.value = _uiState.value.copy(serverUrl = url, sessionChecking = false)
            }
        }
    }
}
