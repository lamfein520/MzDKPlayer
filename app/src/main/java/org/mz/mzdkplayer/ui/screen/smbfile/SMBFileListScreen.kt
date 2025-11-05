package org.mz.mzdkplayer.ui.screen.smbfile

import NoSearchResult
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.mz.mzdkplayer.MzDkPlayerApplication
import org.mz.mzdkplayer.R
import org.mz.mzdkplayer.logic.model.AudioItem
import org.mz.mzdkplayer.logic.model.FileConnectionStatus
import org.mz.mzdkplayer.tool.Tools
import org.mz.mzdkplayer.tool.Tools.VideoBigIcon
import org.mz.mzdkplayer.tool.builderPlayer
import org.mz.mzdkplayer.tool.setupPlayer

import org.mz.mzdkplayer.ui.screen.common.FileEmptyScreen

import org.mz.mzdkplayer.ui.screen.common.LoadingScreen
import org.mz.mzdkplayer.ui.screen.common.VAErrorScreen
import org.mz.mzdkplayer.ui.screen.vm.SMBConViewModel

import org.mz.mzdkplayer.ui.style.myListItemColor
import org.mz.mzdkplayer.ui.style.myTTFColor
import org.mz.mzdkplayer.ui.theme.TvTextField
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(UnstableApi::class)
@Composable
fun SMBFileListScreen(path: String?, navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SMBConViewModel = viewModel()
    val files by viewModel.fileList.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var focusedFileName by remember { mutableStateOf<String?>(null) }
    var focusedIsDir by remember { mutableStateOf(true) }
    var focusedMediaUri by remember { mutableStateOf("") }
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    var seaText: String by remember { mutableStateOf("") }

    // 过滤后的文件列表
    var filteredFiles by remember { mutableStateOf(emptyList<FileListItemData>()) }
    // 是否正在加载
    var isLoading by remember { mutableStateOf(true) }
    // 添加首次加载标志
    var isFirstLoad by remember { mutableStateOf(true) }
    // 处理路径变化和连接状态
    LaunchedEffect(path, connectionStatus) {
        val decodedPath = try {
            URLDecoder.decode(path ?: "", "UTF-8")
        } catch (e: Exception) {
            Log.e("SMBFileListScreen", "路径解码失败: $e")
            Toast.makeText(context, "路径格式错误", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        if (decodedPath.isEmpty()) {
            Log.w("SMBFileListScreen", "路径为空")
            return@LaunchedEffect
        }

        // 解析SMB路径
        val smbConfig = viewModel.parseSMBPath(decodedPath)
        if (smbConfig.server.isEmpty()) {
            Log.e("SMBFileListScreen", "无效的SMB路径: $decodedPath")
            Toast.makeText(context, "无效的SMB路径", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        when (connectionStatus) {
            is FileConnectionStatus.Disconnected -> {
                Log.d("SMBFileListScreen", "未连接，开始连接: ${smbConfig.server}")
                delay(300)
                viewModel.connectToSMB(
                    smbConfig.server,
                    smbConfig.username,
                    smbConfig.password,
                    smbConfig.share
                )
            }

            is SMBConnectionStatus.Connected -> {
                delay(300)
                Log.d("SMBFileListScreen", "已连接，列出文件: ${smbConfig.path}")
                viewModel.listSMBFiles(smbConfig)
            }

            is FileConnectionStatus.Error -> {
                val errorMessage = (connectionStatus as FileConnectionStatus.Error).message
                Log.e("SMBFileListScreen", "连接错误: $errorMessage")
                Toast.makeText(context, "SMB错误: $errorMessage", Toast.LENGTH_LONG).show()
            }

            is FileConnectionStatus.LoadingFile -> {
                Log.d("SMBFileListScreen", "正在加载文件...")
            }

            is FileConnectionStatus.FilesLoaded -> {
                Log.d("SMBFileListScreen", "文件加载完成")
                isLoading = false
                if (isFirstLoad) {
                    isFirstLoad = false
                }
            }

            is FileConnectionStatus.Connecting -> {
                Log.d("SMBFileListScreen", "正在连接...")
            }

        }
    }

    // 处理焦点变化和媒体播放
    LaunchedEffect(focusedFileName, focusedIsDir) {
        // 释放之前的播放器
        exoPlayer?.release()

        if (!focusedIsDir && focusedFileName != null) {
            val extension = Tools.extractFileExtension(focusedFileName)
            if (Tools.containsVideoFormat(extension)) {
                Log.d("SMBFileListScreen", "准备播放视频: $focusedFileName")

                try {
//                    exoPlayer = withContext(Dispatchers.Main) {
//                        builderPlayer(mediaUri = focusedMediaUri, context, dataSourceType = "SMB")
//                    }

//                    withContext(Dispatchers.Main) {
//                        setupPlayer(
//                            exoPlayer!!,
//                            focusedMediaUri,
//                            "SMB",
//                            context,
//                            { mediaInfoMap ->
//                                Log.d("SMBFileListScreen", "媒体信息: $mediaInfoMap")
//                            },
//                            onError = { errorMessage ->
//                                Log.e("SMBFileListScreen", "播放错误: $errorMessage")
//                                //Toast.makeText(context, "播放错误: $errorMessage", Toast.LENGTH_SHORT).show()
//                            }
//                        )
//                    }
                } catch (e: Exception) {
                    Log.e("SMBFileListScreen", "播放器初始化失败: ${e.message}", e)
                    // Toast.makeText(context, "播放器初始化失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            Log.d("SMBFileListScreen", "界面销毁，释放资源")
            exoPlayer?.release()
            viewModel.disconnectSMB()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 👈 先铺满黑色背景
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()// 👈 防止底层界面透出
                .padding(16.dp)
        ) {
            when (connectionStatus) {
                is SMBConnectionStatus.Connecting -> {

//                LoadingScreen(
//                    "正在连接SMB服务器",
//                    Modifier
//                        .fillMaxSize()
//                        .background(Color.Black)
//                )
                }


                is SMBConnectionStatus.Connected, is SMBConnectionStatus.LoadingFiled -> {
                    if (files.isEmpty() && !isLoading) {
                        FileEmptyScreen("此目录为空")
                        return@Box
                    }
                    if (isLoading) {
                        LoadingScreen(
                            "正在加载SMB文件",
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    } else {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxHeight()
                                    .weight(0.7f)
                            ) {
                                if (filteredFiles.isEmpty() && seaText.isNotEmpty()) {
                                    // 显示搜索结果为空的提示
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "搜索结果为空",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                } else if (!isLoading) {
                                    // 显示过滤后的文件列表
                                    items(filteredFiles) { file ->
                                        CommonFileListItem(
                                            file,
                                            context = context,
                                            navController,
                                            onFocused = {
                                                focusedFileName = file.fileName
                                                focusedIsDir = file.isDirectory
                                                focusedMediaUri =
                                                    file.filePath // 因为它已经是 smb://... 形式
                                                Log.d(
                                                    "SMBFileListScreen",
                                                    "焦点变化: ${file.fileName}, 是目录: $focusedIsDir"
                                                )
                                            })
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.3f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                TvTextField(
                                    seaText,
                                    onValueChange = { seaText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = "请输入文件名",
                                    colors = myTTFColor(),
                                    textStyle = TextStyle(color = Color.White)
                                )
                                VideoBigIcon(
                                    focusedIsDir,
                                    focusedFileName,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .fillMaxWidth()
                                )
                                focusedFileName?.let { fileName ->
                                    Text(
                                        fileName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                SMBConnectionStatus.Disconnected -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "未连接到 SMB 服务器",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        // 可以添加连接按钮
                    }
                }

                is SMBConnectionStatus.Error -> {
                    val errorMessage = (connectionStatus as SMBConnectionStatus.Error).message
                    Text(
                        "加载失败: $errorMessage",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                SMBConnectionStatus.LoadingFile -> {
                    LoadingScreen(
                        "正在加载SMB文件",
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}


