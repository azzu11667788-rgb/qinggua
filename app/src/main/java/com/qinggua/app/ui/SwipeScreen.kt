package com.qinggua.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinggua.app.data.AppViewModel
import com.qinggua.app.data.Lane
import com.qinggua.app.data.SwipeAction

/** 主滑动页：三入口 + 卡片流 + 底部待定投放区 */
@Composable
fun SwipeScreen(
    vm: AppViewModel,
    lane: Lane,
    onLaneChange: (Lane) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenPending: () -> Unit
) {
    val queue = vm.queueFor(lane)
    val cursor = vm.cursor(lane)
    val settings by vm.settings.collectAsState()
    val pendingCount = vm.itemsIn(com.qinggua.app.data.Bucket.PENDING).size

    var dropActive by remember { mutableStateOf(false) }
    val zoneScale by animateFloatAsState(if (dropActive) 1.03f else 1f)

    // 游标越界时归零
    LaunchedEffect(queue.size) {
        if (cursor > queue.size && queue.isNotEmpty()) vm.setCursor(lane, queue.size)
    }

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        // 入口切换
        Row(
            modifier = Modifier.fillMaxWidth().background(C.Surface).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaneTab("照片", Lane.PHOTO, lane, onLaneChange)
            LaneTab("视频", Lane.VIDEO, lane, onLaneChange)
            LaneTab("合并", Lane.ALL, lane, onLaneChange)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenFilter) {
                Icon(Icons.Rounded.FilterList, "筛选", tint = C.Ink2)
            }
            IconButton(onClick = { vm.undoLast() }, enabled = vm.canUndo()) {
                Icon(Icons.Rounded.RestartAlt, "撤销", tint = if (vm.canUndo()) C.Info else C.Ink3)
            }
        }

        // 进度
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (queue.isEmpty()) "整理完成" else "还剩 ${queue.size - cursor.coerceAtMost(queue.size)} 张",
                color = C.Ink2, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            vm.toast.collectAsState().value?.let {
                Text(it, color = C.Info, fontSize = 12.sp)
            }
        }

        // 卡片区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            val index = cursor.coerceIn(0, queue.size)
            if (queue.isEmpty() || index >= queue.size) {
                EmptyQueue(onOpenFilter)
            } else {
                val item = queue[index]
                // 背景叠层卡片
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .alpha(0.5f)
                        .background(C.Line, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                )
                SwipeCard(
                    item = item,
                    sensitivity = settings.gestureSensitivity,
                    onSwiped = { action ->
                        vm.swipe(item, action, lane, index)
                        vm.setCursor(lane, index + 1)
                    },
                    onDragOffset = { dropActive = it > 60f }
                )
            }
        }

        // 底部固定待定投放区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.Page)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .clickable { onOpenPending() }
        ) {
            Box(modifier = Modifier.fillMaxWidth().alpha(zoneScale)) {
                PendingDropZone(dropActive, pendingCount, onOpenPending)
            }
        }
    }
}

@Composable
private fun LaneTab(label: String, target: Lane, current: Lane, onChange: (Lane) -> Unit) {
    val active = target == current
    Box(
        modifier = Modifier
            .clickable { onChange(target) }
            .background(if (active) C.KeepBg else androidx.compose.ui.graphics.Color.Transparent,
                androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (active) C.Keep else C.Ink2,
            fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun EmptyQueue(onOpenFilter: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", color = C.Keep, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("这一组都整理完了", color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text("可以换个筛选条件继续", color = C.Ink3, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clickable { onOpenFilter() }
                    .background(C.KeepBg, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("打开筛选", color = C.Keep, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
