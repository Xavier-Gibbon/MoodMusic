package com.example.moodmusic.screenfragments

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moodmusic.R

class NowPlayingFragment : ScreenFragment() {
    override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
        return object: MediaBrowserCompat.SubscriptionCallback() {

        }
    }

    override fun setupTransportControls(mediaController: MediaControllerCompat) {

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_now_playing, container, false)
    }
}