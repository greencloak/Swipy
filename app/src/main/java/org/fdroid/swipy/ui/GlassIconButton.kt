package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A frosted-glass circular icon button: a mostly-solid translucent
 * background (~75% opaque, so it reads as glass rather than a faint
 * outline) with a crisp, nearly fully opaque icon glyph on top so it stays
 * legible over any video or photo.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .background(Color.Black.copy(alpha = 0.30f), CircleShape)
                .background(Color.White.copy(alpha = 0.22f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
