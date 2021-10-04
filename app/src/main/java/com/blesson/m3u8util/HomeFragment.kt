package com.blesson.m3u8util

import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class HomeFragment : Fragment() {

//    private lateinit var mHandler: Handler

    private var isWorking = false
    private var deleteSlicesOnFinish = true
    private var downloadToInner = true
    private var downloadToOuter = false

//    private lateinit var settings: Bundle
    private lateinit var scheduler: Scheduler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textUrl: EditText = view.findViewById(R.id.text_url)
        val deleteSlicesSwitch: SwitchMaterial = view.findViewById(R.id.switch_delete_slices)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
        val progressText: TextView = view.findViewById(R.id.progress_num)
        val progressStat: TextView = view.findViewById(R.id.current_state)
        val startBtn: MaterialCardView = view.findViewById(R.id.btn_start_download)
        val stopBtn : MaterialCardView = view.findViewById(R.id.btn_stop_download)
        val dInnerBtn: Chip = view.findViewById(R.id.btn_loc_inner)
        val dOuterBtn: Chip = view.findViewById(R.id.btn_loc_outer)
        val threadNumSlider: Slider = view.findViewById(R.id.thread_num_slider)

        val mHandler = Handler(Looper.getMainLooper()) {
            when (it.what) {
                1 -> {  // 下载中
                    val sliceIndex = it.arg1
                    val sliceTotal = it.arg2
                    val progressNum = "$sliceIndex/$sliceTotal"
                    progressBar.max = sliceTotal
                    progressBar.progress = sliceIndex
                    progressText.text = progressNum
                    progressStat.text = "下载中..."
                }
                2 -> {  // 解密中 清理中
                    progressStat.text = it.obj.toString()
                }
                3 -> {  // 下载完成
                    Toast.makeText(context, it.obj.toString(), Toast.LENGTH_SHORT).show()
                    isWorking = false
                }
                4 -> {
                    Toast.makeText(context, "下载取消", Toast.LENGTH_SHORT).show()
                    progressStat.text = "空闲"
                    progressBar.progress = 0
                    progressText.text = "0/0"
                    isWorking = false
                }
            }
            false
        }

        startBtn.setOnClickListener {
            val url = textUrl.text.toString()
            if (url != "" && isLegalUrl(url) && (downloadToInner or downloadToOuter)) {
                isWorking = true
                progressStat.text = "准备中"

                // 将下载参数放置到bundle
                val settings = Bundle().apply {
                    putBoolean("deleteOnFinish", deleteSlicesOnFinish)
                    putBoolean("downToInner", downloadToInner)
                    putBoolean("downToOuter", downloadToOuter)
                    putInt("threadNumber", threadNumSlider.value.toInt())
                }

                scheduler = Scheduler(url, mHandler, settings)
                scheduler.start()

                textUrl.setText("")
            } else {
                Toast.makeText(context, "URL非法或未选择保存路径", Toast.LENGTH_SHORT).show()
            }
        }

        stopBtn.setOnClickListener {
            if (isWorking) {
                scheduler.stop()
            }
        }

        deleteSlicesSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            deleteSlicesOnFinish = isChecked
        }

        dInnerBtn.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            downloadToInner = isChecked
        }

        dOuterBtn.setOnCheckedChangeListener { button: CompoundButton?, isChecked: Boolean ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadToOuter = isChecked
            } else {
                button?.isChecked = false
                Toast.makeText(context, "目前仅支持Android 10", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun isLegalUrl(s: String): Boolean {
        return s.startsWith("http") || s.endsWith(".m3u8") || s.contains(".m3u8")
    }
}