package com.simo.smemssdkdemo

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.simo.smemssdk.SMEmsManager
import timber.log.Timber

const val REQUEST_ENABLE_BT = 1

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        SMEmsManager.defaultManager.autoRequestBLEPermission()
        Timber.w("BLE : ${SMEmsManager.defaultManager.isBLEEnable}")
    }

    fun requestPermissions() {
//            Timber.d("setupPermissions")
        val permissionReq = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permission to BLUETOOTH denied")
            permissionReq.add(Manifest.permission.BLUETOOTH)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permission to BLUETOOTH_ADMIN denied")
            permissionReq.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permission to ACCESS_FINE_LOCATION denied")
            permissionReq.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permission to READ_EXTERNAL_STORAGE denied")
            permissionReq.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permission to WRITE_EXTERNAL_STORAGE denied")
            permissionReq.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionReq.size > 0) {
//            ActivityCompat.requestPermissions(this, permissionReq.toTypedArray(), REQUEST_LOCATION)
            requestPermissions(permissionReq.toTypedArray(),
                REQUEST_ENABLE_BT
            )
        }
    }
}