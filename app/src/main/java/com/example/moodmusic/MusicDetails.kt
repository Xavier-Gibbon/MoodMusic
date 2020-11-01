package com.example.moodmusic

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.android.parcel.Parcelize

@Parcelize
class MusicDetails(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Int
) : Parcelable {
    fun createLocalUri(): Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }
}