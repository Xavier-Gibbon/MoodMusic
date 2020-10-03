package com.example.moodmusic

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        val list = findViewById<RecyclerView>(R.id.list_music)

        val data = getMusicFiles()

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = MusicAdapter(data) {}
    }

    private fun getMusicFiles() : List<MusicDetails> {
        val result = mutableListOf<MusicDetails>()

        val resolver: ContentResolver = contentResolver
        val uri = Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        when {
            cursor == null -> {
                // TODO: query failed, handle error.
            }
            !cursor.moveToFirst() -> {
                // no media on the device
            }
            else -> {
                do {
                    val thisId = cursor.getLong(cursor.getColumnIndex(Media._ID))
                    val thisTitle = cursor.getString(cursor.getColumnIndex(Media.TITLE))
                    val isMusic = cursor.getInt(cursor.getColumnIndex(Media.IS_MUSIC))
                    if (isMusic != 0) {
                        val newMusic = MusicDetails(thisId, thisTitle)
                        result.add(newMusic)
                    }
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()

        return result
    }
}