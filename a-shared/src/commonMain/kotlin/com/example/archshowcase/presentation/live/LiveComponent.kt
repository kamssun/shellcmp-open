package com.example.archshowcase.presentation.live

import com.example.archshowcase.presentation.navigation.AppComponentContext
import com.example.archshowcase.rtc.di.loadRtcModule
import com.example.archshowcase.rtc.model.RtcState
import com.example.archshowcase.rtc.service.RtcService
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LiveComponent {
    val rtcState: StateFlow<RtcState>

    fun onBack()
}

class DefaultLiveComponent(
    context: AppComponentContext,
) : LiveComponent, AppComponentContext by context, KoinComponent {

    init {
        loadRtcModule()
    }

    private val rtcService: RtcService by inject()

    override val rtcState: StateFlow<RtcState> = rtcService.stateFlow

}
