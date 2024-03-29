package com.simo.smemssdkdemo

import android.graphics.Color
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by GrayLand119
 * on 2021/3/15
 */
open class BaseFragment: Fragment() {
    private var _lastText: String? = null
    private var _lastDetail: String? = null
    var isShowedLoading: Boolean = false
    var hud: KProgressHUD? = null
    private var hudStyle: KProgressHUD.Style? = KProgressHUD.Style.SPIN_INDETERMINATE
    private var delayJob: Job? = null
    fun showLoadingHUD(text: String = "Loading", calcelHandler: (()->Unit)? = null) {
        hideHUD()
        hudStyle = KProgressHUD.Style.SPIN_INDETERMINATE
        hud = KProgressHUD.create(activity)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel(text)
//                    .setDetailsLabel("Downloading data")
            .setCancellable {
                calcelHandler?.invoke()
            }
            .setAnimationSpeed(2)
            .setDimAmount(0.5f)
            .show()
        isShowedLoading = true
    }

    fun showMessageHUD(text: String = "Message", duration: Long = 1500) {
        hideHUD()

        val label = TextView(context)
        label.text = text
        label.setTextColor(Color.WHITE)
        label.textSize = 16F
        hudStyle = null
        hud = KProgressHUD.create(activity)
            .setCustomView(label)
            .show()
        hud?.isCustom = true
        delayJob = GlobalScope.launch {
            delay(duration)
            hud?.dismiss()
        }
    }

    fun showProgressHUD(progress: Int = 0, text: String = "Please Wait", detail: String? = null, forceUpdate: Boolean = false) {

        _lastText = text
        _lastDetail = detail

        if (hud != null &&
            hudStyle != KProgressHUD.Style.ANNULAR_DETERMINATE) {
            hideHUD()
        } else if (hudStyle == KProgressHUD.Style.ANNULAR_DETERMINATE && !forceUpdate) {
            hud?.setProgress(progress)
            hud?.setLabel(text)
            hud?.setDetailsLabel(detail ?: "")
            return
        }
        hideHUD()
        hudStyle = KProgressHUD.Style.ANNULAR_DETERMINATE
        hud = KProgressHUD.create(activity)
            .setStyle(hudStyle)
            .setLabel(text)
            .setMaxProgress(100)
        if (detail?.isNotEmpty() == true) {
            hud?.setDetailsLabel(detail)
        }
        hud?.setProgress(progress)
        hud?.show()
    }

    fun updateProgressHUD(progress: Int) {
        hud?.setProgress(progress)
    }

    fun hideHUD() {
        isShowedLoading = false
        hud?.dismiss()
        delayJob?.cancel()
    }
}

private var _isCustom: Boolean = false
var KProgressHUD.isCustom: Boolean
    get() { return _isCustom
    }
    set(value) { _isCustom = value }