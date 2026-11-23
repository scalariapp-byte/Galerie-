package com.galerie.app.data

import android.content.ContentResolver
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options

/** Taille unique des miniatures : maximise les hits du cache mémoire (grille + placeholder visionneuse). */
const val THUMB_PX = 384

/**
 * Miniature servie par le cache de MediaStore (ContentResolver.loadThumbnail) :
 * pas de décodage de la photo complète, d'où la vitesse de défilement.
 */
data class Thumb(val uri: Uri, val modified: Long, val px: Int = THUMB_PX)

fun thumbKey(uri: Uri, modified: Long, px: Int = THUMB_PX) = "thumb:$uri:$modified:$px"

class ThumbKeyer : Keyer<Thumb> {
    override fun key(data: Thumb, options: Options): String = thumbKey(data.uri, data.modified, data.px)
}

class ThumbFetcher(
    private val data: Thumb,
    private val options: Options,
    private val resolver: ContentResolver,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bmp = resolver.loadThumbnail(data.uri, Size(data.px, data.px), null)
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bmp),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val resolver: ContentResolver) : Fetcher.Factory<Thumb> {
        override fun create(data: Thumb, options: Options, imageLoader: ImageLoader): Fetcher =
            ThumbFetcher(data, options, resolver)
    }
}
