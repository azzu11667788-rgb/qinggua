package com.qinggua.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qinggua.app.data.MediaItem
import com.qinggua.app.data.SwipeAction
import com.qinggua.app.util.Format
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * 单张滑动卡片。
 * 左滑 = 保留，右滑 = 删除，下滑 = 待定。
 * onDragOffset 用于通知外层高亮底部待定投放区。
 */
@Composable
fun SwipeCard(
    item: MediaItem,
    sensitivity: Float,
    enabledActions: Set<SwipeAction> = setOf(SwipeAction.KEEP, SwipeAction.TRASH, SwipeAction.PENDING),
    onSwiped: (SwipeAction) -> Unit,
    onDragOffset: (Float) -> Unit = {},
    onTap: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { (configuration.screenWidthDp.dp * sensitivity).toPx() }

    suspend fun settle(action: SwipeAction?) {
        if (action != null) {
            onSwiped(action)
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
        } else {
            offsetX.animateTo(0f, spring())
            offsetY.animateTo(0f, spring())
        }
        onDragOffset(0f)
    }

    fun decide(): SwipeAction? = when {
        offsetX.value <= -thresholdPx && SwipeAction.KEEP in enabledActions -> SwipeAction.KEEP
        offsetX.value >= thresholdPx && SwipeAction.TRASH in enabledActions -> SwipeAction.TRASH
        offsetY.value >= thresholdPx && SwipeAction.PENDING in enabledActions -> SwipeAction.PENDING
        else -> null
    }

    val progressX = (offsetX.value / thresholdPx).coerceIn(-1f, 1f)
    val progressY = (offsetY.value / thresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = offsetX.value / 45f
                alpha = 1f - (progressX.absoluteValue * 0.25f)
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtLeast(0f))
                        }
                        onDragOffset(offsetY.value)
                    },
                    onDragEnd = {
                        scope.launch { settle(decide()) }
                    },
                    onDragCancel = {
                        scope.launch { settle(null) }
                    }
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(C.Surface)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column {
                Text(Format.dateTime(item.timeMs), color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(3.dp))
                val bits = mutableListOf<String>()
                Format.location(item)?.let { bits.add(it) }
                bits.add(Format.size(item.sizeBytes))
                if (item.isVideo && item.durationMs > 0) bits.add(Format.duration(item.durationMs))
                Text(bits.joinToString(" · "), color = Color(0xDDFFFFFF), fontSize = 11.sp)
            }
        }

        if (progressX < 0.05f && SwipeAction.KEEP in enabledActions) {
            DirectionStamp(
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                icon = Icons.Rounded.Favorite,
                label = "保留",
                color = C.Keep,
                bg = C.KeepBg,
                alpha = (-progressX).coerceIn(0f, 1f)
            )
        }
        if (progressX > 0.05f && SwipeAction.TRASH in enabledActions) {
            DirectionStamp(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                icon = Icons.Rounded.Delete,
                label = "删除",
                color = C.Delete,
                bg = C.DeleteBg,
                alpha = progressX.coerceIn(0f, 1f)
            )
        }
        if (progressY > 0.05f && SwipeAction.PENDING in enabledActions) {
            DirectionStamp(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp),
                icon = Icons.Rounded.Schedule,
                label = "待定",
                color = C.Pending,
                bg = C.PendingBg,
                alpha = progressY.coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
private fun DirectionStamp(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    bg: Color,
    alpha: Float
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg.copy(alpha = 0.55f + 0.45f * alpha))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .rotate(if (label == "保留") -12f else if (label == "删除") 12f else 0f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** 卡片下方的静态待定投放区 */
@Composable
fun PendingDropZone(active: Boolean, count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) C.PendingBg else C.Surface)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = C.Pending, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "下滑到这里 = 待定" + if (count > 0) "　·　待定区 $count 张" else "",
                color = C.Pending,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
