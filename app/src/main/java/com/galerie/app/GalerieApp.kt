package com.galerie.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import com.galerie.app.data.ThumbFetcher
import com.galerie.app.data.ThumbKeyer

class GalerieApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(ThumbKeyer())
            add(ThumbFetcher.Factory(contentResolver))
            add(ImageDecoderDecoder.Factory()) // GIF / WebP animés
        }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.30).build() }
        .crossfade(false)
        .build()
}
