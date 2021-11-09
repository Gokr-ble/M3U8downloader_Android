package com.blesson.m3u8util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blesson.m3u8util.model.SharedViewModel
import com.blesson.m3u8util.utils.ContextUtil
import java.io.File

class VideoListFragment : Fragment(){

    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        val data = prepareData()
        val layoutManager = LinearLayoutManager(context)
        val adapter = VideoAdapter(data)
        val recyclerView: RecyclerView = view.findViewById(R.id.videoList)
        recyclerView.apply {
            setLayoutManager(layoutManager)
            setAdapter(adapter)
        }

        val swipeRefresh: SwipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.blue_500)
        swipeRefresh.setOnRefreshListener {
            val newData = prepareData()
            adapter.updateVideoData(newData)
            recyclerView.scrollToPosition(0)
            swipeRefresh.isRefreshing = false
        }


        viewModel = ViewModelProvider(activity!!)[SharedViewModel::class.java]
        viewModel.downloadFinish.observe(viewLifecycleOwner, Observer {
            if (it) {
                adapter.updateVideoData(prepareData())
                recyclerView.scrollToPosition(0)
            }
        })


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
            data.sortWith(Comparator.reverseOrder())
        }
        return data
    }
}