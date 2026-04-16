package com.example.archshowcase.core.di

import android.os.Build
import coil3.ComponentRegistry
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

internal actual fun addPlatformDecoders(builder: ComponentRegistry.Builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        builder.add(AnimatedImageDecoder.Factory())
    } else {
        builder.add(GifDecoder.Factory())
    }
}
