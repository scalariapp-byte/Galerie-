package com.galerie.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberSmartItems(vm: GalleryViewModel): List<SmartItem> {
    val counts by vm.smartCounts.collectAsState()
    val trash by vm.trash.collectAsState()
    return buildList {
        SmartKind.entries.forEach { k ->
            val n = counts[k]?.first ?: 0
            if (n > 0 || k == SmartKind.FAVORITES || k == SmartKind.WALLPAPERS) {
                add(SmartItem(smartIcon(k), k.short, n, smartColor(k)) {
                    vm.push(if (k == SmartKind.WALLPAPERS) Screen.Wallpapers else Screen.Collection(Source.Smart(k), k.label))
                })
            }
        }
        add(SmartItem(Icons.Filled.Delete, "Corbeille", trash.size, Color(0xFFFF6B6B)) { vm.push(Screen.Collection(Source.Trash, "Corbeille")) })
    }
}

/**
 * Panneau latéral des collections : on tire la languette depuis le bord de l'écran.
 * La zone de la languette est exclue du geste « retour » d'Android.
 */
@Composable
fun BoxScope.EdgePanel(vm: GalleryViewModel) {
    val settings by vm.settings.collectAsState()
    val left = settings.panelLeft
    val side = if (left) -1f else 1f
    val items = rememberSmartItems(vm)
    val density = LocalDensity.current
    val panelW = 88.dp
    val hiddenPx = with(density) { (panelW + 24.dp).toPx() }
    // 0 = ouvert, hiddenPx = caché
    val hidden = remember(hiddenPx) { Animatable(hiddenPx) }
    val scope = rememberCoroutineScope()
    val progress = (1f - hidden.value / hiddenPx).coerceIn(0f, 1f)
    val align = if (left) Alignment.CenterStart else Alignment.CenterEnd
    val maxPanelH = (LocalConfiguration.current.screenHeightDp * 0.72f).dp

    fun settle(open: Boolean) {
        scope.launch { hidden.animateTo(if (open) 0f else hiddenPx, spring(dampingRatio = 0.82f, stiffness = 520f)) }
    }

    val drag = Modifier.pointerInput(left, hiddenPx) {
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = { tracker.resetTracking() },
            onDragEnd = {
                val v = tracker.calculateVelocity().x * side // > 0 : vers la fermeture
                val p = 1f - hidden.value / hiddenPx
                settle(open = v < -500f || (v <= 500f && p > 0.4f))
            },
            onDragCancel = { settle(open = hidden.value < hiddenPx / 2) },
        ) { change, dx ->
            change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
            scope.launch { hidden.snapTo((hidden.value + dx * side).coerceIn(0f, hiddenPx)) }
        }
    }

    BackHandler(enabled = progress > 0.5f) { settle(false) }

    if (progress > 0f) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * progress))
                .pointerInput(Unit) { detectTapGestures { settle(false) } }
                .then(drag),
        )
    }

    // Languette + zone de saisie au bord
    Box(
        Modifier.align(align).offset(y = (-40).dp).width(28.dp).height(190.dp)
            .systemGestureExclusion()
            .pointerInput(Unit) { detectTapGestures { settle(true) } }
            .then(drag),
    ) {
        Box(
            Modifier.align(align).padding(horizontal = 4.dp).width(5.dp).height(58.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * (1f - progress)), CircleShape),
        )
    }

    // Panneau
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
        shadowElevation = if (progress > 0f) 14.dp else 0.dp,
        modifier = Modifier.align(align).padding(horizontal = 10.dp).offset(y = (-24).dp)
            .offset { IntOffset((hidden.value * side).roundToInt(), 0) }
            .width(panelW).heightIn(max = maxPanelH)
            .then(drag),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { s -> PanelItem(s.icon, s.label, s.count, s.color) { settle(false); s.open() } }
            HorizontalDivider(Modifier.width(40.dp), color = MaterialTheme.colorScheme.outlineVariant)
            PanelItem(Icons.Filled.CleaningServices, "Espace", -1, MaterialTheme.colorScheme.primary) { settle(false); vm.push(Screen.Cleanup) }
            PanelItem(Icons.Filled.Settings, "Réglages", -1, MaterialTheme.colorScheme.onSurfaceVariant) { settle(false); vm.push(Screen.SettingsScreen) }
        }
    }
}

@Composable
private fun PanelItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).background(color.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = fixedSp(11f), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        if (count >= 0) Text(if (count > 0) "$count" else "–", fontSize = fixedSp(10f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
