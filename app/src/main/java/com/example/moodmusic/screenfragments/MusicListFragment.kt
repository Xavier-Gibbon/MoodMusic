package com.example.moodmusic.screenfragments

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodmusic.MusicAdapter
import com.example.moodmusic.R
import com.example.moodmusic.SELECTED_MUSIC_KEY
import com.example.moodmusic.browserservice.ACTION_RESET_QUEUE_PLACEMENT

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

    override fun getSubscriptionCallback(): MediaBrowserCompat.SubscriptionCallback {
        return object: MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                val adapter = view?.findViewById<RecyclerView>(R.id.list_music)?.adapter as MusicAdapter
                adapter.updateDataset(children)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun setupTransportControls(mediaController: MediaControllerCompat) {
        view?.findViewById<ImageView>(R.id.btn_update_playlist)?.setOnClickListener {
            val adapter = view?.findViewById<RecyclerView>(R.id.list_music)?.adapter as MusicAdapter
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
    }
}