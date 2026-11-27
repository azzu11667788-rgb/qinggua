package com.qinggua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinggua.app.data.AppViewModel

/** 设置页 */
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        ScreenHeader("设置", "清瓜 · 版本 1.0") {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = C.Ink2)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            SettingsCard {
                ToggleRow(
                    "隐私锁",
                    "打开时用手机锁屏的指纹或面容解锁",
                    s.unlockEnabled
                ) { vm.updateSettings { it.copy(unlockEnabled = it.unlockEnabled.not()) } }
                Divider()
                ChoiceRow(
                    "手势灵敏度",
                    currentLabel = when (s.gestureSensitivity) { 0.5f -> "低"; 0.2f -> "高"; else -> "中" },
                    listOf("低" to 0.5f, "中" to 0.35f, "高" to 0.2f)
                ) { _, value ->
                    vm.updateSettings { it.copy(gestureSensitivity = value as Float) }
                }
                Divider()
                ChoiceRow(
                    "撤销步数",
                    currentLabel = s.undoLimit.toString(),
                    listOf("10" to 10, "20" to 20, "50" to 50)
                ) { _, value ->
                    vm.updateSettings { it.copy(undoLimit = value as Int) }
                }
                Divider()
                ToggleRow(
                    "删除前二次确认",
                    "回收箱真删时再弹一次确认",
                    s.trashConfirm
                ) { vm.updateSettings { it.copy(trashConfirm = it.trashConfirm.not()) } }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "所有整理记录只保存在这台手机上，不上传、不登录。",
                color = C.Ink3, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.Surface)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange() }.padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = C.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = C.Ink3, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = C.Keep
            )
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    currentLabel: String,
    options: List<Pair<String, *>>,
    onPick: (String, Any) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = C.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(currentLabel, color = C.Ink3, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            options.forEach { (label, value) ->
                val active = label == currentLabel
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) C.KeepBg else C.Page)
                        .clickable { onPick(label, value as Any) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(label, color = if (active) C.Keep else C.Ink2, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun Divider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(C.Line)
    )
}
