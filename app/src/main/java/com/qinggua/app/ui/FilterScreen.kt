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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/** 筛选页：排序 + 相册 + 日期 */
@Composable
fun FilterScreen(vm: AppViewModel, onBack: () -> Unit) {
    val f by vm.filter.collectAsState()
    val albums = vm.albums()
    val months = vm.months()

    Column(modifier = Modifier.fillMaxSize().background(C.Page)) {
        ScreenHeader("筛选", "筛选后只滑符合条件的照片") {
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
            SectionTitle("排序")
            Row {
                Chip("最新在前", f.sortNewestFirst) {
                    vm.updateFilter { it.copy(sortNewestFirst = true) }
                }
                Spacer(Modifier.width(12.dp))
                Chip("最老在前", !f.sortNewestFirst) {
                    vm.updateFilter { it.copy(sortNewestFirst = false) }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("相册")
            ChipFlow(
                options = listOf("全部" to null) + albums.map { it to it },
                selected = f.bucketName
            ) { value -> vm.updateFilter { it.copy(bucketName = value) } }

            Spacer(Modifier.height(22.dp))
            SectionTitle("日期")
            ChipFlow(
                options = listOf("全部时间" to null) + months.map { it to it },
                selected = f.yearMonth
            ) { value -> vm.updateFilter { it.copy(yearMonth = value) } }

            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.KeepBg)
                    .clickable { onBack() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("完成", color = C.Keep, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = C.Ink2, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) C.InfoBg else C.Surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (active) C.Info else C.Ink2, fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
private fun ChipFlow(
    options: List<Pair<String, String?>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column {
        options.chunked(3).forEach { row ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                row.forEach { (label, value) ->
                    Chip(label, selected == value) { onSelect(value) }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}
