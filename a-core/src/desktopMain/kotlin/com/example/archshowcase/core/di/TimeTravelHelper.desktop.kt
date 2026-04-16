package com.example.archshowcase.core.di

import com.arkivanov.mvikotlin.timetravel.server.TimeTravelServer
import com.example.archshowcase.core.util.Log
import javax.swing.SwingUtilities

private const val TAG = "TimeTravel"
private var timeTravelServer: TimeTravelServer? = null

internal actual fun startTimeTravelServer() {
    if (timeTravelServer == null) {
        timeTravelServer = TimeTravelServer(
            runOnMainThread = { SwingUtilities.invokeLater(it) }
        ).also { it.start() }
        Log.d(TAG) { "Server started" }
    }
}
