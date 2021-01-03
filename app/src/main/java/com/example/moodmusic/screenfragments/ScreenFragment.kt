package com.example.moodmusic.screenfragments

import android.support.v4.media.MediaBrowserCompat
import androidx.fragment.app.Fragment

abstract class ScreenFragment : Fragment() {
    abstract fun onChildrenLoaded(parentId: String,
                                  children: MutableList<MediaBrowserCompat.MediaItem>)
}