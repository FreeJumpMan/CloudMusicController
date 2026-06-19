package com.musiccontrol.controller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Green = Color(0xFF1DB954)
private val Red = Color(0xFFE74C3C)
private val Gray = Color(0xFF333333)
private val DarkBg = Color(0xFF1E1E1E)
private val PageBg = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("网易云遥控器", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (state.connected) {
                            Text(
                                "已连接 · ${state.host}:${state.port}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PageBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = PageBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── 连接状态 ──
            ConnectionStatus(connected = state.connected)

            Spacer(Modifier.height(20.dp))

            // ── 控制面板 ──
            ControlPanel(enabled = state.connected, onCommand = viewModel::sendCommand)
        }
    }

    // ── 设置弹窗 ──
    if (showSettings) {
        SettingsDialog(
            host = state.host,
            port = state.port,
            loading = state.loading,
            connected = state.connected,
            onHostChange = viewModel::updateHost,
            onPortChange = viewModel::updatePort,
            onConnect = viewModel::checkConnection,
            onDismiss = { showSettings = false }
        )
    }
}

// ═══════════════════════════════════════════
//  连接状态指示器
// ═══════════════════════════════════════════

@Composable
fun ConnectionStatus(connected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (connected) Green else Color(0xFF555555))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (connected) "已连接 — 点击按钮控制" else "未连接 — 点右上角 ⚙ 设置",
            fontSize = 13.sp,
            color = if (connected) Color(0xFFAAAAAA) else Color(0xFF666666)
        )
    }
}

// ═══════════════════════════════════════════
//  设置弹窗
// ═══════════════════════════════════════════

@Composable
fun SettingsDialog(
    host: String,
    port: Int,
    loading: Boolean,
    connected: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "服务器设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text("IP 地址", color = Color(0xFF888888)) },
                    placeholder = { Text("192.168.1.x") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Filled.Computer, contentDescription = null, tint = Green)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Green
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = port.toString(),
                        onValueChange = { it.toIntOrNull()?.let(onPortChange) },
                        label = { Text("端口", color = Color(0xFF888888)) },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConnect() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Green
                        )
                    )

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = onConnect,
                        enabled = !loading && host.isNotBlank(),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                if (connected) Icons.Filled.Check else Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (connected) "已连接" else "连接",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 关闭按钮
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭", color = Color(0xFF888888))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  控制面板 — 匹配 Web 页面布局
// ═══════════════════════════════════════════

@Composable
fun ControlPanel(enabled: Boolean, onCommand: (String) -> Unit) {
    val bgColor = if (enabled) DarkBg else DarkBg.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 第1行: 上一曲 | 播放/暂停 | 下一曲 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlButton(
                    label = "上一曲",
                    icon = { Icon(Icons.Filled.SkipPrevious, "上一曲", modifier = Modifier.size(32.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("prev") },
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    label = "播放/暂停",
                    icon = { Icon(Icons.Filled.PlayArrow, "播放/暂停", modifier = Modifier.size(32.dp)) },
                    enabled = enabled,
                    color = Green,
                    onClick = { onCommand("play_pause") },
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    label = "下一曲",
                    icon = { Icon(Icons.Filled.SkipNext, "下一曲", modifier = Modifier.size(32.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("next") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 第2行: 音量－ | 音量＋ ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlButton(
                    label = "音量－",
                    icon = { Icon(Icons.Filled.VolumeDown, "音量－", modifier = Modifier.size(32.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("vol_down") },
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    label = "音量＋",
                    icon = { Icon(Icons.Filled.VolumeUp, "音量＋", modifier = Modifier.size(32.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("vol_up") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 第3行: ♥ 喜欢 (全宽, 红色) ──
            ControlButton(
                label = "喜欢",
                icon = { Icon(Icons.Filled.Favorite, "喜欢", modifier = Modifier.size(24.dp)) },
                enabled = enabled,
                color = Red,
                fullWidth = true,
                onClick = { onCommand("like") }
            )

            Spacer(Modifier.height(12.dp))

            // ── 第4行: 切换模式 | 歌词 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ControlButton(
                    label = "切换模式",
                    icon = { Icon(Icons.Filled.CropFree, "切换模式", modifier = Modifier.size(24.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("toggle_mode") },
                    modifier = Modifier.weight(1f)
                )
                ControlButton(
                    label = "歌词",
                    icon = { Icon(Icons.Filled.Subtitles, "歌词", modifier = Modifier.size(24.dp)) },
                    enabled = enabled,
                    onClick = { onCommand("toggle_lyrics") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
//  单个控制按钮
// ═══════════════════════════════════════════

@Composable
private fun ControlButton(
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Gray,
    fullWidth: Boolean = false,
) {
    val bg = if (enabled) color else color.copy(alpha = 0.3f)
    val shape = RoundedCornerShape(16.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = if (fullWidth) modifier.fillMaxWidth().heightIn(min = 56.dp)
                   else modifier.heightIn(min = 72.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            disabledContainerColor = color.copy(alpha = 0.15f)
        ),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
