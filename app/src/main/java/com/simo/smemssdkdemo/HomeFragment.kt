package com.simo.smemssdkdemo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.simo.smemssdk.SMBLEState
import com.simo.smemssdk.SMEmsManager
import com.simo.smemssdk.SMEmsManagerDelegate
import com.simo.smemssdkdemo.databinding.FragmentHomeBinding


class HomeFragment : BaseFragment(), SMEmsManagerDelegate {

    lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        if (SMEmsManager.defaultManager.isBLEEnable) {
            binding.searchBtn.text = "Start Scan EMS Devices"
        }else {
            binding.searchBtn.text = "Request Open Bluetooth"
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchBtn.setOnClickListener {
            if (SMEmsManager.defaultManager.isBLEEnable) {
                findNavController().navigate(R.id.scanFragment)
            }else {
                SMEmsManager.defaultManager.autoRequestBLEPermission()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SMEmsManager.defaultManager.addDelegate(this)
    }

    override fun onStop() {
        super.onStop()
        SMEmsManager.defaultManager.removeDelegate(this)
    }

    override fun bleStateDidChanged(state: SMBLEState) {
        if (state == SMBLEState.powerOn) {
            binding.searchBtn.text = "Start Scan EMS Devices"
        }else {
            binding.searchBtn.text = "Request Open Bluetooth"
        }
    }


}