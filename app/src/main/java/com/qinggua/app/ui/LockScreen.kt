package com.qinggua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 解锁页：点击后用系统指纹/面容验证 */
@Composable
fun LockScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(50))
                    .background(C.KeepBg)
                    .clickable { onUnlockClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Fingerprint, "解锁", tint = C.Keep, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("轻触指纹解锁", color = C.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "用手机锁屏的指纹或面容打开清瓜",
                color = C.Ink2, fontSize = 13.sp
            )
            Spacer(Modifier.height(40.dp))
            Text(
                "验证由系统完成，清瓜不保存你的生物信息",
                color = C.Ink3, fontSize = 11.sp
            )
        }
    }
}
