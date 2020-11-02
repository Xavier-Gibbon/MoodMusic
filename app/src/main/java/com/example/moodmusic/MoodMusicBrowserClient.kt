package com.example.moodmusic

import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodmusic.browserservice.MoodMusicBrowserService

class MoodMusicBrowserClient : AppCompatActivity() {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var rootString = ""
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

            rootString = mediaBrowser.root
            mediaBrowser.subscribe(rootString, subscriptionCallback)
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }

        fun buildTransportControls() {
            val mediaController = MediaControllerCompat.getMediaController(this@MoodMusicBrowserClient)
            val adapter = findViewById<RecyclerView>(R.id.list_music).adapter as MusicAdapter
            // Grab the view for the play/pause button
            findViewById<ImageView>(R.id.btn_play_pause).setOnClickListener {
                //TODO: Move this part to a different button, it shouldn't be part of the play button
                adapter.selectedData.forEach {
                    mediaController.addQueueItem(it.description)
                }

                val pbState = mediaController.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.pause()
                } else {
                    mediaController.transportControls.play()
                }
            }

            findViewById<ImageView>(R.id.btn_next).setOnClickListener {
                mediaController.transportControls.skipToNext()
            }

            findViewById<ImageView>(R.id.btn_prev).setOnClickListener {
                mediaController.transportControls.skipToPrevious()
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
            Log.d(MoodMusicBrowserClient::class.qualifiedName, "onMetadataChanged")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            //Change play/pause button
            state?.apply {
                findViewById<ImageView>(R.id.btn_play_pause).apply {
                    if (state.state == PlaybackStateCompat.STATE_PLAYING) {
                        setImageResource(android.R.drawable.ic_media_pause)
                    } else {
                        setImageResource(android.R.drawable.ic_media_play)
                    }
                }
            }

        }
    }

    private var subscriptionCallback = object: MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            val adapter = findViewById<RecyclerView>(R.id.list_music).adapter as MusicAdapter
            adapter.updateDataset(children)
            adapter.notifyDataSetChanged()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        val adapter = MusicAdapter(mutableListOf())
        val list = findViewById<RecyclerView>(R.id.list_music)

        list.layoutManager = LinearLayoutManager(this@MoodMusicBrowserClient)
        list.adapter = adapter
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
