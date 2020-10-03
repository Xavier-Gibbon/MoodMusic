package com.example.moodmusic

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class MusicDetails(
    val id: Long,
    val title: String
) : Parcelable {
}