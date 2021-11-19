package com.simo.smemssdkdemo

import android.app.Application
import com.simo.smemssdk.*
import timber.log.Timber

/**
 * Created by GrayLand119
 * on 2021/3/15
 */
@ExperimentalUnsignedTypes
class SMApplication: Application(), SMEmsManagerDelegate {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        SMEmsManager.defaultManager.attachContext(this)
        SMEmsManager.defaultManager.addDelegate(this)
    }

    /**
     * 扫描设备时回调
    - Parameter bleDevices: 设备列表
     */
    override fun didDiscoverDevice(bleDevices: List<SMEmsDeviceModel>) {
        Timber.e("发现设备: $bleDevices")
    }
    /** 设备连接成功/失败回调 */
    override fun didConnectedDevice(isSuccess: Boolean, bleDevice: SMEmsDevice, error: String?) {
        Timber.e("连接状态回调: ${ if(isSuccess) "成功" else ("失败"+error)}")
    }

    /** 若开启自动重连, 设备断开时不会调用此回调, 转而会调用 didStartReconnectDevice. */
    override fun didDisconnectedDevice(bleDevice: SMEmsDevice?, error: String?) {
        Timber.e("设备已断开连接, 没有自动重连, 持久的断开连接")
    }

    /** 设备断开连接时, 若开启自动重连, 则会回调此函数. */
    override fun didStartReconnectDevice(bleDevice: SMEmsDevice) {
        Timber.e("设备断开连接, 开启了自动重连, 正在重连...")
    }

    /** 操作超时回调 */
    override fun deviceExecuteTimeout(bleDevice: SMEmsDevice?, action: SMEmsDeviceAction, msg: String?) {
        Timber.e("发送指令超时: $msg")
    }

    /** 蓝牙开关状态改变 */
    override fun bleStateDidChanged(state: SMBLEState) {
        Timber.e("系统蓝牙开关: $state")
    }

    /** 或定时回调状态, 且设备以下状态改变时会立即回调.
    - 电源状态/底座状态/充电状态/运动状态(开始/暂停/停止)/电量/剩余运动时间/当前强度等级
    - Parameter device: EMS 蓝牙设备 */
    override fun didUpdateStatus(device: SMEmsDevice) {
        Timber.e("设备状态改变回调")
    }
}
