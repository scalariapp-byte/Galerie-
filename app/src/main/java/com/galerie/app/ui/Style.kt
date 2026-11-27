package com.galerie.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.math.roundToInt

val GradStart = Color(0xFFA77BFF)
val GradEnd = Color(0xFF5B2EE0)
val VioletGradient get() = Brush.linearGradient(listOf(GradStart, GradEnd))

/** Fond sombre avec halos et courbes violettes lumineuses. */
@Composable
fun GlowBackground(modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val p = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxSize()) {
        drawRect(bg)
        val w = size.width
        val h = size.height
        drawCircle(Brush.radialGradient(listOf(p.copy(alpha = 0.30f), Color.Transparent), Offset(w * 1.05f, -h * 0.02f), w * 0.85f), w * 0.85f, Offset(w * 1.05f, -h * 0.02f))
        drawCircle(Brush.radialGradient(listOf(GradEnd.copy(alpha = 0.32f), Color.Transparent), Offset(-w * 0.1f, h * 1.02f), w * 0.95f), w * 0.95f, Offset(-w * 0.1f, h * 1.02f))
        drawCircle(Brush.radialGradient(listOf(p.copy(alpha = 0.22f), Color.Transparent), Offset(w * 1.1f, h * 0.95f), w * 0.7f), w * 0.7f, Offset(w * 1.1f, h * 0.95f))
        fun curve(x0: Float, y0: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, x1: Float, y1: Float, a: Float, sw: Float) {
            val path = Path().apply { moveTo(x0, y0); cubicTo(cx1, cy1, cx2, cy2, x1, y1) }
            drawPath(path, Brush.linearGradient(listOf(Color.Transparent, p.copy(alpha = a), Color.Transparent), Offset(x0, y0), Offset(x1, y1)), style = Stroke(sw))
        }
        curve(w * 0.45f, 0f, w * 0.65f, h * 0.06f, w * 0.85f, h * 0.10f, w * 1.05f, h * 0.06f, 0.55f, 3f)
        curve(w * 0.62f, -h * 0.01f, w * 0.80f, h * 0.05f, w * 0.95f, h * 0.15f, w * 1.05f, h * 0.20f, 0.35f, 2f)
        curve(-w * 0.05f, h * 0.86f, w * 0.30f, h * 0.80f, w * 0.65f, h * 0.98f, w * 1.05f, h * 0.88f, 0.45f, 3f)
        curve(w * 0.55f, h * 1.01f, w * 0.75f, h * 0.90f, w * 0.90f, h * 0.84f, w * 1.05f, h * 0.83f, 0.6f, 2.5f)
    }
}

@Composable
fun AppIconTile(size: Dp = 50.dp) {
    Box(
        Modifier.size(size).shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = GradStart, spotColor = GradStart)
            .clip(RoundedCornerShape(16.dp)).background(VioletGradient)
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Image, null, tint = Color.White, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 8.dp).size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.75f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun GlowSearchBar(onClick: () -> Unit) {
    val p = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(p.copy(alpha = 0.9f), p.copy(alpha = 0.25f), p.copy(alpha = 0.9f)))),
        modifier = Modifier.fillMaxWidth().height(52.dp).shadow(14.dp, CircleShape, ambientColor = p, spotColor = p),
    ) {
        Row(Modifier.padding(start = 18.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text("Rechercher des photos…", fontSize = fixedSp(15f), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
            Box(Modifier.size(42.dp).clip(CircleShape).background(VioletGradient), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * En-tête « Galerie » qui se replie quand on descend et revient quand on remonte.
 * content reçoit la hauteur de l'en-tête à réserver en haut de la liste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderScaffold(
    vm: GalleryViewModel,
    title: String,
    subtitle: String,
    selectionItems: List<com.galerie.app.data.Media>,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable (Dp) -> Unit,
) {
    val scroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var headerPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize()) {
        content(with(density) { headerPx.toDp() })
        if (vm.selection.isNotEmpty()) {
            Box(Modifier.background(bg.copy(alpha = 0.95f))) { SelectionTopBar(vm, selectionItems) }
        } else {
            Column(
                Modifier.fillMaxWidth()
                    .onSizeChanged { headerPx = it.height; scroll.state.heightOffsetLimit = -it.height.toFloat() }
                    .offset { IntOffset(0, scroll.state.heightOffset.roundToInt()) }
                    .background(Brush.verticalGradient(0f to bg.copy(alpha = 0.92f), 0.75f to bg.copy(alpha = 0.80f), 1f to Color.Transparent))
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = fixedSp(28f), fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subtitle, fontSize = fixedSp(14f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    actions()
                }
            }
        }
    }
}

private data class Tab(val icon: ImageVector, val label: String)

@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Tab(Icons.Filled.Image, "Photos"),
        Tab(Icons.Outlined.PlayCircle, "Vidéos"),
        Tab(Icons.Outlined.Folder, "Albums"),
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                tabs.forEachIndexed { i, t -> NavTab(t, i == selected, Modifier.weight(1f)) { onSelect(i) } }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun NavTab(t: Tab, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "c")
    val indicator by animateFloatAsState(if (active) 1f else 0f, label = "i")
    Column(
        modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Icon(t.icon, t.label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(t.label, fontSize = fixedSp(12f), fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = color, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.width(30.dp * indicator).height(3.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = indicator)),
        )
        Spacer(Modifier.height(4.dp))
    }
}
