package com.example.archshowcase

import com.example.archshowcase.attribution.AttributionBridge
import com.example.archshowcase.auth.LoginBridge
import com.example.archshowcase.devicetoken.DeviceTokenBridge
import com.example.archshowcase.im.ImBridge
import com.example.archshowcase.payment.PaymentBridge
import com.example.archshowcase.rtc.RtcBridge
import com.example.archshowcase.user.UserBridge

private var loginBridge: LoginBridge? = null
private var imBridge: ImBridge? = null
private var rtcBridge: RtcBridge? = null
private var paymentBridge: PaymentBridge? = null
private var userBridge: UserBridge? = null
private var deviceTokenBridge: DeviceTokenBridge? = null
private var attributionBridge: AttributionBridge? = null

fun setLoginBridge(bridge: LoginBridge) { loginBridge = bridge }
fun setImBridge(bridge: ImBridge) { imBridge = bridge }
fun setRtcBridge(bridge: RtcBridge) { rtcBridge = bridge }
fun setPaymentBridge(bridge: PaymentBridge) { paymentBridge = bridge }
fun setUserBridge(bridge: UserBridge) { userBridge = bridge }
fun setDeviceTokenBridge(bridge: DeviceTokenBridge) { deviceTokenBridge = bridge }
fun setAttributionBridge(bridge: AttributionBridge) { attributionBridge = bridge }

internal fun getLoginBridgeOrNull(): LoginBridge? = loginBridge
internal fun getImBridgeOrNull(): ImBridge? = imBridge
internal fun getRtcBridgeOrNull(): RtcBridge? = rtcBridge
internal fun getPaymentBridgeOrNull(): PaymentBridge? = paymentBridge
internal fun getUserBridgeOrNull(): UserBridge? = userBridge
internal fun getDeviceTokenBridgeOrNull(): DeviceTokenBridge? = deviceTokenBridge
internal fun getAttributionBridgeOrNull(): AttributionBridge? = attributionBridge
