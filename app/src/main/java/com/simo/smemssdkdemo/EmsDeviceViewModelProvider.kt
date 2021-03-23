package com.simo.smemssdkdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.simo.smemssdk.SMEmsDeviceModel

/**
 * Created by GrayLand119
 * on 2021/3/15
 */
class EmsDeviceViewModelProvider(private val devices: List<SMEmsDeviceModel>) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmsDeviceViewModel::class.java)) {
            return EmsDeviceViewModel(MutableLiveData(devices)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}