package com.qinggua.app.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinggua.app.data.AppViewModel
import com.qinggua.app.data.Bucket

/** 已保留页：可批量移回待整理或移到回收箱 */
@Composable
fun KeptScreen(vm: AppViewModel) {
    val items = vm.itemsIn(Bucket.KEPT)
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val aliveSelected = selected.intersect(items.map { it.id }.toSet())

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        ScreenHeader("已保留", "共 ${items.size} 张 · 照片仍留在手机相册原位") {
            androidx.compose.material3.TextButton(onClick = {
                selected = if (aliveSelected.size == items.size && items.isNotEmpty())
                    emptySet() else items.map { it.id }.toSet()
            }) {
                androidx.compose.material3.Text(
                    if (aliveSelected.size == items.size && items.isNotEmpty()) "取消全选" else "全选",
                    color = C.Info, fontSize = 13.sp
                )
            }
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
                    "移回待整理",
                    onClick = {
                        if (aliveSelected.isEmpty()) { vm.notify("先选择照片"); return@ActionButton }
                        vm.backToUnreviewed(aliveSelected, desc = "已保留移回待整理 ${aliveSelected.size} 张")
                        selected = emptySet()
                    },
                    modifier = Modifier.weight(1f),
                    color = C.Keep,
                    background = C.KeepBg,
                    enabled = aliveSelected.isNotEmpty()
                )
                Spacer(Modifier.width(12.dp))
                ActionButton(
                    "移到回收箱",
                    onClick = {
                        if (aliveSelected.isEmpty()) { vm.notify("先选择照片"); return@ActionButton }
                        vm.moveBatch(aliveSelected, Bucket.TRASH, desc = "已保留移到回收箱 ${aliveSelected.size} 张")
                        selected = emptySet()
                    },
                    modifier = Modifier.weight(1f),
                    color = C.Delete,
                    background = C.DeleteBg,
                    enabled = aliveSelected.isNotEmpty()
                )
            }
        }
    }
}
