package com.galerie.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.provider.MediaStore.MediaColumns
import androidx.exifinterface.media.ExifInterface
import com.galerie.app.ui.Format
import kotlin.math.roundToInt

class MediaRepository(context: Context) {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    val filesUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    private val imagesUri: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    private val videosUri: Uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private val projection = arrayOf(
        BaseColumns._ID,
        MediaColumns.DISPLAY_NAME,
        MediaColumns.MIME_TYPE,
        FileColumns.MEDIA_TYPE,
        MediaColumns.DATE_TAKEN,
        MediaColumns.DATE_ADDED,
        MediaColumns.DATE_MODIFIED,
        MediaColumns.SIZE,
        MediaColumns.WIDTH,
        MediaColumns.HEIGHT,
        MediaColumns.ORIENTATION,
        MediaColumns.DURATION,
        MediaColumns.BUCKET_ID,
        MediaColumns.BUCKET_DISPLAY_NAME,
        MediaColumns.RELATIVE_PATH,
        MediaColumns.IS_FAVORITE,
        MediaColumns.DATE_EXPIRES,
    )

    /** Une seule requête pour toutes les photos et vidéos (ou uniquement la corbeille). */
    fun load(trashed: Boolean): List<Media> {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${FileColumns.MEDIA_TYPE} IN (?,?)")
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(FileColumns.MEDIA_TYPE_IMAGE.toString(), FileColumns.MEDIA_TYPE_VIDEO.toString()),
            )
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${MediaColumns.DATE_MODIFIED} DESC")
            if (trashed) putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }
        val out = ArrayList<Media>(2048)
        runCatching {
            resolver.query(filesUri, projection, args, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(BaseColumns._ID)
                val iName = c.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
                val iMime = c.getColumnIndexOrThrow(MediaColumns.MIME_TYPE)
                val iType = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
                val iTaken = c.getColumnIndexOrThrow(MediaColumns.DATE_TAKEN)
                val iAdded = c.getColumnIndexOrThrow(MediaColumns.DATE_ADDED)
                val iMod = c.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
                val iSize = c.getColumnIndexOrThrow(MediaColumns.SIZE)
                val iW = c.getColumnIndexOrThrow(MediaColumns.WIDTH)
                val iH = c.getColumnIndexOrThrow(MediaColumns.HEIGHT)
                val iOr = c.getColumnIndexOrThrow(MediaColumns.ORIENTATION)
                val iDur = c.getColumnIndexOrThrow(MediaColumns.DURATION)
                val iBucket = c.getColumnIndexOrThrow(MediaColumns.BUCKET_ID)
                val iBucketName = c.getColumnIndexOrThrow(MediaColumns.BUCKET_DISPLAY_NAME)
                val iPath = c.getColumnIndexOrThrow(MediaColumns.RELATIVE_PATH)
                val iFav = c.getColumnIndexOrThrow(MediaColumns.IS_FAVORITE)
                val iExp = c.getColumnIndexOrThrow(MediaColumns.DATE_EXPIRES)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val isVideo = c.getInt(iType) == FileColumns.MEDIA_TYPE_VIDEO
                    val modified = c.getLong(iMod) * 1000
                    val added = c.getLong(iAdded) * 1000
                    val taken = c.getLong(iTaken)
                    var w = c.getInt(iW)
                    var h = c.getInt(iH)
                    val orientation = c.getInt(iOr)
                    if (orientation == 90 || orientation == 270) { val t = w; w = h; h = t }
                    out += Media(
                        id = id,
                        uri = ContentUris.withAppendedId(if (isVideo) videosUri else imagesUri, id),
                        name = c.getString(iName) ?: "",
                        mime = c.getString(iMime) ?: if (isVideo) "video/*" else "image/*",
                        isVideo = isVideo,
                        date = when { taken > 0 -> taken; modified > 0 -> modified; else -> added },
                        dateAdded = added,
                        dateModified = modified,
                        size = c.getLong(iSize),
                        width = w,
                        height = h,
                        duration = c.getLong(iDur),
                        bucketId = c.getLong(iBucket),
                        bucketName = c.getString(iBucketName) ?: "Sans nom",
                        relativePath = c.getString(iPath) ?: "",
                        isFavorite = c.getInt(iFav) == 1,
                        dateExpires = c.getLong(iExp) * 1000,
                    )
                }
            }
        }
        return out
    }

    // ---- Demandes d'autorisation système (Android 11+) ----
    fun trashRequest(items: List<Media>, trash: Boolean): IntentSender =
        MediaStore.createTrashRequest(resolver, items.map { it.uri }, trash).intentSender

    fun deleteRequest(items: List<Media>): IntentSender =
        MediaStore.createDeleteRequest(resolver, items.map { it.uri }).intentSender

    fun favoriteRequest(items: List<Media>, fav: Boolean): IntentSender =
        MediaStore.createFavoriteRequest(resolver, items.map { it.uri }, fav).intentSender

    fun writeRequest(items: List<Media>): IntentSender =
        MediaStore.createWriteRequest(resolver, items.map { it.uri }).intentSender

    // ---- Opérations ----
    fun move(items: List<Media>, relativePath: String): Int = items.count { m ->
        runCatching {
            resolver.update(m.uri, ContentValues().apply { put(MediaColumns.RELATIVE_PATH, relativePath) }, null, null) > 0
        }.getOrDefault(false)
    }

    fun rename(m: Media, newName: String): Boolean = runCatching {
        resolver.update(m.uri, ContentValues().apply { put(MediaColumns.DISPLAY_NAME, newName) }, null, null) > 0
    }.getOrDefault(false)

    fun copy(items: List<Media>, relativePath: String): Int = items.count { m ->
        runCatching {
            val collection = if (m.isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaColumns.DISPLAY_NAME, m.name)
                put(MediaColumns.MIME_TYPE, m.mime)
                put(MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaColumns.DATE_TAKEN, m.date)
                put(MediaColumns.IS_PENDING, 1)
            }
            val dst = resolver.insert(collection, values) ?: return@runCatching false
            try {
                resolver.openInputStream(m.uri)!!.use { input ->
                    resolver.openOutputStream(dst)!!.use { output -> input.copyTo(output, 1 shl 16) }
                }
                resolver.update(dst, ContentValues().apply { put(MediaColumns.IS_PENDING, 0) }, null, null)
                true
            } catch (e: Exception) {
                resolver.delete(dst, null, null)
                false
            }
        }.getOrDefault(false)
    }

    // ---- Détails / EXIF ----
    data class Details(val rows: List<Pair<String, String>>, val lat: Double?, val lon: Double?)

    fun details(m: Media, withLocation: Boolean): Details {
        val rows = mutableListOf(
            "Nom" to m.name,
            "Date" to Format.fullDate(m.date),
            "Taille" to Format.size(m.size),
        )
        if (m.width > 0) rows += "Résolution" to "${m.width} × ${m.height}" +
            if (!m.isVideo) "  (${"%.1f".format(m.pixels / 1_000_000.0)} Mpx)" else ""
        if (m.isVideo && m.duration > 0) rows += "Durée" to Format.duration(m.duration)
        rows += "Album" to m.bucketName
        rows += "Emplacement" to "/${m.relativePath}${m.name}"
        rows += "Type" to m.mime
        var lat: Double? = null
        var lon: Double? = null
        if (!m.isVideo) {
            val exif = openExif(m, withLocation)
            if (exif != null) {
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val device = if (model.startsWith(make, true)) model else "$make $model".trim()
                if (device.isNotBlank()) rows += "Appareil" to device
                val parts = mutableListOf<String>()
                exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0 }?.let { parts += "ƒ/%.1f".format(it) }
                exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }?.let {
                    parts += if (it < 1) "1/${(1 / it).roundToInt()} s" else "%.1f s".format(it)
                }
                exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 }?.let { parts += "ISO $it" }
                exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }?.let { parts += "%.1f mm".format(it) }
                if (parts.isNotEmpty()) rows += "Prise de vue" to parts.joinToString("  ·  ")
                exif.latLong?.let { lat = it[0]; lon = it[1]; rows += "Position" to "%.5f, %.5f".format(it[0], it[1]) }
            }
        }
        return Details(rows, lat, lon)
    }

    private fun openExif(m: Media, withLocation: Boolean): ExifInterface? {
        val uris = if (withLocation) listOf(MediaStore.setRequireOriginal(m.uri), m.uri) else listOf(m.uri)
        for (u in uris) {
            val exif = runCatching { resolver.openInputStream(u)?.use { ExifInterface(it) } }.getOrNull()
            if (exif != null) return exif
        }
        return null
    }
}
