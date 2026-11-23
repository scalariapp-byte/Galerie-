package com.galerie.app.data

import android.net.Uri

data class Media(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mime: String,
    val isVideo: Boolean,
    /** Date de capture en ms (repli sur la date de modification). */
    val date: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val duration: Long,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String,
    val isFavorite: Boolean,
    /** Pour la corbeille : date de suppression définitive (ms). */
    val dateExpires: Long,
) {
    val pixels: Long get() = width.toLong() * height
    val isGif: Boolean get() = mime == "image/gif"
    val isScreenshot: Boolean
        get() = relativePath.contains("screenshot", true) || bucketName.contains("screenshot", true) ||
            bucketName.contains("capture", true) || relativePath.contains("screen", true)
    val isPanorama: Boolean
        get() = !isVideo && width > 0 && height > 0 &&
            maxOf(width, height).toFloat() / minOf(width, height) >= 2.2f
    val isHd: Boolean get() = if (isVideo) pixels >= 1920L * 1080 else pixels >= 12_000_000L
}

data class Album(
    val id: Long,
    val name: String,
    val relativePath: String,
    val cover: Media,
    val count: Int,
    val size: Long,
    val latest: Long,
)
