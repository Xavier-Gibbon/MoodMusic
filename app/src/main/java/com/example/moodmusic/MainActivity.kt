package com.example.moodmusic

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodmusic.browserservice.MoodMusicPlayerManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

const val PERMISSION_CODE = 55

class MainActivity : AppCompatActivity() {
    private lateinit var playerManager: MoodMusicPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)
        playerManager = MoodMusicPlayerManager(this)

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: The permission should be requested when the user interacts with some sort of component.
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.READ_EXTERNAL_STORAGE).toTypedArray(), PERMISSION_CODE)
            return
        }

        val list = findViewById<RecyclerView>(R.id.list_music)
        /*val data = getMusicFiles()
        val adapter = MusicAdapter(data)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<FloatingActionButton>(R.id.btn_playitnow).setOnClickListener {
            if (adapter.selectedData.isEmpty()) {
                return@setOnClickListener
            }

            playerManager.playList = adapter.selectedData
            playerManager.play()
        }*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getMusicFiles() : List<MusicDetails> {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: The permission should be requested when the user interacts with some sort of component.
            ActivityCompat.requestPermissions(this, listOf(Manifest.permission.READ_EXTERNAL_STORAGE).toTypedArray(), PERMISSION_CODE)
            return listOf()
        }

        val result = mutableListOf<MusicDetails>()

        val resolver: ContentResolver = contentResolver
        val uri = Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        when {
            cursor == null -> {
                // TODO: query failed, handle error.
            }
            !cursor.moveToFirst() -> {
                return listOf()
            }
            else -> {
                do {
                    val thisId = cursor.getLong(cursor.getColumnIndex(Media._ID))
                    val thisTitle = cursor.getString(cursor.getColumnIndex(Media.TITLE))
                    val isMusic = cursor.getInt(cursor.getColumnIndex(Media.IS_MUSIC))
                    if (isMusic != 0) {
                        //val newMusic = MusicDetails(thisId, thisTitle, "", 1)
                        //result.add(newMusic)
                    }
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()

        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.teardown()
    }
}