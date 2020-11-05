package com.example.moodmusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodmusic.browserservice.ACTION_RESET_QUEUE_PLACEMENT
import com.example.moodmusic.browserservice.MoodMusicBrowserService

const val SELECTED_MUSIC_KEY = "selected_music"
const val PERMISSION_CODE = 55

class MoodMusicBrowserClient : AppCompatActivity() {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var rootString = ""

    // connectionCallbacks handles the appropriate methods for connecting to the browser service
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
            // TODO: Disable the buttons here
        }

        override fun onConnectionFailed() {
            // TODO: This shouldn't really happen, but some error handling should go here
        }

        // This sets up the buttons in the activity and updates the UI using the metadata and playback state
        // TODO: Should we have the buttons disabled by default, then enabled here?
        private fun buildTransportControls() {
            val mediaController = MediaControllerCompat.getMediaController(this@MoodMusicBrowserClient)
            val adapter = findViewById<RecyclerView>(R.id.list_music).adapter as MusicAdapter
            // Grab the view for the play/pause button
            findViewById<ImageView>(R.id.btn_play_pause).setOnClickListener {
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

            findViewById<ImageView>(R.id.btn_update_playlist).setOnClickListener {
                if (adapter.getSelectedItems().isEmpty()) {
                    return@setOnClickListener
                }

                // When we are updating the playlist, we expect the media to go back to the start
                mediaController.transportControls.sendCustomAction(ACTION_RESET_QUEUE_PLACEMENT, null)

                if (mediaController.queue != null) {
                    mediaController.queue.forEach {
                        mediaController.removeQueueItem(it.description)
                    }
                }

                adapter.getSelectedItems().forEach {
                    mediaController.addQueueItem(it.description)
                }
            }

            // Display the initial state
            val metadata = mediaController.metadata
            val pbState = mediaController.playbackState
            controllerCallback.onMetadataChanged(metadata)
            controllerCallback.onPlaybackStateChanged(pbState)

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback)
        }
    }

    // controllerCallback handles any changes that can happen to the metadata or the playback state
    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            // TODO: when we finally show the current music playing, we should do it here
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

    // The subscription callback handles getting the media items from the service
    // Right now, this just fills in the music list, but it can be used for showing playlists
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

        val data = mutableListOf<String>()
        if (savedInstanceState !== null) {
            data.addAll(savedInstanceState.getStringArrayList(SELECTED_MUSIC_KEY)!!)
        }

        val adapter = MusicAdapter(data)
        val list = findViewById<RecyclerView>(R.id.list_music)

        list.layoutManager = LinearLayoutManager(this@MoodMusicBrowserClient)
        list.adapter = adapter

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

        // TODO: There should be an interactable component that requests for the permission
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: The permission should be requested when the user interacts with some sort of component.
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.READ_EXTERNAL_STORAGE).toTypedArray(), PERMISSION_CODE)
        }
    }

    public override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    public override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val adapter = findViewById<RecyclerView>(R.id.list_music).adapter as MusicAdapter
        outState.putStringArrayList(SELECTED_MUSIC_KEY, ArrayList(adapter.selectedIds))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //TODO: if the permission is denied, there needs to be a way to re-request the permission
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The service should also be re-created from this as nothing else is bound to it
                // and it doesn't stick around due to it not playing any music
                recreate()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
