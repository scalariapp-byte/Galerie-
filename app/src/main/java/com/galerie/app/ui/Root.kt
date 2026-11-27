package com.galerie.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class Access { NONE, PARTIAL, FULL }

object Perms {
    val request: Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, Manifest.permission.ACCESS_MEDIA_LOCATION,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.ACCESS_MEDIA_LOCATION,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION)
    }

    private fun granted(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun access(ctx: Context): Access = when {
        Build.VERSION.SDK_INT >= 33 && granted(ctx, Manifest.permission.READ_MEDIA_IMAGES) && granted(ctx, Manifest.permission.READ_MEDIA_VIDEO) -> Access.FULL
        Build.VERSION.SDK_INT < 33 && granted(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) -> Access.FULL
        Build.VERSION.SDK_INT >= 34 && granted(ctx, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Access.PARTIAL
        Build.VERSION.SDK_INT >= 33 && (granted(ctx, Manifest.permission.READ_MEDIA_IMAGES) || granted(ctx, Manifest.permission.READ_MEDIA_VIDEO)) -> Access.PARTIAL
        else -> Access.NONE
    }

    fun hasLocation(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_MEDIA_LOCATION)

    fun canManageMedia(ctx: Context) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(ctx)
}

@Composable
fun Root(vm: GalleryViewModel) {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(Perms.access(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = Perms.access(ctx)
        if (access != Access.NONE) vm.start()
    }
    LifecycleResumeEffect(Unit) {
        access = Perms.access(ctx)
        if (access != Access.NONE) vm.start()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { if (access == Access.NONE) launcher.launch(Perms.request) }

    if (access == Access.NONE) {
        PermissionScreen(onGrant = { launcher.launch(Perms.request) }, onSettings = { Actions.appSettings(ctx) })
    } else {
        Navigation(vm, partial = access == Access.PARTIAL, onRequestMore = { launcher.launch(Perms.request) })
    }
}

@Composable
private fun Navigation(vm: GalleryViewModel, partial: Boolean, onRequestMore: () -> Unit) {
    val holder = rememberSaveableStateHolder()
    val keys = vm.stack.mapIndexed { i, s -> "$i:$s" }
    var previous by remember { mutableStateOf(keys) }
    LaunchedEffect(keys) {
        (previous - keys.toSet()).forEach { holder.removeState(it) }
        previous = keys
    }
    BackHandler(enabled = vm.selection.isNotEmpty() || vm.stack.size > 1) {
        if (vm.selection.isNotEmpty()) vm.selection = emptySet() else vm.pop()
    }
    val target = keys.last() to vm.stack.last()
    AnimatedContent(
        targetState = target,
        contentKey = { it.first },
        transitionSpec = { fadeIn(tween(170)) togetherWith fadeOut(tween(110)) },
        label = "nav",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) { (key, screen) ->
        holder.SaveableStateProvider(key) {
            when (screen) {
                Screen.Home -> HomeScreen(vm, partial, onRequestMore)
                is Screen.Collection -> CollectionScreen(vm, screen.source, screen.title)
                is Screen.Viewer -> ViewerScreen(vm, screen.source, screen.startId)
                Screen.SettingsScreen -> SettingsScreen(vm)
                Screen.Cleanup -> CleanupScreen(vm)
                Screen.Search -> SearchScreen(vm)
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: GalleryViewModel, partial: Boolean, onRequestMore: () -> Unit) {
    val ctx = LocalContext.current
    val tabs = remember { rememberSaveableStateHolderKeyed() }
    val holder = rememberSaveableStateHolder()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPad = PaddingValues(bottom = navInset + 72.dp)
    val tab = vm.tab.coerceIn(0, 1)
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Box(Modifier.fillMaxSize()) {
        GlowBackground()
        holder.SaveableStateProvider(tabs[tab]) {
            when (tab) {
                0 -> PhotosTab(vm, contentPad)
                else -> AlbumsTab(vm, contentPad)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            if (vm.selection.isNotEmpty()) {
                Box(Modifier.navigationBarsPadding().padding(bottom = 10.dp)) { SelectionBottomBar(vm, trashMode = false) }
            } else {
                if (partial) {
                    Surface(
                        onClick = onRequestMore, shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            "Accès limité · Tout autoriser", style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                BottomNav(tab, onSelect = { vm.tab = it })
            }
        }
    }
    }
}

@Composable
private fun FloatingNav(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        shadowElevation = 10.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NavPill(Icons.Outlined.Image, "Photos", selected == 0) { onSelect(0) }
            NavPill(Icons.Outlined.PhotoAlbum, "Albums", selected == 1) { onSelect(1) }
        }
    }
}

@Composable
private fun NavPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, label = "bg")
    val fg by animateColorAsState(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "fg")
    Row(
        Modifier.clip(CircleShape).background(bg).clickable(onClick = onClick)
            .animateContentSize().height(46.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(22.dp))
        if (active) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = fg, fontSize = fixedSp(15f), fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun rememberSaveableStateHolderKeyed() = listOf("tab-photos", "tab-albums")

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text("Vos photos et vidéos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Galerie a besoin d'accéder à vos médias pour les afficher. Rien ne quitte votre téléphone.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onGrant) { Text("Autoriser l'accès") }
        TextButton(onSettings) { Text("Ouvrir les paramètres") }
    }
}
