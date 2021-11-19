package com.simo.smemssdkdemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableStringBuilder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import com.simo.smemssdk.*
import com.simo.smemssdkdemo.databinding.FragmentConnectedBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.*
import kotlin.math.floor

/**
 * Created by GrayLand119
 * on 2021/3/15
 */
@ExperimentalUnsignedTypes
class ConnectedFragment : BaseFragment(), SMEmsManagerDelegate {

    var binding: FragmentConnectedBinding? = null

    var enabledExerciseModes: MutableList<SMDeviceExerciseMode> = mutableListOf()
    var maxIntensity = 32
    var minIntensity = 1
    var isAerobicEnabled = true
    var isMuscleEnabled = true
    var isMassageEnabled = true
    var isDragging = false
    var exerciseSeconds = 0
        set(value) {
            binding?.remainSecLabel?.text = secondsToMMSS(value)
            field = value
        }
    /// 当前运行模式
    var currentRunMode: SMDeviceRunMode = SMDeviceRunMode.none
        set(value) {
            field = value
            when(currentRunMode) {
                SMDeviceRunMode.working -> {
                    binding?.runModeTab?.setScrollPosition(0, 0f, true)
                }
                SMDeviceRunMode.paused -> {
                    binding?.runModeTab?.setScrollPosition(1, 0f, true)
                }
                SMDeviceRunMode.stopped -> {
                    binding?.runModeTab?.setScrollPosition(2, 0f, true)
                }
                else -> {}
            }
        }

    /// 当前训练模式
    var currentExerciseMode: SMDeviceExerciseMode = SMDeviceExerciseMode.none
        set(value) {
            field = value
            var findIndex: Int = -1
            for (index in enabledExerciseModes.indices) {
                val ele = enabledExerciseModes[index]
                if (ele == value) {
                    findIndex = index
                }
            }

            if (findIndex != -1) {
                binding?.exerciseModeTab?.setScrollPosition(findIndex, 0f, true)
//                binding?.exerciseModeTab.selectedTabPosition = findIndex
            }

            adjustMaxIntensity()
        }
    /// 当前训练强度
    var currentIntensity = 1
        set(value) {
            field = value
            binding?.progressValueLabel?.text = value.toString()
            binding?.intensitySlider?.value = value.toFloat()
        }

    val map32To9: Map<Int, Int> = mapOf(
    1 to 1, 2 to 1, 3 to 1,
    4 to 2, 5 to 2, 6 to 2, 7 to 2,
    8 to 3, 9 to 3, 10 to 3,
    11 to 4, 12 to 4, 13 to 4, 14 to 4,
    15 to 5, 16 to 5, 17 to 5,
    18 to 6, 19 to 6, 20 to 6, 21 to 6,
    22 to 7, 23 to 7, 24 to 7,
    25 to 8, 26 to 8, 27 to 8, 28 to 8,
    29 to 9, 30 to 9, 31 to 9, 32 to 9,
    )
    var map9To32: Map<Int,Int> = mapOf(1 to 1, 2 to 4, 3 to 8, 4 to 11, 5 to  15, 6 to 18, 7 to 22, 8 to  25, 9 to  29)

