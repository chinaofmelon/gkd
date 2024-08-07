package li.songe.gkd.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.dylanc.activityresult.launcher.launchForResult
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.app
import li.songe.gkd.appScope
import li.songe.gkd.debug.FloatingService
import li.songe.gkd.debug.HttpService
import li.songe.gkd.debug.ScreenshotService
import li.songe.gkd.permission.canDrawOverlaysState
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.permission.shizukuOkState
import li.songe.gkd.shizuku.CommandResult
import li.songe.gkd.shizuku.newActivityTaskManager
import li.songe.gkd.shizuku.newUserService
import li.songe.gkd.shizuku.safeGetTasks
import li.songe.gkd.ui.component.AuthCard
import li.songe.gkd.ui.component.SettingItem
import li.songe.gkd.ui.component.TextSwitch
import li.songe.gkd.ui.component.updateDialogOptions
import li.songe.gkd.ui.destinations.ActivityLogPageDestination
import li.songe.gkd.ui.destinations.SnapshotPageDestination
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemPadding
import li.songe.gkd.ui.style.titleItemPadding
import li.songe.gkd.util.LocalLauncher
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.appInfoCacheFlow
import li.songe.gkd.util.buildLogFile
import li.songe.gkd.util.json
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.openApp
import li.songe.gkd.util.openUri
import li.songe.gkd.util.saveFileToDownloads
import li.songe.gkd.util.shareFile
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast
import rikka.shizuku.Shizuku

