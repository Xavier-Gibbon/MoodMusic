package com.example.moodmusic.browserservice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.support.v4.media.MediaDescriptionCompat

// This class is responsible for knowing what songs are in the current playlist
// as well as having the means to play those songs

class MoodMusicPlayerManager(
    private val context: Context
) {
    var playList: MutableList<MediaDescriptionCompat> = mutableListOf()
    var currentMusic = 0

    private var player: MediaPlayer? = null

    fun hasMusic(): Boolean {
        return playList.isEmpty()
    }

    fun play() {
        // Can't play anything if the playlist is empty
        if (hasMusic()) {
            return
        }

        if (player == null) {
            player = createNewReadyPlayer()
        }

        player!!.apply {
            if (!isPlaying) {
                start()
            }
        }
    }

    fun pause() {
        // Can't pause anything if the playlist is empty
        if (hasMusic()) {
            return
        }

        player?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    fun stop() {
        // Can't stop anything if...you get the point
        if (hasMusic()) {
            return
        }

        player?.apply {
            if (isPlaying){
                stop()
            }
        }
    }

    fun skipToNext() {
        if (hasMusic()) {
            return
        }

        currentMusic++
        if (currentMusic >= playList.size) {
            currentMusic = 0
        }
        val newPlayer = createNewReadyPlayer()
        player?.apply {
            stop()
            release()
        }

        player = newPlayer
        player!!.start()
    }

    fun skipToPrevious() {
        if (hasMusic()) {
            return
        }

        currentMusic--
        if (currentMusic < 0) {
            currentMusic = playList.size - 1
        }
        val newPlayer = createNewReadyPlayer()
        player?.apply {
            stop()
            release()
        }

        player = newPlayer
        player!!.start()
    }

    fun addQueueItem(newItem: MediaDescriptionCompat) {
        playList.add(newItem)
    }

    fun getCurrentSong(): MediaDescriptionCompat {
        return playList[currentMusic]
    }

    fun teardown() {
        player?.apply {
            stop()
            release()
        }

        player = null
    }

    private fun createNewReadyPlayer() : MediaPlayer {
        val uri = playList[currentMusic].mediaUri
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            if (uri != null) {
                setDataSource(context, uri)
            }
            prepare()
            setOnCompletionListener {
                skipToNext()
            }
        }
    }
}