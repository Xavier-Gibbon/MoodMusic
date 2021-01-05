package com.example.moodmusic.screenfragments

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import androidx.fragment.app.Fragment

abstract class ScreenFragment : Fragment() {
    abstract fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback
    abstract fun setupTransportControls(mediaController: MediaControllerCompat)
}