    private var _cdTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SMEmsManager.defaultManager.addDelegate(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connected, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SMEmsManager.defaultManager.currentDevice?.let {
            binding!!.configBtn.isEnabled = it.isConnected && it.fwVersion > 1.7
            if (it.presetConfigIndex >= 0) {
                binding!!.configText.text = SpannableStringBuilder(it.presetConfigIndex.toString())
            }
        }

        binding?.intensitySlider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
                Timber.d("onStartTrackingTouch")
                isDragging = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
                Timber.d("h(slider: Slider")
                isDragging = false
                setIntensity(currentIntensity,  true)
            }
        })
        binding?.intensitySlider?.addOnChangeListener { slider, value, fromUser ->
            // Responds to when slider's value is changed
            currentIntensity = value.toInt()
            Timber.d("intensityValueChanged: $currentIntensity")
        }

        binding?.exerciseModeTab?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab select
                val index = tab?.position ?: 0

                val mode = enabledExerciseModes[index]
                changeExerciseMode( mode,true)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselect
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })

        binding?.addSecBtn?.setOnClickListener {
            exerciseSeconds += 60
        }

        binding?.minusSecBtn?.setOnClickListener {
            if (exerciseSeconds > 60) {
                exerciseSeconds -= 60
            }
        }

        binding?.configBtn?.setOnClickListener {
            val configIndex = binding?.configText?.text.toString().toIntOrNull() ?: 0
            if (configIndex > 7 || configIndex < 0) {
                binding!!.configText.text = SpannableStringBuilder(SMEmsManager.defaultManager.currentDevice?.presetConfigIndex.toString())
                showMessageHUD("请输入正确的参数")
                return@setOnClickListener
            }
            SMEmsManager.defaultManager.currentDevice?.setPresetConfig(configIndex)
            showMessageHUD("设置成功")
        }

        binding?.runModeTab?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Timber.i("onTabSelected")
                // Handle tab select
                val index = tab?.position ?: 0
                when(index) {
                    0 -> {
                        Timber.d("开始/继续")
                        startWorking(true)
                    }
                    1 -> {
                        Timber.d("暂停")
                        pauseWorking(true)
                    }
                    2 -> {
                        Timber.d("停止")
                        stoppWorking(true)
                    }
                    3 -> {
                        Timber.d("只发开始指令")
                        startWorkingOnly(true)
                    }
                    else -> {}
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding?.otaBtn?.setOnClickListener {
            MainScope().async {
                startOTA()
            }
        }

        /// Switch
        binding!!.aerobicSwitch.isChecked = true
        binding!!.muscleSwitch.isChecked = true
        binding!!.massageSwitch.isChecked = true
        binding?.aerobicSwitch?.setOnCheckedChangeListener { compoundButton: CompoundButton, isChecked: Boolean ->

            if (isAerobicEnabled != isChecked) {
                updateEnbaleExerciseModes(true,  true)
            }
            isAerobicEnabled = isChecked
            Timber.d("aerobicSwitch : $isAerobicEnabled")
        }
        binding?.muscleSwitch?.setOnCheckedChangeListener { compoundButton: CompoundButton, isChecked: Boolean ->

            if (isMuscleEnabled != isChecked) {
                isMuscleEnabled = isChecked
                updateEnbaleExerciseModes(true,  true)
            }
            Timber.d("muscleSwitch : $isMuscleEnabled")
        }
        binding?.massageSwitch?.setOnCheckedChangeListener { compoundButton: CompoundButton, isChecked: Boolean ->
            if (isMassageEnabled != isChecked) {
                isMassageEnabled = isChecked
                updateEnbaleExerciseModes(true,  true)
            }
            Timber.d("massageSwitch : $isMassageEnabled")
        }

        /// 默认 1200 秒 => 20 分钟,
        exerciseSeconds = 1200

        // 初始默认强度
        setIntensity( 1,false)
        // 初始默认工作状态
        stoppWorking( false)

        // 读取模式使能,  默认都开启,  若没有设定则不需要读取
//        SMEmsManager.defaultManager.currentDevice?.readExerciseModeStatus( SMDeviceExerciseMode.aerobic, null)
//        SMEmsManager.defaultManager.currentDevice?.readExerciseModeStatus( SMDeviceExerciseMode.muscle, null)
//        SMEmsManager.defaultManager.currentDevice?.readExerciseModeStatus( SMDeviceExerciseMode.massage, null)

        if (isAerobicEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.aerobic)
            binding?.aerobicSwitch?.isChecked = true
        }
        if (isMuscleEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.muscle)
            binding?.muscleSwitch?.isChecked = true
        }
        if (isMassageEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.massage)
            binding?.massageSwitch?.isChecked = true
        }

        enabledExerciseModes.add(SMDeviceExerciseMode.extend1)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend2)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend3)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend4)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend5)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend6)

        // 初始化模式, 默认都开启,  若没有设定则不需要初始化, 初始化会发送 3 条开启命令.
