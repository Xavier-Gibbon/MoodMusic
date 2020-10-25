package com.example.moodmusic

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private val data: List<MusicDetails>
) : RecyclerView.Adapter<MusicAdapter.ViewHolder>() {
    var selectedData: MutableList<MusicDetails> = mutableListOf()

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
        if (selectedData.contains(item)) {
            holder.itemView.setBackgroundColor(Color.parseColor("#CCCCCC"))
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"))
        }
    }

    inner class ViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        private val number: TextView = v.findViewById(R.id.txt_name)

        fun bind(item: MusicDetails) {
            number.text = item.title
            v.setOnClickListener {
                if (selectedData.contains(item)) {
                    selectedData.remove(item)
                    v.setBackgroundColor(Color.parseColor("#FFFFFF"))
                } else {
                    selectedData.add(item)
                    v.setBackgroundColor(Color.parseColor("#CCCCCC"))
                }
            }
        }

    }
}