package com.example.moodmusic.screenfragments

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodmusic.MusicAdapter
import com.example.moodmusic.R
import com.example.moodmusic.SELECTED_MUSIC_KEY

class MusicListFragment : ScreenFragment() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val selectedIds = mutableListOf<String>()
        if (savedInstanceState !== null) {
            selectedIds.addAll(savedInstanceState.getStringArrayList(SELECTED_MUSIC_KEY)!!)
        }

        val adapter = MusicAdapter(selectedIds)
        val list = view!!.findViewById<RecyclerView>(R.id.list_music)

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_view, container, false)
    }

    override fun onChildrenLoaded(
        parentId: String,
        children: MutableList<MediaBrowserCompat.MediaItem>
    ) {
        val adapter = view?.findViewById<RecyclerView>(R.id.list_music)?.adapter as MusicAdapter
        adapter.updateDataset(children)
        adapter.notifyDataSetChanged()
    }
}