//        updateEnbaleExerciseModes(false,  false)
    }

    @SuppressLint("Assert")
    private fun apiTest() {
        MainScope().async {
            val curDevice = SMEmsManager.defaultManager.currentDevice!!
            curDevice.setVoltageConfig(0)
            curDevice.readPresetConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==0xF0) { "setVoltageConfig Error1! value:$value" }
            }
            delay(300)
            curDevice.setVoltageConfig(7)
            curDevice.readPresetConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==0xF7) { "setVoltageConfig Error2! value:$value" }
            }
            delay(300)
            curDevice.setPresetConfig(0)
            curDevice.readPresetConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==0x00) { "setPresetConfig Error1! value: $value" }
            }
            delay(300)
            curDevice.setPresetConfig(7)
            curDevice.readPresetConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==0x07) { "setPresetConfig Error2! value:$value" }
            }
            delay(300)
            curDevice.setMinPWConfig(10)
            curDevice.readMinPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==10) { "setMinPWConfig Error1! value:$value" }
            }
            delay(300)
            curDevice.setMinPWConfig(250)
            curDevice.readMinPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==250) { "setMinPWConfig Error2! value:$value" }
            }

            delay(300)
            curDevice.setMaxPWConfig(40)
            curDevice.readMaxPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==40) { "setMaxPWConfig Error1! value=$value" }
            }
            delay(300)
            curDevice.setMaxPWConfig(300)
            curDevice.readMaxPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==300) { "setMaxPWConfig Error12 value:$value" }
            }

            delay(300)
            curDevice.setAscPWConfig(2)
            curDevice.readAscPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==2) { "setAscPWConfig Error1! value:$value" }
            }
            delay(300)
            curDevice.setAscPWConfig(250)
            curDevice.readAscPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==250) { "setAscPWConfig Error2! value:$value" }
            }
            delay(300)
            curDevice.setNormalPWConfig(10)
            curDevice.readNormalPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==10) { "setNormalPWConfig Error2! value:$value" }
            }
            delay(300)
            curDevice.setNormalPWConfig(300)
            curDevice.readNormalPWConfig { isSuccessfully, errorCode, errorDesc, value ->
                assert(value==300) { "setNormalPWConfig Error2! value:$value" }
            }

            BLELog.d("Run All Test Finished.")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        SMEmsManager.defaultManager.removeDelegate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.e("手动断开连接")
        SMEmsManager.defaultManager.removeDelegate(this)
        SMEmsManager.defaultManager.disconnectCurrentDevice()
    }

    fun secondsToMMSS(seconds: Int) : String {
        val mm: Int = floor(seconds.toFloat() / 60.0).toInt()
        val ss: Int = seconds % 60
        return  "%02d:%02d".format(mm, ss)
    }

    fun updateEnbaleExerciseModes(sendCmd: Boolean, animated: Boolean = false) {
        enabledExerciseModes.clear()
        if (isAerobicEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.aerobic)
        }
        SMEmsManager.defaultManager.currentDevice?.setExerciseModeEnable(SMDeviceExerciseMode.aerobic, isAerobicEnabled, null)

        if (isMuscleEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.muscle)
        }
        SMEmsManager.defaultManager.currentDevice?.setExerciseModeEnable(SMDeviceExerciseMode.muscle, isMuscleEnabled, null)

        if (isMassageEnabled) {
            enabledExerciseModes.add(SMDeviceExerciseMode.massage)
        }
        SMEmsManager.defaultManager.currentDevice?.setExerciseModeEnable(SMDeviceExerciseMode.massage, isMassageEnabled, null)


        if (enabledExerciseModes.size == 0) {
            Timber.e("至少要开启 1 个模式!!!")
            isAerobicEnabled = true
            enabledExerciseModes.add(SMDeviceExerciseMode.aerobic)
            binding!!.aerobicSwitch.isChecked = true
            SMEmsManager.defaultManager.currentDevice?.setExerciseModeEnable(SMDeviceExerciseMode.aerobic,isAerobicEnabled,null)
        }

        enabledExerciseModes.add(SMDeviceExerciseMode.extend1)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend2)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend3)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend4)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend5)
        enabledExerciseModes.add(SMDeviceExerciseMode.extend6)
        // Update Segment!!
        binding!!.exerciseModeTab.removeAllTabs()
