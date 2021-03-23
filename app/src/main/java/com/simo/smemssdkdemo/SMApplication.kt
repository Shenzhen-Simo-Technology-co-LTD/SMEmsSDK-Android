package com.simo.smemssdkdemo

import android.app.Application
import com.simo.smemssdk.SMEmsManager
import timber.log.Timber

/**
 * Created by GrayLand119
 * on 2021/3/15
 */
class SMApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        SMEmsManager.defaultManager.attachContext(this)
    }
}