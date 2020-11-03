package com.example.moodmusic.browserservice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.support.v4.media.MediaDescriptionCompat

// This class is responsible for knowing what songs are in the current playlist
// as well as having the means to play those songs using the mediaPlayer

class MoodMusicPlayerManager(
    private val context: Context,
    // This is used by the service to update the metadata and notification when the song ends naturally
    private val onPlayCompletion: () -> Unit
) {
    var playList: MutableList<MediaDescriptionCompat> = mutableListOf()
    var currentMusic = 0

    private var player: MediaPlayer? = null

    fun hasMusic(): Boolean {
        return playList.isNotEmpty()
    }

    fun isPlaying(): Boolean {
        if (player == null) {
            return false
        }

        return player!!.isPlaying
    }

    fun play() {
        // Can't play anything if the playlist is empty
        if (!hasMusic()) {
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
        if (!hasMusic()) {
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
        if (!hasMusic()) {
            return
        }

        player?.apply {
            if (isPlaying){
                stop()
                release()
            }
        }

        player = null
    }

    fun skipToNext() {
        skipToNext(false)
    }

    fun skipToNext(forcePlay: Boolean) {
        if (!hasMusic()) {
            return
        }

        val shouldStartPlaying = forcePlay || player?.isPlaying!!

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
        if (shouldStartPlaying) {
            player!!.start()
        }
    }

    fun skipToPrevious() {
        if (!hasMusic()) {
            return
        }

        val shouldStartPlaying = player?.isPlaying

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
        if (shouldStartPlaying !== null && shouldStartPlaying) {
            player!!.start()
        }
    }

    fun addQueueItem(newItem: MediaDescriptionCompat) {
        playList.add(newItem)
    }

    fun removeQueueItem(itemId: String) {
        val toRemove = playList.find { song -> song.mediaId == itemId }
        toRemove?.apply {
            playList.remove(this)
        }
    }

    fun getCurrentSong(): MediaDescriptionCompat {
        return playList[currentMusic]
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
                skipToNext(true)
                onPlayCompletion()
            }
        }
    }
}