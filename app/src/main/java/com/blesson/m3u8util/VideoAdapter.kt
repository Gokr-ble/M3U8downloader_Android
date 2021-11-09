package com.blesson.m3u8util

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.blesson.m3u8util.utils.ContextUtil
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class VideoAdapter(data: ArrayList<String>) :
    RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle: TextView = itemView.findViewById(R.id.videoItemTitle)
        val itemSubtitle: TextView = itemView.findViewById(R.id.videoItemSubtitle)
        val itemDelete: ImageButton = itemView.findViewById(R.id.videoItemDelete)
    }

    private val videoData = data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            // 获取子项绝对路径、RecyclerView中的相对位置
            val path = ContextUtil.context.filesDir.path
            val pos = viewHolder.bindingAdapterPosition
            // 通过Content Provider调用外部打开视频
            val videoFile = File(path + File.separator + videoData[pos])
            val outUri = FileProvider.getUriForFile(view.context, view.context.packageName+".fileProvider", videoFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setDataAndType(outUri, "video/*")
            }
            view.context.startActivity(intent)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val str = videoData[position]
        val timeStr = str.substring(0, str.lastIndexOf("/"))

        var subtitle = str.substring(str.lastIndexOf("/") + 1)
        val path = ContextUtil.context.filesDir.path

        val dateTime = LocalDateTime.now()
        val formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatter2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val title = try {
            LocalDateTime.parse(timeStr, formatter2).format(formatter1)
        } catch (e: Exception) {
            timeStr
        }


        val f = File(path + File.separator + videoData[position])
        val videoDate = Date(f.lastModified())
        val nowDate = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())

        val dateHint: String
        if (nowDate != null){
            val diff = nowDate.time - videoDate.time
            val day = diff / (24 * 60 * 60 * 1000)
            val hour = (diff / (60 * 60 * 1000))
            val min = ((diff / (60 * 1000)))
            val sec = diff / 1000

            // 遵循kotlin规范，使用when代替级联if语句
            dateHint = when {
                sec <= 60 -> { "1分钟前" }
                min  < 60 -> { "${min}分钟前" }
                hour < 24 -> { "${hour}小时前" }
                else -> { "${day}天前" }
            }

            subtitle = "$subtitle - $dateHint"
        }

        holder.itemTitle.text = title
        holder.itemSubtitle.text = subtitle

        holder.itemDelete.setOnClickListener {
            val builder = AlertDialog.Builder(it.context).apply {
                setTitle("删除视频")
                setMessage("确定要删除该视频吗？")
                setCancelable(true)
            }
            builder.setPositiveButton("确定", DialogInterface.OnClickListener { dialogInterface, i ->
                var dir = videoData[position]
                dir = dir.substring(0, dir.lastIndexOf("/"))
                val videoDir = File(path + File.separator + dir + "/")
                val videoFile = File(path + File.separator + videoData[position])
                try {
                    videoFile.delete()
                    if (videoDir.listFiles() != null && videoDir.listFiles().isEmpty()) {
                        videoDir.delete()
                    }
                    videoData.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, videoData.size - position)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(it.context, "删除失败", Toast.LENGTH_SHORT).show()
                }
            })
            builder.create().show()
        }

        holder.itemView.setOnClickListener {
            // 通过Content Provider调用外部app打开视频
            try {
                // 通过Content Provider调用外部打开视频
                val videoFile = File(path + File.separator + videoData[position])
                val outUri = FileProvider.getUriForFile(it.context, it.context.packageName+".fileProvider", videoFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setDataAndType(outUri, "video/*")
                }
                it.context.startActivity(intent)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(it.context, "打开失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    override fun getItemCount(): Int {
        return videoData.size
    }

    fun updateVideoData(newData: ArrayList<String>) {
        if (newData.size != videoData.size) {
            videoData.clear()
            videoData.addAll(newData)
            notifyItemInserted(0)

        }
    }

    fun addVideoData(data: String) {
        videoData.add(data)
        notifyItemInserted(itemCount)
    }

}