package com.blesson.m3u8util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class VideoListFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val data = prepareData()
        val layoutManager = LinearLayoutManager(context)
        val adapter = VideoAdapter(data)
        val recyclerView: RecyclerView = view.findViewById(R.id.videoList)
        recyclerView.apply {
            setLayoutManager(layoutManager)
            setAdapter(adapter)
        }
    }

    private fun prepareData(): ArrayList<String> {
        val data = ArrayList<String>()
        val videoPath = File(ContextUtil.context.filesDir.path)
        val dirs = videoPath.listFiles()
        if (dirs != null) {
            for (f in dirs) {
                if (f.isDirectory) {
                    val tmp = f.listFiles()
                    if (tmp != null && tmp.isNotEmpty()) {
                        data.add(f.name + "/" + tmp[0].name)
                    }
                }
            }
            data.sortWith(Comparator.naturalOrder())
        }
        return data
    }
}