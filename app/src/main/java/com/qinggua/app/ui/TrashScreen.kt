package com.qinggua.app.ui

import androidx.compose.foundation.layout.width
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinggua.app.data.AppViewModel
import com.qinggua.app.data.Bucket
import com.qinggua.app.data.MediaItem
import com.qinggua.app.data.MediaRepository

/** 回收箱：多选/全选后真删 */
@Composable
fun TrashScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val items = vm.itemsIn(Bucket.TRASH)
    val settings by vm.settings.collectAsState()
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingTargets by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val deleteLauncher: ActivityResultLauncher<IntentSenderRequest> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                vm.onDeleted(pendingTargets.map { it.id })
                selected = emptySet()
            }
            pendingTargets = emptyList()
        }

    val aliveSelected = selected.intersect(items.map { it.id }.toSet())

    fun requestRealDelete() {
        val targets = items.filter { it.id in aliveSelected }
        if (targets.isEmpty()) return
        if (settings.trashConfirm) {
            pendingTargets = targets
            confirmDelete = true
        } else {
            executeDelete(context, targets, deleteLauncher, vm) { selected = emptySet() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        ScreenHeader("回收箱", "共 ${items.size} 张 · 未真删前可随时恢复") {
            TextButton(onClick = {
                selected = if (aliveSelected.size == items.size && items.isNotEmpty())
                    emptySet() else items.map { it.id }.toSet()
            }) {
                Text(
                    if (aliveSelected.size == items.size && items.isNotEmpty()) "取消全选" else "全选",
                    color = C.Info, fontSize = 13.sp
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().background(C.DeleteBg).padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("选中的照片会从手机里真正删除，不可恢复", color = C.Delete, fontSize = 12.sp)
        }

        MediaGrid(
            items = items,
            selectionMode = true,
            selected = aliveSelected,
            onToggle = { id -> selected = if (id in selected) selected - id else selected + id },
            onTap = {}
        )

        Box(modifier = Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    "恢复所选",
                    onClick = {
                        if (aliveSelected.isEmpty()) { vm.notify("先选择照片"); return@ActionButton }
                        vm.backToUnreviewed(aliveSelected, desc = "已从回收箱恢复 ${aliveSelected.size} 张")
                        selected = emptySet()
                    },
                    modifier = Modifier.weight(1f),
                    color = C.Info,
                    background = C.InfoBg,
                    enabled = aliveSelected.isNotEmpty()
                )
                Spacer(Modifier.width(12.dp))
                ActionButton(
                    "删除所选${if (aliveSelected.isNotEmpty()) " (${aliveSelected.size})" else ""}",
                    onClick = { requestRealDelete() },
                    modifier = Modifier.weight(1f),
                    color = C.Delete,
                    background = C.DeleteBg,
                    enabled = aliveSelected.isNotEmpty()
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确认真正删除") },
            text = { Text("将从手机里永久删除选中的 ${pendingTargets.size} 张照片/视频，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    executeDelete(context, pendingTargets, deleteLauncher, vm) { selected = emptySet() }
                }) { Text("确认删除", color = C.Delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
}

/** 执行真删：Android 11+ 走系统确认弹窗，低版本直接删 */
private fun executeDelete(
    context: android.content.Context,
    targets: List<MediaItem>,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    vm: AppViewModel,
    onDone: () -> Unit
) {
    if (targets.isEmpty()) return
    val sender = MediaRepository.createDeleteRequest(context, targets)
    if (sender != null) {
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    } else {
        MediaRepository.deleteLegacy(context, targets)
        vm.onDeleted(targets.map { it.id })
        onDone()
    }
}
