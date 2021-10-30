package com.blesson.m3u8util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File

class VideoListFragment : Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        val data = prepareData()
        val dataSize = data.size
        val layoutManager = LinearLayoutManager(context)
        val adapter = VideoAdapter(data)
        val recyclerView: RecyclerView = view.findViewById(R.id.videoList)
        recyclerView.apply {
            setLayoutManager(layoutManager)
            setAdapter(adapter)
        }

        //TODO 下拉刷新
        // https://cdn.workgreat14.live//m3u8/547509/547509.m3u8?st=NOUxRTmQCUYJFU7x66mcRw&e=1635568589
        val swipeRefresh: SwipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.blue_500)
        swipeRefresh.setOnRefreshListener {
            val newData = prepareData()
            if(newData.size != dataSize) {
//                adapter.notifyItemInserted(dataSize+1)
                adapter.updateVideoData(newData)
            }
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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