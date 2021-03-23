package com.simo.smemssdkdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.simo.smemssdk.SMEmsDeviceModel


/**
 * Created by GrayLand119
 * on 2021/3/15
 */
class EmsDeviceViewModel(val devices: MutableLiveData<List<SMEmsDeviceModel>>): ViewModel() {

}