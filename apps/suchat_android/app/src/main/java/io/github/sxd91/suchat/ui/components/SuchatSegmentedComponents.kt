package io.github.sxd91.suchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Identifies a row's visual edge within a connected segmented group. */
enum class SuchatSegmentPosition { Single, Top, Middle, Bottom }

/**
 * Content used by [SuchatSegmentedColumn]. Icons and custom trailing content are composable
 * slots so callers can use their preferred icon library without coupling this component to one.
 */
data class SuchatSegmentedItem(
    val title: String,
    val description: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showChevron: Boolean = true,
    val onClick: (() -> Unit)? = null,
)

/**
 * A continuous Material-themed list with correctly rounded top, middle, and bottom rows.
 *
 * This implementation is original Compose code and deliberately relies only on MaterialTheme's
 * semantic color roles, making it suitable for dynamic color and light/dark themes.
 */
@Composable
fun SuchatSegmentedColumn(
    items: List<SuchatSegmentedItem>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            SuchatBaseWidget(
                title = item.title,
                description = item.description,
                icon = item.icon,
                trailingContent = item.trailingContent,
                showChevron = item.showChevron,
                onClick = item.onClick,
                position = items.segmentPositionAt(index),
                cornerRadius = cornerRadius,
            )
            if (index != items.lastIndex) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}

/** A single setting-style row that can be connected to neighboring segmented rows. */
@Composable
fun SuchatBaseWidget(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    position: SuchatSegmentPosition = SuchatSegmentPosition.Single,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
) {
    val colors = MaterialTheme.colorScheme
    val rowModifier = modifier
        .fillMaxWidth()
        .clip(segmentShape(position, cornerRadius))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = rowModifier,
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        shape = segmentShape(position, cornerRadius),
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (trailingContent != null) trailingContent()
            if (showChevron) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun List<*>.segmentPositionAt(index: Int): SuchatSegmentPosition = when (size) {
    1 -> SuchatSegmentPosition.Single
    else -> when (index) {
        0 -> SuchatSegmentPosition.Top
        lastIndex -> SuchatSegmentPosition.Bottom
        else -> SuchatSegmentPosition.Middle
    }
}

private fun segmentShape(position: SuchatSegmentPosition, radius: Dp): Shape = when (position) {
    SuchatSegmentPosition.Single -> RoundedCornerShape(radius)
    SuchatSegmentPosition.Top -> RoundedCornerShape(topStart = radius, topEnd = radius)
    SuchatSegmentPosition.Middle -> RoundedCornerShape(0.dp)
    SuchatSegmentPosition.Bottom -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
}
