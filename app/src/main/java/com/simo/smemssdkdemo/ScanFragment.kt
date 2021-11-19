package com.simo.smemssdkdemo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simo.smemssdk.*
import com.simo.smemssdkdemo.databinding.DeviceItemBinding
import com.simo.smemssdkdemo.databinding.FragmentScanBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber


class ScanFragment : BaseFragment(), SMEmsManagerDelegate {

    lateinit var binding: FragmentScanBinding
    val deviceData: MutableList<SMEmsDeviceModel> = mutableListOf()
    private lateinit var adapter: SearchAdapter
    lateinit var viewModel: EmsDeviceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_scan, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        SMEmsManager.defaultManager.addDelegate(this)
        setupViews()
    }

    var testScanAndAutoReconnect = false
    var lastSN: String? = null
    override fun onResume() {
        super.onResume()
//        if (SMEmsManager.defaultManager.isEnableAutoReconnect && lastSN?.isNotEmpty() == true) {
//            Timber.e("开启的自动重连, 不用操作")
//        }else

//        test()
//        return

        if (testScanAndAutoReconnect && lastSN?.isNotEmpty() == true){
            Timber.e("测试扫描连接")
            SMEmsManager.defaultManager.scanAndConnectDevice(lastSN!!, timeout = 15.0)
        }else {
            startSearchingDevice()
        }
    }

    fun test() {
        MainScope().async {
            showMessageHUD("正在准备")
            delay(1500)
            for (i in 0 until 100 step 2) {
                showProgressHUD(i, "正在升级", i.toString())
                delay(100)
            }
            showMessageHUD("升级完成", 3000L)
            delay(3000)
        }
    }

    private fun setupViews() {
        adapter = SearchAdapter(DeviceItemListener {
            didSelectDevice(it)
        })

        val vmFactory = EmsDeviceViewModelProvider(deviceData)
        viewModel = ViewModelProvider(this, vmFactory).get(EmsDeviceViewModel::class.java)
        viewModel.devices.observe(viewLifecycleOwner, Observer {
            Timber.i("Observer list changed ${it.size} and submitList")
            adapter.submitList(it)
        })

        val layoutManager = LinearLayoutManager(requireActivity())
        layoutManager.orientation = RecyclerView.VERTICAL

        binding.apply {

            resultListView.adapter = adapter
            resultListView.layoutManager = layoutManager
        }

        adapter.submitList(deviceData)
    }

    private fun startSearchingDevice() {
        Timber.d("startSearchingDevice")
        showLoadingHUD("Searching...")
        deviceData.clear()
        adapter.submitList(deviceData)
        Timber.e("开始扫描")
        SMEmsManager.defaultManager.startScanEmsDevice(1.0, 0.0)
    }

    override fun onPause() {
        super.onPause()
        SMEmsManager.defaultManager.stopScan()
        hideHUD()
    }

    override fun onStop() {
        super.onStop()
        SMEmsManager.defaultManager.removeDelegate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SMEmsManager.defaultManager.stopScan()
        hideHUD()
    }

    private fun didSelectDevice(deviceModel: SMEmsDeviceModel) {
        SMEmsManager.defaultManager.stopScan()
        Timber.i("didSelectDevice ${deviceModel.name}\nSN:${deviceModel.snCodeDisplay}")
        showLoadingHUD("Connecting...")
        Timber.e("开始连接设备")
        SMEmsManager.defaultManager.isEnableAutoReconnect = true
//        testScanAndAutoReconnect = true
        lastSN = deviceModel.snCode
        SMEmsManager.defaultManager.connectDevice(deviceModel.device!!, deviceModel)
    }

    private fun gotoConnected() {
        findNavController().navigate(R.id.connectedFragment)
//        findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToConnectedFragment())
    }

    override fun didDiscoverDevice(bleDevices: List<SMEmsDeviceModel>) {
        deviceData.clear()

//        deviceData.addAll(bleDevices)
        deviceData.addAll(bleDevices.filter { it.rssi > -55 })
        adapter.submitList(deviceData)
        binding.apply {
            if (bleDevices.size > 0) {
                hideHUD()
            }else {
                showLoadingHUD("Searching")
            }
        }
        viewModel.devices.value = deviceData.toList()
    }

    override fun deviceExecuteTimeout(bleDevice: SMEmsDevice?, action: SMEmsDeviceAction, msg: String?) {
        BLELog.w("Searching timeout, stop search. action:$action , msg:$msg")
//        hideHUD()
        showMessageHUD("Searching timeout, stop search. action:$action , msg:$msg")
    }

    override fun didConnectedDevice(isSuccess: Boolean, bleDevice: SMEmsDevice, error: String?) {
        SMEmsManager.defaultManager.stopScan()
        if (isSuccess) {
            gotoConnected()
        }else{
            showMessageHUD("Connect failed: $error")
        }
    }
}

class SearchAdapter(val clickListener: DeviceItemListener) : ListAdapter<SMEmsDeviceModel, SearchAdapter.DeviceItem>(EmsDevicesListDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceItem {
        return DeviceItem.from(parent)
    }

    override fun onBindViewHolder(holder: DeviceItem, position: Int) {
        val deviceModel = getItem(position)
        holder.bind(deviceModel, clickListener)
    }

    class DeviceItem(val binding: DeviceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: SMEmsDeviceModel, listener: DeviceItemListener) {
            binding.model = model
            binding.listener = listener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): DeviceItem {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = DeviceItemBinding.inflate(layoutInflater, parent, false)
                return DeviceItem(binding)
            }
        }
    }
}

class EmsDevicesListDiffCallback : DiffUtil.ItemCallback<SMEmsDeviceModel>() {
    override fun areItemsTheSame(oldItem: SMEmsDeviceModel, newItem: SMEmsDeviceModel): Boolean {
        return oldItem.snCode == newItem.snCode
    }

    override fun areContentsTheSame(oldItem: SMEmsDeviceModel, newItem: SMEmsDeviceModel): Boolean {
        return oldItem.device == newItem.device
    }
}

class DeviceItemListener(val clickListener: (model: SMEmsDeviceModel) -> Unit) {
    fun onClick(model: SMEmsDeviceModel) {
        clickListener(model)
    }
}

class CommonItemClickListener<T>(val clickListener: (model: T) -> Unit) {
    fun onClick(model: T) {
        clickListener(model)
    }
}

@BindingAdapter("wifiLevelImage")
fun ImageView.setWifiLevelImage(item: Int) {
    setImageResource(when (item) {
        0 -> R.drawable.wifi_0
        1 -> R.drawable.wifi_1
        2 -> R.drawable.wifi_2
        3 -> R.drawable.wifi_3
        else -> R.drawable.wifi_0
    })
}