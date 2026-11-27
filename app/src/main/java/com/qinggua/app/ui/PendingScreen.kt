package com.qinggua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.qinggua.app.data.MediaItem
import com.qinggua.app.data.SwipeAction

/** 待定区：网格预览 + 点进去二次分类（只能保留/删除，不能再次待定） */
@Composable
fun PendingScreen(vm: AppViewModel, onBack: () -> Unit) {
    val items = vm.itemsIn(Bucket.PENDING)
    var reviewing by remember { mutableStateOf<MediaItem?>(null) }

    if (reviewing != null) {
        PendingReview(vm, reviewing!!, queueSize = items.size, onDone = { reviewing = null })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        ScreenHeader("待定", "共 ${items.size} 张 · 下滑进来的照片都在这里") {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = C.Ink2)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.PendingBg)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text("这里只能左滑保留或右滑删除，不能再次待定", color = C.Pending, fontSize = 12.sp)
        }
        MediaGrid(
            items = items,
            selectionMode = false,
            selected = emptySet(),
            onToggle = {},
            onTap = { reviewing = it }
        )
    }
}

@Composable
private fun PendingReview(vm: AppViewModel, item: MediaItem, queueSize: Int, onDone: () -> Unit) {
    val settings by vm.settings.collectAsState()
    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = C.Ink2)
            }
            Text("待定整理 · 剩 $queueSize 张", color = C.Ink, fontSize = 14.sp)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            SwipeCard(
                item = item,
                sensitivity = settings.gestureSensitivity,
                enabledActions = setOf(SwipeAction.KEEP, SwipeAction.TRASH),
                onSwiped = { action ->
                    vm.swipe(item, action, com.qinggua.app.data.Lane.ALL, 0)
                    onDone()
                }
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row {
                ActionButton("保留", onClick = {
                    vm.swipe(item, SwipeAction.KEEP, com.qinggua.app.data.Lane.ALL, 0); onDone()
                }, modifier = Modifier.weight(1f), color = C.Keep, background = C.KeepBg)
                Spacer(Modifier.width(12.dp))
                ActionButton("删除", onClick = {
                    vm.swipe(item, SwipeAction.TRASH, com.qinggua.app.data.Lane.ALL, 0); onDone()
                }, modifier = Modifier.weight(1f), color = C.Delete, background = C.DeleteBg)
            }
        }
    }
}
