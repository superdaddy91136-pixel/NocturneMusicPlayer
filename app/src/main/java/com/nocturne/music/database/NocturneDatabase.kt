package com.nocturne.music.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayHistoryEntity::class,
        FavoriteAlbumEntity::class,
        FavoriteArtistEntity::class,
        PinnedAlbumEntity::class,
        LyricsCacheEntity::class,
        SearchHistoryEntity::class,
        ExcludedFolderEntity::class,
        CustomArtworkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NocturneDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}
