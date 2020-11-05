package com.example.moodmusic.browserservice

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.example.moodmusic.R

const val ACTION_RESET_QUEUE_PLACEMENT = "Reset_queue"

private const val MEDIA_ROOT_ID = "media_root_id"
private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
private const val CHANNEL_ID = "MoodMusicServiceChannelID"
private const val NOTIFICATION_ID = 6906

class MoodMusicBrowserService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat

    private val listOfMusic = mutableListOf<MediaDescriptionCompat>()

    // The afChangeListener responds to requests made by the device when the focus is changed
    // This prevents our app from playing over other apps.
    private val focusChangeListener: AudioManager.OnAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.i(MoodMusicBrowserService::class.qualifiedName, "Audio Focus gained")
                    mediaCallback.onPlay()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    Log.i(MoodMusicBrowserService::class.qualifiedName, "Audio Focus lost")
                    mediaCallback.onPause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Log.i(MoodMusicBrowserService::class.qualifiedName, "Audio Focus lost: transient")
                    mediaCallback.onPause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->  {
                    Log.i(MoodMusicBrowserService::class.qualifiedName, "Audio Focus lost: transient and can duck")
                    // Audio ducking is done automatically, nothing to do on this end
                }
            }
        }

    // The mediaCallback handles the requests to change the playing state
    // This not only handles requests from the activity, but also from media buttons
    private val mediaCallback = object: MediaSessionCompat.Callback() {
        private lateinit var audioFocusRequest: AudioFocusRequest
        private lateinit var player: MoodMusicPlayerManager

        // noisyReceiver is used to pause the music if the phone becomes noisy
        // e.g. if the headphones are unplugged
        private val noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_AUDIO_BECOMING_NOISY) {
                    onPause()
                }
            }
        }

        // IMPORTANT: This must be called before calling any other function in this object
        fun createPlayer() {
            if (!this::player.isInitialized) {
                player = MoodMusicPlayerManager(applicationContext) {
                    onSkipToNext(true)
                }
            }
        }

        override fun onPlay() {
            Log.d(this::class.qualifiedName, "onPlay called")

            // We don't play music if we have no music
            // TODO: Notify the user why this failed, or maybe prevent the user from pressing
            // the play button while there are no items in the queue
            if (!player.hasMusic()) {
                return
            }

            val am = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Request audio focus for playback, this registers the afChangeListener
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setOnAudioFocusChangeListener(focusChangeListener)
                setAudioAttributes(AudioAttributes.Builder().run {
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                build()
            }

            // Try to get the audio focus, we don't play anything if we can't have the audio focus
            val result = am.requestAudioFocus(audioFocusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(this::class.qualifiedName, "Audio focus granted, playing music")
                // Start the service (prevents the service from dying when the activity is closed)
                startService(Intent(baseContext, MoodMusicBrowserService::class.java))
                mediaSession.isActive = true

                // start the player
                player.play()

                // Start the noisy receiver and update the metadata and playback state
                registerReceiver(noisyReceiver, IntentFilter(ACTION_AUDIO_BECOMING_NOISY))
                updateMetadataAndPlaybackState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.ACTION_PAUSE
                )
                // Put the service in the foreground, post notification
                updateNotification()
            }
        }

        // Stops the media playing
        override fun onStop() {
            Log.d(this::class.qualifiedName, "onStop called")
            val am = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Abandon audio focus
            am.abandonAudioFocusRequest(audioFocusRequest)
            // Stop the service
            this@MoodMusicBrowserService.stopSelf()
            // Set the session inactive  (and update metadata and state)
            mediaSession.isActive = false
            // stop the player (custom call)
            player.stop()
            updateMetadataAndPlaybackState(
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.ACTION_PLAY
            )
            // Take the service out of the foreground
            updateNotification()
            this@MoodMusicBrowserService.stopForeground(false)
        }

        // Pauses the current song
        override fun onPause() {
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            Log.d(this::class.qualifiedName, "onPause called")
            // Update metadata and state
            updateMetadataAndPlaybackState(
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.ACTION_PLAY
            )
            // pause the player (custom call)
            player.pause()
            unregisterReceiver(noisyReceiver)
            // Take the service out of the foreground, retain the notification
            updateNotification()
            this@MoodMusicBrowserService.stopForeground(false)
        }

        // Skips forward to the next song
        override fun onSkipToNext() {
            onSkipToNext(false)
        }

        fun onSkipToNext(forcePlay: Boolean) {
            // forcePlay should be true only when the current song is finished
            // as '#isPlaying()' returns false
            Log.d(this::class.qualifiedName, "onSkipToNext called")

            player.skipToNext(forcePlay)
            if (forcePlay || player.isPlaying()) {
                updateMetadataAndPlaybackState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            } else {
                updateMetadataAndPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.ACTION_PLAY
                )
            }
            updateNotification()
        }

        // Skips back to the last song
        override fun onSkipToPrevious() {
            Log.d(this::class.qualifiedName, "onSkipToPrevious called")
            player.skipToPrevious()
            if (player.isPlaying()) {
                updateMetadataAndPlaybackState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            } else {
                updateMetadataAndPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.ACTION_PLAY
                )
            }
            updateNotification()
        }

        // Adds an item currently in the queue
        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            Log.d(this::class.qualifiedName, "onAddQueueItem called")
            description?.apply {
                player.addQueueItem(this)

                val queueToSetTo = mutableListOf<MediaSessionCompat.QueueItem>()
                if (mediaSession.controller.queue != null) {
                    queueToSetTo.addAll(mediaSession.controller.queue)
                }
                queueToSetTo.add(MediaSessionCompat.QueueItem(this, this.mediaId!!.toLong()))
                mediaSession.setQueue(queueToSetTo)
            }
        }

        // Removes an item currently in the queue
        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            Log.d(this::class.qualifiedName, "onRemoveQueueItem called")
            description?.apply {
                player.removeQueueItem(this.mediaId!!)
                val queueToSetTo = mutableListOf<MediaSessionCompat.QueueItem>()
                queueToSetTo.addAll(mediaSession.controller.queue)
                val toRemove =
                    queueToSetTo.find { queue -> queue.queueId == description.mediaId!!.toLong() }
                toRemove?.apply {
                    queueToSetTo.remove(this)
                }

                mediaSession.setQueue(queueToSetTo)
            }
        }

        // This function handles any custom actions
        // TODO: Would this be the right place to update playlists?
        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                ACTION_RESET_QUEUE_PLACEMENT -> {
                    if (mediaSession.controller.playbackState.state != PlaybackStateCompat.STATE_STOPPED && mediaSession.controller.playbackState.state != PlaybackStateCompat.STATE_NONE) {
                        onStop()
                    }

                    player.currentMusic = 0
                }
            }
        }

        fun getCurrentSong(): MediaDescriptionCompat {
            return player.getCurrentSong()
        }

    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        mediaCallback.createPlayer()

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, MoodMusicBrowserService::class.java.name).apply {
            // Set an initial PlaybackState with the default actions
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            setPlaybackState(stateBuilder.build())

            // callback has methods that handle callbacks from a media controller
            setCallback(mediaCallback)

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
            setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        }

        initializeNotification()
        loadMusicFiles()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // This allows the mediaSession to respond to media buttons
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    // This function returns the root browser so that apps can get the media items to browse
    // What media items should be returned should be determined by #onLoadChildren()
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
        return if (clientUid >= 0) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            BrowserRoot(MEDIA_ROOT_ID, null)
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierarchy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            BrowserRoot(EMPTY_MEDIA_ROOT_ID, null)
        }

    }

    // This function returns a list of mediaItems through the result parameter.
    // This function gets called when a client calls #subscribe()
    // Note that the client should use #getRoot() for the parentId parameter
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //  Browsing not allowed
        if (EMPTY_MEDIA_ROOT_ID == parentId) {
            result.sendResult(null)
            return
        }

        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        // Check if this is the root menu:
        if (MEDIA_ROOT_ID == parentId) {
            // Send everything in the listOfMusic
            listOfMusic.forEach {
                mediaItems.add(MediaBrowserCompat.MediaItem(it, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }
        } else {
            // TODO: Use this to sort by playlist
        }
        result.sendResult(mediaItems)
    }

    // This function sets the values for the notification. Note that stuff like contentText don't
    // get set as this will get updated in #updateNotification()
    private fun initializeNotification() {
        // Create the notification channel
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), importance)
        manager.createNotificationChannel(channel)

        notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            // Stop the service when the notification is swiped away
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    applicationContext,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            setSmallIcon(R.drawable.ic_baseline_music_note_24)
            color = ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark)

            // Add a skip to previous button
            addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    getString(R.string.skip_to_previous),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
            // Add a pause button (this will change in #updateNotification())
            addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
            // Add a skip to next button
            addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    getString(R.string.skip_to_next),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )

            // Take advantage of MediaStyle features
            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
            )
        }
    }

    // This function updates the metadata and playback state
    fun updateMetadataAndPlaybackState(newState: Int, newAction: Long) {
        val currentSong = mediaCallback.getCurrentSong()
        val metadata = MediaMetadataCompat.Builder()
            .putText(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
            .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.subtitle)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, currentSong.mediaUri.toString())
            .build()
        val playbackState = PlaybackStateCompat.Builder()
            .setState(newState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActions(newAction or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .build()

        mediaSession.setMetadata(metadata)
        mediaSession.setPlaybackState(playbackState)
    }

    // This function updates the notification with new information, such as the song that is playing
    // or the play/pause button icon/functionality
    @SuppressLint("RestrictedApi")
    private fun updateNotification() {
        // Get the session's metadata and playback state
        val controller = mediaSession.controller
        val playbackState = controller.playbackState
        val description = controller.metadata.description

        // Update the notification builder with new information
        notificationBuilder.apply {
            // Add the metadata for the currently playing track
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)

            // Enable launching the player by clicking the notification
            setContentIntent(controller.sessionActivity)

            //Update the play/pause button icon
            val icon: Int
            val title: String
            if (playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                icon = android.R.drawable.ic_media_pause
                title = getString(R.string.pause)
            } else {
                icon = android.R.drawable.ic_media_play
                title = getString(R.string.play)
            }
            // This is apparently restricted API, but suppressing the warning seems to make it work
            mActions[1] = NotificationCompat.Action(
                icon,
                title,
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    applicationContext,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
        }

        // Display the notification and place the service in the foreground
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

    }

    // This function finds the music files on disk using a content resolver then adds them to the list of music
    private fun loadMusicFiles() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: The permission should be requested when the user interacts with some sort of component.
            return
        }

        val resolver: ContentResolver = contentResolver
        val uri = Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = resolver.query(
            uri,
            arrayOf(
                Media._ID,
                Media.TITLE,
                Media.ARTIST,
                Media.ALBUM_ID
            ),
            "${Media.IS_MUSIC} != 0",
            null,
            null)
        when {
            cursor == null -> {
                // TODO: query failed, handle error.
            }
            !cursor.moveToFirst() -> {
                //TODO: No music, do something about it
            }
            else -> cursor.apply {
                do {
                    val thisId = getLong(getColumnIndex(Media._ID))
                    val thisTitle = getString(getColumnIndex(Media.TITLE))
                    val thisArtist = getString(getColumnIndex(Media.ARTIST))
                    val thisAlbumId = getLong(getColumnIndex(Media.ALBUM_ID))

                    val desc = MediaDescriptionCompat.Builder()
                        .setMediaId(thisId.toString())
                        .setTitle(thisTitle)
                        .setSubtitle(thisArtist)
                        .setMediaUri(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, thisId))
                        .setIconUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), thisAlbumId))
                        .build()

                    listOfMusic.add(desc)
                } while (moveToNext())
            }
        }
        cursor?.close()
    }
}