package com.example.moodmusic

import android.graphics.Color
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class MusicAdapter(
    var selectedIds: MutableList<String>
) : RecyclerView.Adapter<MusicAdapter.ViewHolder>() {
    private val data: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater
            .inflate(R.layout.layout_music, parent, false) as View
        return ViewHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: MusicAdapter.ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
        if (selectedIds.contains(item.mediaId)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#CCCCCC"))
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
        }
    }

    fun updateDataset(newList: MutableList<MediaBrowserCompat.MediaItem>) {
        data.clear()
        data.addAll(newList)
    }

    // selectedIds are saved as strings, convert them back into media items
    fun getSelectedItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val result = mutableListOf<MediaBrowserCompat.MediaItem>()
        selectedIds.forEach {
            val item = data.find { song -> song.mediaId == it }
            item?.apply {
                result.add(this)
            }
        }

        return result
    }

    inner class ViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        private val title: TextView = v.findViewById(R.id.txt_title)
        private val artist: TextView = v.findViewById(R.id.txt_artist)
        private val albumArt: ImageView = v.findViewById(R.id.img_album_art)

        fun bind(item: MediaBrowserCompat.MediaItem) {
            title.text = item.description.title
            artist.text = item.description.subtitle

            // I use Picasso here as this allows us to show a placeholder
            // in the case that there is no album art available on disk
            Picasso.get()
                .load(item.description.iconUri)
                .error(R.drawable.ic_baseline_music_note_24)
                .placeholder(R.drawable.ic_baseline_music_note_24)
                .into(albumArt)

            v.setOnClickListener {
                if (selectedIds.contains(item.mediaId)) {
                    selectedIds.remove(item.mediaId)
                    v.setBackgroundColor(Color.parseColor("#FFFFFF"))
                } else {
                    selectedIds.add(item.mediaId!!)
                    v.setBackgroundColor(Color.parseColor("#DDDDDD"))
                }
            }
        }

    }
}