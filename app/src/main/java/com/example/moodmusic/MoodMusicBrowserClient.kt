package com.example.moodmusic

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MoodMusicBrowserClient : AppCompatActivity() {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {

            // Get the token for the MediaSession
            mediaBrowser.sessionToken.also { token ->

                // Create a MediaControllerCompat
                val mediaController = MediaControllerCompat(
                    this@MoodMusicBrowserClient, // Context
                    token
                )

                // Save the controller
                MediaControllerCompat.setMediaController(this@MoodMusicBrowserClient, mediaController)
            }

            // Finish building the UI
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }

        fun buildTransportControls() {
            val mediaController = MediaControllerCompat.getMediaController(this@MoodMusicBrowserClient)
            // Grab the view for the play/pause button
            findViewById<ImageView>(R.id.btn_play_pause).apply {
                setOnClickListener {
                    // Since this is a play/pause button, you'll need to test the current state
                    // and choose the action accordingly

                    val pbState = mediaController.playbackState.state
                    if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                        mediaController.transportControls.pause()
                    } else {
                        mediaController.transportControls.play()
                    }
                }
            }

            findViewById<ImageView>(R.id.btn_next).apply {
                setOnClickListener {
                    mediaController.transportControls.skipToNext()
                }
            }

            findViewById<ImageView>(R.id.btn_previous).apply {
                setOnClickListener {
                    mediaController.transportControls.skipToPrevious()
                }
            }

            // Display the initial state
            val metadata = mediaController.metadata
            val pbState = mediaController.playbackState

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback)
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //Update the UI to look different
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            //Change play/pause button
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // ...
        // Create MediaBrowserServiceCompat
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MoodMusicBrowserService::class.java),
            connectionCallbacks,
            null // optional Bundle
        )
    }

    public override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    public override fun onStop() {
        super.onStop()
        // (see "stay in sync with the MediaSession")
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }
}
