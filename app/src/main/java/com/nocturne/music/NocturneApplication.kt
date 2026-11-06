package com.nocturne.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.nocturne.music.playback.AudioArtworkFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NocturneApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AudioArtworkFetcher.Factory(this@NocturneApplication))
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork_cache"))
                    .maxSizeBytes(128L * 1024L * 1024L) // 128 MB artwork cache
                    .build()
            }
            .crossfade(250)
            .respectCacheHeaders(false)
            .build()
    }
}
