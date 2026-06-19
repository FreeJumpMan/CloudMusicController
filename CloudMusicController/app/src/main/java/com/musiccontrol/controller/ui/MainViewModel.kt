package com.musiccontrol.controller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musiccontrol.controller.network.MusicApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 界面状态
 */
data class UiState(
    val host: String = "192.168.10.6",     // 服务器 IP
    val port: Int = 8888,                 // 服务器端口
    val connected: Boolean = false,       // 是否已连接
    val loading: Boolean = false,         // 正在连接/发送中
    val error: String? = null,            // 错误提示
)

/**
 * 主界面 ViewModel — 管理状态和业务逻辑
 */
class MainViewModel : ViewModel() {

    private val api = MusicApi()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun updateHost(host: String) {
        _state.update { it.copy(host = host, connected = false, error = null) }
    }

    fun updatePort(port: Int) {
        _state.update { it.copy(port = port, connected = false, error = null) }
    }

    /**
     * 测试与服务器的连接
     */
    fun checkConnection() {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(loading = true, error = null) }

            val ok = api.checkConnection(s.host, s.port)
            _state.update {
                it.copy(
                    connected = ok,
                    loading = false,
                    error = if (!ok) "无法连接到服务器，请检查 IP 和端口" else null
                )
            }
        }
    }

    /**
     * 发送控制指令（播放、暂停、切歌等）
     */
    fun sendCommand(command: String) {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(loading = true, error = null) }

            val result = api.sendCommand(s.host, s.port, command)
            result.onFailure { e ->
                _state.update {
                    it.copy(connected = false, loading = false, error = e.message ?: "请求失败")
                }
            }
            result.onSuccess {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
