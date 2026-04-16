package com.example.archshowcase.core.analytics

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

actual object AppLifecycleTracker {

    private var backgroundObserver: Any? = null
    private var foregroundObserver: Any? = null

    actual fun start() {
        val center = NSNotificationCenter.defaultCenter
        try {
            backgroundObserver = center.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { _ -> onAppBackground() }

            foregroundObserver = center.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = NSOperationQueue.mainQueue
            ) { _ -> onAppForeground() }
        } catch (e: Exception) {
            stop()
        }
    }

    actual fun stop() {
        val center = NSNotificationCenter.defaultCenter
        backgroundObserver?.let { center.removeObserver(it) }
        foregroundObserver?.let { center.removeObserver(it) }
        backgroundObserver = null
        foregroundObserver = null
    }
}
