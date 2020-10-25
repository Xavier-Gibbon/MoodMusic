package com.example.moodmusic

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore

// This class is responsible for knowing what songs are in the current playlist
// as well as having the means to play those songs

class MoodMusicPlayerManager(
    private val context: Context
) {
    var playList: List<MusicDetails> = mutableListOf()
    var currentMusic = 0

    private var player: MediaPlayer? = null

    fun play() {
        // Can't play anything if the playlist is empty
        if (playList.isEmpty()) {
            return
        }

        if (player == null) {
            player = createNewReadyPlayer(createUriForCurrentMusic())
        }

        player!!.apply {
            if (!isPlaying) {
                start()
            }
        }
    }

    fun pause() {
        // Can't pause anything if the playlist is empty
        if (playList.isEmpty()) {
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
        if (playList.isEmpty()) {
            return
        }

        player?.apply {
            if (isPlaying){
                stop()
            }
        }
    }

    fun skipToNext() {
        if (playList.isEmpty()) {
            return
        }

        currentMusic++
        if (currentMusic >= playList.size) {
            currentMusic = 0
        }
        val newPlayer = createNewReadyPlayer(createUriForCurrentMusic())
        player?.apply {
            stop()
            release()
        }

        player = newPlayer
        player!!.start()
    }

    fun skipToPrevious() {
        if (playList.isEmpty()) {
            return
        }

        currentMusic--
        if (currentMusic < 0) {
            currentMusic = playList.size - 1
        }
        val newPlayer = createNewReadyPlayer(createUriForCurrentMusic())
        player?.apply {
            stop()
            release()
        }

        player = newPlayer
        player!!.start()
    }

    fun teardown() {
        player?.apply {
            stop()
            release()
        }

        player = null
    }

    private fun createNewReadyPlayer(uri: Uri) : MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, uri)
            prepare()
            setOnCompletionListener {
                skipToNext()
            }
        }
    }

    private fun createUriForCurrentMusic() : Uri {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playList[currentMusic].id)
    }
}