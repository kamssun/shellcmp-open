package com.example.archshowcase.core.di

import com.arkivanov.mvikotlin.timetravel.server.TimeTravelServer
import com.example.archshowcase.core.util.Log

private const val TAG = "TimeTravel"
private var timeTravelServer: TimeTravelServer? = null

internal actual fun startTimeTravelServer() {
    if (timeTravelServer == null) {
        timeTravelServer = TimeTravelServer().also { it.start() }
        Log.d(TAG) { "Server started" }
    }
}
