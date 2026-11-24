package com.galerie.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.galerie.app.data.Media

/** Intents vers d'autres applications (partage, édition, fond d'écran, carte…). */
object Actions {
    private fun Context.launch(intent: Intent, title: String) {
        runCatching { startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(this, "Aucune application disponible", Toast.LENGTH_SHORT).show() }
    }

    fun share(ctx: Context, items: List<Media>) {
        if (items.isEmpty()) return
        val type = when {
            items.all { it.isVideo } -> "video/*"
            items.none { it.isVideo } -> "image/*"
            else -> "*/*"
        }
        val intent = if (items.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, items[0].uri)
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
        }
        intent.type = type
        intent.clipData = ClipData.newRawUri("", items[0].uri).apply { items.drop(1).forEach { addItem(ClipData.Item(it.uri)) } }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.launch(intent, "Partager")
    }

    fun shareUri(ctx: Context, uri: Uri, type: String?) {
        val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri).setType(type ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.launch(intent, "Partager")
    }

    fun edit(ctx: Context, m: Media) {
        val intent = Intent(Intent.ACTION_EDIT).setDataAndType(m.uri, m.mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        ctx.launch(intent, "Modifier avec")
    }

    fun setAs(ctx: Context, m: Media) {
        val intent = Intent(Intent.ACTION_ATTACH_DATA).setDataAndType(m.uri, m.mime)
            .putExtra("mimeType", m.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.launch(intent, "Définir comme")
    }

    fun openWith(ctx: Context, m: Media) {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(m.uri, m.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.launch(intent, "Ouvrir avec")
    }

    fun openMap(ctx: Context, lat: Double, lon: Double) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon"))
        ctx.launch(intent, "Ouvrir la carte")
    }

    fun camera(ctx: Context) {
        val intents = listOf(
            Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
        )
        for (i in intents) {
            if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
        Toast.makeText(ctx, "Aucune application appareil photo", Toast.LENGTH_SHORT).show()
    }

    fun appSettings(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun manageMedia(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { appSettings(ctx) }
    }
}