@RootNavGraph
@Destination(style = ProfileTransitions::class)
@Composable
fun AdvancedPage() {
    val context = LocalContext.current as MainActivity
    val vm = hiltViewModel<AdvancedVm>()
    val launcher = LocalLauncher.current
    val navController = LocalNavController.current
    val store by storeFlow.collectAsState()
    val snapshotCount by vm.snapshotCountFlow.collectAsState()

    ShizukuErrorDialog(vm.shizukuErrorFlow)
    vm.uploadOptions.ShowDialog()

    var showPortDlg by remember {
        mutableStateOf(false)
    }
    var showShareLogDlg by remember {
        mutableStateOf(false)
    }
    if (showShareLogDlg) {
        Dialog(onDismissRequest = { showShareLogDlg = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                Text(
                    text = "分享到其他应用", modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logZipFile = buildLogFile()
                                context.shareFile(logZipFile, "分享日志文件")
                            }
                        })
                        .then(modifier)
                )
                Text(
                    text = "保存到下载", modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logZipFile = buildLogFile()
                                context.saveFileToDownloads(logZipFile)
                            }
                        })
                        .then(modifier)
                )
                Text(
                    text = "生成链接(需科学上网)",
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            showShareLogDlg = false
                            vm.viewModelScope.launchTry(Dispatchers.IO) {
                                val logZipFile = buildLogFile()
                                vm.uploadOptions.startTask(logZipFile)
                            }
                        })
                        .then(modifier)
                )
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(scrollBehavior = scrollBehavior, navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }, title = { Text(text = "高级模式") }, actions = {})
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Text(
                text = "Shizuku",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val shizukuOk by shizukuOkState.stateFlow.collectAsState()
            if (!shizukuOk) {
                AuthCard(title = "Shizuku授权",
                    desc = "高级模式:准确区别界面ID,强制模拟点击",
                    onAuthClick = {
                        try {
                            Shizuku.requestPermission(Activity.RESULT_OK)
                        } catch (e: Exception) {
                            LogUtils.d("Shizuku授权错误", e.message)
                            vm.shizukuErrorFlow.value = true
                        }
                    })
                ShizukuFragment(false)
            } else {
                ShizukuFragment()
            }

            val httpServerRunning by HttpService.isRunning.collectAsState()
            val localNetworkIps by HttpService.localNetworkIpsFlow.collectAsState()

            Text(
                text = "HTTP服务",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.itemPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HTTP服务",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium
                    ) {
                        if (!httpServerRunning) {
                            Text(
                                text = "在浏览器下连接调试工具",
                            )
                        } else {
                            Text(
                                text = "点击下面任意链接打开即可自动连接",
                            )
                            Row {
                                Text(
                                    text = "http://127.0.0.1:${store.httpServerPort}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                    modifier = Modifier.clickable {
                                        context.openUri("http://127.0.0.1:${store.httpServerPort}")
                                    },
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = "仅本设备可访问")
                            }
                            localNetworkIps.forEach { host ->
                                Text(
                                    text = "http://${host}:${store.httpServerPort}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                                    modifier = Modifier.clickable {
                                        context.openUri("http://${host}:${store.httpServerPort}")
                                    }
                                )
                            }
                        }
                    }
                }
                Switch(
                    checked = httpServerRunning,
                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, notificationState)
                            HttpService.start()
                        } else {
                            HttpService.stop()
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .clickable { showPortDlg = true }
                    .itemPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "服务端口",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = store.httpServerPort.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
            }

            TextSwitch(
                name = "保留内存订阅",
                desc = "当HTTP服务关闭时,保留内存订阅",
                checked = !store.autoClearMemorySubs
            ) {
                storeFlow.value = store.copy(
                    autoClearMemorySubs = !it
                )
            }

            Text(
                text = "快照",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingItem(
                title = "快照记录" + (if (snapshotCount > 0) "-$snapshotCount" else ""),
                onClick = {
                    navController.navigate(SnapshotPageDestination)
                }
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val screenshotRunning by ScreenshotService.isRunning.collectAsState()
                TextSwitch(
                    name = "截屏服务",
                    desc = "生成快照需要获取屏幕截图",
                    checked = screenshotRunning,
                    onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                        if (it) {
                            requiredPermission(context, notificationState)
                            val mediaProjectionManager =
                                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            val activityResult =
                                launcher.launchForResult(mediaProjectionManager.createScreenCaptureIntent())
                            if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
                                ScreenshotService.start(intent = activityResult.data!!)
                            }
                        } else {
                            ScreenshotService.stop()
                        }
                    }
                )
            }

            val floatingRunning by FloatingService.isRunning.collectAsState()
            TextSwitch(
                name = "悬浮窗服务",
                desc = "显示截屏按钮,点击即可保存快照",
                checked = floatingRunning,
                onCheckedChange = vm.viewModelScope.launchAsFn<Boolean> {
                    if (it) {
                        requiredPermission(context, notificationState)
                        requiredPermission(context, canDrawOverlaysState)
                        val intent = Intent(context, FloatingService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    } else {
                        FloatingService.stop(context)
                    }
                }
            )

            TextSwitch(
                name = "音量快照",
                desc = "音量变化时生成快照,若悬浮按钮不工作可使用",
                checked = store.captureVolumeChange
            ) {
                storeFlow.value = store.copy(
                    captureVolumeChange = it
                )
            }

            TextSwitch(
                name = "截屏快照",
                descContent = {
                    Row {
                        Text(
                            text = "触发截屏时保存快照",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "查看限制",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = throttle {
                                context.mainVm.dialogFlow.updateDialogOptions(
                                    title = "限制说明",
                                    text = "仅支持部分小米设备截屏触发\n\n只保存节点信息不保存图片, 用户需要在快照记录里替换截图",
                                )
                            })
                        )
                    }
                },
                checked = store.captureScreenshot
            ) {
                storeFlow.value = store.copy(
                    captureScreenshot = it
                )
            }

            TextSwitch(
                name = "隐藏状态栏",
                desc = "隐藏快照截图顶部状态栏",
                checked = store.hideSnapshotStatusBar
            ) {
                storeFlow.value = store.copy(
                    hideSnapshotStatusBar = it
                )
            }

            TextSwitch(
                name = "保存提示",
                desc = "保存快照时提示\"正在保存快照\"",
                checked = store.showSaveSnapshotToast
            ) {
                storeFlow.value = store.copy(
                    showSaveSnapshotToast = it
                )
            }

            Text(
                text = "界面记录",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            TextSwitch(
                name = "记录界面",
                desc = "记录打开的应用及界面",
                checked = store.enableActivityLog
            ) {
                storeFlow.value = store.copy(
                    enableActivityLog = it
                )
            }
            SettingItem(
                title = "界面记录",
                onClick = {
                    navController.navigate(ActivityLogPageDestination)
                }
            )

            Text(
                text = "日志",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            TextSwitch(
                name = "保存日志",
                desc = "保存7天日志,帮助定位BUG",
                checked = store.log2FileSwitch,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        log2FileSwitch = it
                    )
                    if (!it) {
                        context.mainVm.viewModelScope.launchTry(Dispatchers.IO) {
                            val logFiles = LogUtils.getLogFiles()
                            if (logFiles.isNotEmpty()) {
                                logFiles.forEach { f ->
                                    f.delete()
                                }
                                toast("已删除全部日志")
                            }
                        }
                    }
                })

            SettingItem(title = "导出日志", imageVector = Icons.Default.Upload, onClick = {
                showShareLogDlg = true
            })

            Text(
                text = "其它",
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            TextSwitch(name = "前台悬浮窗",
                desc = "添加透明悬浮窗,关闭可能导致不点击/点击缓慢",
                checked = store.enableAbFloatWindow,
                onCheckedChange = {
                    storeFlow.value = store.copy(
                        enableAbFloatWindow = it
                    )
                })

            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }

    if (showPortDlg) {
        var value by remember {
            mutableStateOf(store.httpServerPort.toString())
        }
        AlertDialog(title = { Text(text = "服务端口") }, text = {
            OutlinedTextField(
                value = value,
                placeholder = {
                    Text(text = "请输入 5000-65535 的整数")
                },
                onValueChange = {
                    value = it.filter { c -> c.isDigit() }.take(5)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        text = "${value.length} / 5",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
        }, onDismissRequest = {
            if (value.isEmpty()) {
                showPortDlg = false
            }
        }, confirmButton = {
            TextButton(
                enabled = value.isNotEmpty(),
                onClick = {
                    val newPort = value.toIntOrNull()
                    if (newPort == null || !(5000 <= newPort && newPort <= 65535)) {
                        toast("请输入 5000-65535 的整数")
                        return@TextButton
                    }
                    storeFlow.value = store.copy(
                        httpServerPort = newPort
                    )
                    showPortDlg = false
                }
            ) {
                Text(
                    text = "确认", modifier = Modifier
                )
            }
        }, dismissButton = {
            TextButton(onClick = { showPortDlg = false }) {
                Text(
                    text = "取消"
                )
            }
        })
    }
}

@Composable
private fun ShizukuFragment(enabled: Boolean = true) {
    val store by storeFlow.collectAsState()
    TextSwitch(name = "Shizuku-界面识别",
        desc = "更准确识别界面ID",
        checked = store.enableShizukuActivity,
        enabled = enabled,
        onCheckedChange = { enableShizuku ->
            if (enableShizuku) {
                appScope.launchTry(Dispatchers.IO) {
                    // 校验方法是否适配, 再允许使用 shizuku
                    val tasks =
                        newActivityTaskManager()?.safeGetTasks()?.firstOrNull()
                    if (tasks != null) {
                        storeFlow.value = store.copy(
                            enableShizukuActivity = true
                        )
                    } else {
                        toast("Shizuku-界面识别校验失败,无法使用")
                    }
                }
            } else {
                storeFlow.value = store.copy(
                    enableShizukuActivity = false
                )
            }
        })

    TextSwitch(
        name = "Shizuku-模拟点击",
        desc = "变更 clickCenter 为强制模拟点击",
        checked = store.enableShizukuClick,
        enabled = enabled,
        onCheckedChange = { enableShizuku ->
            if (enableShizuku) {
                appScope.launchTry(Dispatchers.IO) {
                    val service = newUserService()
                    val result = service.userService.execCommand("input tap 0 0")
                    service.destroy()
                    if (json.decodeFromString<CommandResult>(result).code == 0) {
                        storeFlow.update { it.copy(enableShizukuClick = true) }
                    } else {
                        toast("Shizuku-模拟点击校验失败,无法使用")
                    }
                }
            } else {
                storeFlow.value = store.copy(
                    enableShizukuClick = false
                )
            }

        })

}

@Composable
private fun ShizukuErrorDialog(stateFlow: MutableStateFlow<Boolean>) {
    val state = stateFlow.collectAsState()
    if (state.value) {
        val appId = "moe.shizuku.privileged.api"
        val appInfoCache = appInfoCacheFlow.collectAsState()
        val installed = appInfoCache.value.contains(appId)
        AlertDialog(
            onDismissRequest = { stateFlow.value = false },
            title = { Text(text = "授权错误") },
            text = {
                Text(
                    text = if (installed) {
                        "Shizuku 授权失败, 请检查是否运行"
                    } else {
                        "Shizuku 未安装, 请先下载后安装"
                    }
                )
            },
            confirmButton = {
                if (installed) {
                    TextButton(onClick = {
                        stateFlow.value = false
                        app.openApp(appId)
                    }) {
                        Text(text = "打开 Shizuku")
                    }
                } else {
                    TextButton(onClick = {
                        stateFlow.value = false
                        app.openUri("https://shizuku.rikka.app/")
                    }) {
                        Text(text = "去下载")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { stateFlow.value = false }) {
                    Text(text = "我知道了")
                }
            }
        )
    }
}
