package com.blesson.m3u8util.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var isFileLocked = MutableLiveData<Boolean>(true)

    fun setFileLocked(locked: Boolean) {
        isFileLocked.value = locked
    }
}