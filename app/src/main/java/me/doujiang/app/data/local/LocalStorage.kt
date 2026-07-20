package me.doujiang.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qinglong_prefs")

data class AccountEntry(
    val server: String,
    val token: String,
    val username: String
)

class LocalStorage(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_ACCOUNTS = stringPreferencesKey("accounts")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[KEY_SERVER_URL] ?: "" }
    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun saveLoginInfo(serverUrl: String, token: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_TOKEN] = token
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun getServerUrl(): String = serverUrl.first()
    suspend fun getToken(): String = token.first()
    suspend fun getUsername(): String = username.first()
    suspend fun isLoggedIn(): Boolean = getToken().isNotEmpty()

    suspend fun clearLoginInfo() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SERVER_URL)
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USERNAME)
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun saveDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    // 多账号支持
    suspend fun getAccounts(): List<AccountEntry> {
        val json = context.dataStore.data.first()[KEY_ACCOUNTS] ?: "[]"
        return try {
            Gson().fromJson(json, object : TypeToken<List<AccountEntry>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun switchAccount(entry: AccountEntry) {
        // 把当前账号存回列表
        val current = AccountEntry(getServerUrl(), getToken(), getUsername())
        val accounts = getAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.server == entry.server && it.username == entry.username }

        // 保存当前账号（替换或新增）
        if (current.server.isNotEmpty()) {
            val curIdx = accounts.indexOfFirst { it.server == current.server && it.username == current.username }
            if (curIdx >= 0) accounts[curIdx] = current
            else accounts.add(current)
        }

        // 切换到目标账号
        saveLoginInfo(entry.server, entry.token, entry.username)

        // 保存更新后的列表（去掉当前被选中的）
        val updatedList = accounts.filter { !(it.server == entry.server && it.username == entry.username) }
        context.dataStore.edit { it[KEY_ACCOUNTS] = Gson().toJson(updatedList) }
    }

    suspend fun saveCurrentAccount() {
        val current = AccountEntry(getServerUrl(), getToken(), getUsername())
        if (current.server.isEmpty()) return
        val accounts = getAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.server == current.server && it.username == current.username }
        if (idx >= 0) accounts[idx] = current
        else accounts.add(current)
        context.dataStore.edit { it[KEY_ACCOUNTS] = Gson().toJson(accounts) }
    }
}
