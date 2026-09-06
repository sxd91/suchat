package io.github.sxd91.suchat.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sxd91.suchat.feature.media.model.MediaAsset
import io.github.sxd91.suchat.feature.media.model.SelectionChange
import io.github.sxd91.suchat.feature.media.model.SelectionState

@Composable
fun PreviewMediaPicker(request: MediaPickerRequest, assets: List<MediaAsset> = PreviewMediaFixtures.mediaAssets, onComplete: (List<MediaAsset>) -> Unit) {
    val selection = remember(request.maxSelection) { SelectionState<MediaAsset>(request.maxSelection) }
    var notice by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("选择图片", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("已选 " + selection.count + " / " + request.maxSelection, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(request.title, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(assets, key = { it.id }) { asset ->
                val chosen = selection.isSelected(asset)
                val locked = selection.isAtLimit && !chosen
                Surface(shape = RoundedCornerShape(18.dp), color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth().height(108.dp).alpha(if (locked) .42f else 1f).clickable(enabled = !locked || chosen) {
                    when (val change = selection.toggle(asset)) {
                        is SelectionChange.LimitReached -> notice = "最多选择 " + change.selectionLimit + " 项"
                        else -> notice = ""
                    }
                }) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(asset.displayName.take(2), style = MaterialTheme.typography.titleLarge)
                        selection.selectionIndex(asset)?.let { order ->
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text(order.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
        if (notice.isNotEmpty()) Text(notice, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("□ 原图", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled = selection.count >= request.minSelection, onClick = { onComplete(selection.items.toList()) }) { Text("完成 (" + selection.count + ")") }
        }
    }
}
