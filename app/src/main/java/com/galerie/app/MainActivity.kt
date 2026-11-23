package com.galerie.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.galerie.app.ui.ExternalViewer
import com.galerie.app.ui.GalerieTheme
import com.galerie.app.ui.GalleryViewModel
import com.galerie.app.ui.Root
import com.galerie.app.ui.ThemeMode
import com.galerie.app.ui.UiEvent

class MainActivity : ComponentActivity() {
    private val vm: GalleryViewModel by viewModels()

    private val senderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.onIntentResult(it.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val external = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        val externalType = intent?.type ?: external?.let { contentResolver.getType(it) }

        setContent {
            val settings by vm.settings.collectAsState()
            GalerieTheme(settings.theme, settings.dynamicColor) {
                val dark = when (settings.theme) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    else -> true
                }
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                LaunchedEffect(Unit) {
                    vm.events.collect { e ->
                        when (e) {
                            is UiEvent.Sender -> runCatching { senderLauncher.launch(IntentSenderRequest.Builder(e.sender).build()) }
                            is UiEvent.Toast -> Toast.makeText(this@MainActivity, e.text, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (external != null) ExternalViewer(external, externalType, onBack = { finish() })
                else Root(vm)
            }
        }
    }
}
