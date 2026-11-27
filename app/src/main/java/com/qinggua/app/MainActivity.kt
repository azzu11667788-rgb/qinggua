package com.qinggua.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qinggua.app.data.AppViewModel
import com.qinggua.app.data.Lane
import com.qinggua.app.ui.C
import com.qinggua.app.ui.FilterScreen
import com.qinggua.app.ui.KeptScreen
import com.qinggua.app.ui.LockScreen
import com.qinggua.app.ui.PendingScreen
import com.qinggua.app.ui.QingGuaTheme
import com.qinggua.app.ui.SettingsScreen
import com.qinggua.app.ui.SwipeScreen
import com.qinggua.app.ui.TrashScreen
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QingGuaTheme {
                AppEntry()
            }
        }
    }

    private fun mediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun allGranted(context: Context): Boolean =
        mediaPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @Composable
    private fun AppEntry() {
        val context = LocalContext.current
        val vm: AppViewModel = viewModel()
        var granted by remember { mutableStateOf(allGranted(context)) }
        var unlocked by remember { mutableStateOf(false) }
        val settings by vm.settings.collectAsState()

        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            granted = result.values.all { it }
            if (granted) vm.reload()
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (!granted) permLauncher.launch(mediaPermissions())
            else vm.reload()
        }

        if (!granted) {
            PermissionGate(onRequest = { permLauncher.launch(mediaPermissions()) })
            return
        }

        if (settings.unlockEnabled && !unlocked) {
            BiometricGate(
                onSuccess = { unlocked = true },
                onFail = { vm.notify("验证失败，请重试") }
            )
            return
        }

        MainScaffold(vm)
    }

    @Composable
    private fun PermissionGate(onRequest: () -> Unit) {
        Box(Modifier.fillMaxSize().background(C.Surface).padding(32.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("需要相册权限", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("清瓜需要读取你的照片和视频，才能开始整理。", color = C.Ink2, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .clickable { onRequest() }
                        .background(C.Keep, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("授予权限", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    @Composable
    private fun BiometricGate(onSuccess: () -> Unit, onFail: () -> Unit) {
        val context = LocalContext.current

        fun authenticate() {
            val executor = ContextCompat.getMainExecutor(context)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationFailed() { onFail() }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED) onFail()
                }
            }
            try {
                val prompt = BiometricPrompt(context as FragmentActivity, executor, callback)
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("解锁清瓜")
                    .setSubtitle("验证手机锁屏")
                    .setAllowedAuthenticators(authenticators)
                    .build()
                prompt.authenticate(info)
            } catch (e: Exception) {
                onSuccess() // 设备不支持生物识别时放行，避免卡死
            }
        }

        DisposableEffect(Unit) {
            authenticate()
            onDispose { }
        }
        LockScreen(onUnlockClick = { authenticate() })
    }

    @Composable
    private fun MainScaffold(vm: AppViewModel) {
        var tab by remember { mutableStateOf(Tab.SWIPE) }
        var lane by remember { mutableStateOf(Lane.PHOTO) }
        var showFilter by remember { mutableStateOf(false) }
        var showPending by remember { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }
        val toast by vm.toast.collectAsState()

        androidx.compose.runtime.LaunchedEffect(toast) {
            toast?.let {
                snackbar.showSnackbar(it)
                vm.clearToast()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                BottomBar(current = tab, onSelect = { tab = it })
            },
            containerColor = C.Page
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.SWIPE -> SwipeScreen(
                        vm = vm,
                        lane = lane,
                        onLaneChange = { lane = it },
                        onOpenFilter = { showFilter = true },
                        onOpenPending = { showPending = true }
                    )
                    Tab.PENDING -> PendingScreen(vm, onBack = { tab = Tab.SWIPE })
                    Tab.TRASH -> TrashScreen(vm)
                    Tab.KEPT -> KeptScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm, onBack = { tab = Tab.SWIPE })
                }

                if (showFilter) {
                    FilterScreen(vm, onBack = { showFilter = false })
                }
                if (showPending) {
                    PendingScreen(vm, onBack = { showPending = false })
                }
            }
        }
    }

    private enum class Tab(val label: String, val icon: ImageVector) {
        SWIPE("整理", Icons.Rounded.Home),
        PENDING("待定", Icons.Rounded.Schedule),
        TRASH("回收箱", Icons.Rounded.DeleteOutline),
        KEPT("已保留", Icons.Rounded.FavoriteBorder),
        SETTINGS("设置", Icons.Rounded.Settings)
    }

    @Composable
    private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.Surface)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Tab.values().forEach { tab ->
                val active = tab == current
                Column(
                    modifier = Modifier
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = if (active) C.Keep else C.Ink3,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        color = if (active) C.Keep else C.Ink3,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
