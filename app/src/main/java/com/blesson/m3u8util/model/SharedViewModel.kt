package com.blesson.m3u8util.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    var downloadFinish = MutableLiveData<Boolean>(false)

    fun setDownloadState(state: Boolean) {
        downloadFinish.value = state
    }

}