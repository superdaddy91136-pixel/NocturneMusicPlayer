package com.nocturne.music.playback

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

class AudioArtworkFetcher(
    private val uri: Uri,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val bitmap = context.contentResolver.loadThumbnail(uri, Size(800, 800), null)
                    return@withContext DrawableResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                } catch (_: Exception) {
                    // Fallback to MediaMetadataRetriever if loadThumbnail fails
                }
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val picture = retriever.embeddedPicture
            retriever.release()

            if (picture != null) {
                val buffer = Buffer().apply { write(picture) }
                return@withContext SourceResult(
                    source = ImageSource(buffer, context),
                    mimeType = "image/jpeg",
                    dataSource = DataSource.DISK
                )
            }
        } catch (_: Exception) {
            // Ignore extraction errors and return null
        }
        null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle audio media URIs
            val path = data.toString()
            if (path.startsWith("content://media/") && path.contains("/audio/media/")) {
                return AudioArtworkFetcher(data, context)
            }
            return null
        }
    }
}