//        exerciseModeSegment.removeAllSegments()
        binding!!
        for (ele in enabledExerciseModes) {
            when (ele) {
                SMDeviceExerciseMode.aerobic -> {

                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "有氧"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.muscle -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "增肌"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.massage -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "按摩"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend1 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩1"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend2 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩2"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend3 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩3"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend4 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩4"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend5 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩5"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                SMDeviceExerciseMode.extend6 -> {
                    val tab = binding!!.exerciseModeTab.newTab()
                    tab.text = "扩6"
                    binding!!.exerciseModeTab.addTab(tab)
                }
                else -> {}
            }
        }

        // Apply ExrciseMode
        changeExerciseMode(enabledExerciseModes.firstOrNull() ?: SMDeviceExerciseMode.aerobic, sendCmd)
    }

    fun updateDeviceInfo(device: SMEmsDevice)  {
        var infoDesc = ""
        infoDesc += "设备名称: %s \n".format(device.name)
        infoDesc += "设备SN: %s \n".format(device.sn)
        infoDesc += "固件版本: %s \n".format(device.fwVersion.toString())
        infoDesc += "硬件版本: %s \n".format(device.hwVersion.toString())
        infoDesc += "剩余电量: %s \n".format(device.battery.toString())
        infoDesc += "运行状态: %s \n".format(device.runMode.toString())
        infoDesc += "当前模式: %s \n".format(device.excerciseMode.toString())
        infoDesc += "当前模式信号强度: %s \n".format(device.intensity.toString())
        infoDesc += "充电状态: %s \n".format(device.chargeState.toString())
        infoDesc += "底座状态: %s \n".format(device.hubState.toString())
        infoDesc += "预设配置: %s \n".format(device.presetConfigIndex.toString())
        infoDesc += "剩余运动时长(设备内计时器): %s \n".format(device.remainSeconds.toString())

        binding!!.deviceInfoLabel.text = infoDesc
    }

    fun startWorking(sendCmd: Boolean) {
        currentRunMode = SMDeviceRunMode.working

                if (exerciseSeconds <= 0) {
                    exerciseSeconds = 1200
                }

        _cdTimer?.cancel()
        _cdTimer = null
        _cdTimer = object: CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
//                Timber.i("CD Timer onTick")
                exerciseSeconds -= 1
                if (exerciseSeconds <= 0) {
                    _cdTimer?.cancel()
                    _cdTimer = null

                    stoppWorking(sendCmd = true)
                }
            }
            override fun onFinish() {
            }
        }
        _cdTimer?.start()

        if (sendCmd) {
            // Set Mode Only
//            SMEmsManager.defaultManager.currentDevice?.setRunMode(mode: SMDeviceRunMode.working, null)
            // Set Multi-Property, can sync exerciseSeconds to EMS device.
            SMEmsManager.defaultManager.currentDevice?.setCommandCommit(currentExerciseMode, exerciseSeconds, intensity = currentIntensity, runMode = currentRunMode, null)
        }
    }

    fun pauseWorking(sendCmd: Boolean) {
        currentRunMode = SMDeviceRunMode.paused

        _cdTimer?.cancel()
        _cdTimer = null

        if (sendCmd) {
            SMEmsManager.defaultManager.currentDevice?.setRunMode(mode = SMDeviceRunMode.paused, null)
        }
    }

    fun startWorkingOnly(sendCmd: Boolean) {
        currentRunMode = SMDeviceRunMode.working
        _cdTimer?.cancel()
        _cdTimer = null
        _cdTimer = object: CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
//                Timber.i("CD Timer onTick")
                exerciseSeconds -= 1
                if (exerciseSeconds <= 0) {
                    _cdTimer?.cancel()
                    _cdTimer = null

                    stoppWorking(sendCmd = true)
                }
            }
            override fun onFinish() {
            }
        }
        _cdTimer?.start()

        if (sendCmd) {
            // Set Mode Only
            SMEmsManager.defaultManager.currentDevice?.setRunMode(SMDeviceRunMode.working, null)
        }
    }
    fun stoppWorking(sendCmd: Boolean) {
        currentRunMode = SMDeviceRunMode.stopped

        _cdTimer?.cancel()
        _cdTimer = null

        exerciseSeconds = 1200

        if (sendCmd) {
            SMEmsManager.defaultManager.currentDevice?.setRunMode(mode = SMDeviceRunMode.stopped, null)
        }
    }

    fun changeExerciseMode(mode: SMDeviceExerciseMode, sendCmd: Boolean) {
        currentExerciseMode = mode
        if (sendCmd) {
            SMEmsManager.defaultManager.currentDevice?.setExerciseMode(mode = currentExerciseMode, null)
        }
    }

    /// 测试扩展模式,  强度可以自己调整,
    fun testExtendMode(mode: SMDeviceExerciseMode) {
        currentExerciseMode = mode
        currentIntensity = 5

        SMEmsManager.defaultManager.currentDevice?.setCommandCommit( currentExerciseMode,  exerciseSeconds, intensity = 5, runMode = SMDeviceRunMode.working, null)
    }

    fun adjustMaxIntensity() {
        var toValue = currentIntensity
        if (currentExerciseMode == SMDeviceExerciseMode.massage ||
        currentExerciseMode == SMDeviceExerciseMode.extend2 ||
        currentExerciseMode == SMDeviceExerciseMode.extend3 ||
        currentExerciseMode == SMDeviceExerciseMode.extend4 ||
        currentExerciseMode == SMDeviceExerciseMode.extend5 ||
        currentExerciseMode == SMDeviceExerciseMode.extend6){
            if (maxIntensity > 9) {
                maxIntensity = 9
                toValue = map32To9[currentIntensity] ?: 1
            }

        }else {
            if (maxIntensity <= 9) {
                maxIntensity = 32
                toValue = map9To32[currentIntensity] ?: 1
            }
        }

        binding?.intensitySlider?.valueTo = maxIntensity.toFloat()
        binding?.intensitySlider?.value = toValue.toFloat()

        currentIntensity = toValue
    }

    fun setIntensity(intensity: Int, sendCmd: Boolean) {
        currentIntensity = intensity

        if (sendCmd) {
            SMEmsManager.defaultManager.currentDevice?.setIntensity(currentIntensity, null)
            SMEmsManager.defaultManager.currentDevice?.intensity = currentIntensity.toInt()
        }
    }

    suspend fun startOTA() {
        val binData = loadDFUFile()
        if (binData == null) {
            showMessageHUD("升级包不存在或加载失败")
            return
        }
        
        showLoadingHUD("准备升级")
        SMEmsManager.defaultManager.currentDevice?.startOTA(binData,
            { errorDesc ->
                MainScope().async {
                    showMessageHUD(errorDesc!!)
                }
            }, { p ->
                MainScope().async {
                    val progress = floor(p!! * 100).toInt()
                    Timber.d("ota progress:${progress}, p: ${p}")
                    showProgressHUD(progress, "正在升级", "${progress}%")
                }
            },
            {
                MainScope().async {
                    showMessageHUD("升级完成", 3000L)
                    delay(3000L)
                    SMEmsManager.defaultManager.disconnectCurrentDevice()
                    findNavController().navigateUp()
                }
            }
        )
    }

    fun loadDFUFile(): ByteArray {
//        val dataS = requireContext().assets.open("EMS_HW1001_SW1500.smbin")
        val dataS = requireContext().assets.open("EMS_HW1001_SW1704.smbin")
        val rawData = dataS.readBytes()
        Timber.i("Bin Data: ${rawData.size}")
        return rawData
    }

    /** SMEmsManagerDelegate */

    override fun didConnectedDevice(isSuccess: Boolean, bleDevice: SMEmsDevice, error: String?) {
        if (isSuccess) {
            Timber.d("设备连接成功/重连成功")
            showMessageHUD("设备连接成功/重连成功")
        }else {
            Timber.d("设备连接成功/重连失败")
            showMessageHUD("设备连接成功/重连失败")
        }
    }

    override fun didDisconnectedDevice(bleDevice: SMEmsDevice?, error: String?) {
        Timber.d("设备连接已断开")
        showMessageHUD("设备连接已断开")
    }

    override fun didStartReconnectDevice(bleDevice: SMEmsDevice) {
        Timber.d("正在重连...")
        showLoadingHUD("正在重连...")
    }

    override fun deviceExecuteTimeout(
        bleDevice: SMEmsDevice?,
        action: SMEmsDeviceAction,
        msg: String?
    ) {
        super.deviceExecuteTimeout(bleDevice, action, msg)
    }

    override fun bleStateDidChanged(state: SMBLEState) {
        if (state == SMBLEState.powerOff) {
            Timber.d("蓝牙已关闭")
            showMessageHUD("蓝牙已关闭")
        }else if (state == SMBLEState.powerOn) {
            Timber.d("蓝牙已打开")
            showMessageHUD("蓝牙已打开")
        }
    }

    override fun didUpdateStatus(device: SMEmsDevice) {
        // RunMode Changed
        if (device.runMode != currentRunMode) {
            // Update Local RunMode
            currentRunMode = device.runMode

            if (device.runMode == SMDeviceRunMode.stopped) {
                _cdTimer?.cancel()
                _cdTimer = null
                exerciseSeconds = 1200
            }else if (device.runMode == SMDeviceRunMode.paused) {
                _cdTimer?.cancel()
                _cdTimer = null
            }else if (device.runMode == SMDeviceRunMode.working) {
                startWorking(false)
            }
        }

        // ExerciseMode Changed
        if (device.excerciseMode != currentExerciseMode) {
            changeExerciseMode( device.excerciseMode,  false)
        }

        // Intensity Changed
        if (device.intensity != currentIntensity && isDragging == false) {
            setIntensity(device.intensity, false)
        }

        updateDeviceInfo(device)
    }
}