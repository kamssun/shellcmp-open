package com.example.archshowcase.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource

interface StringProvider {
    @Composable
    fun getString(resource: StringResource): String

    @Composable
    fun getString(resource: StringResource, vararg args: Any): String
}

val LocalStringProvider = staticCompositionLocalOf<StringProvider> {
    error("No StringProvider provided")
}

@Composable
fun tr(res: StringResource): String = LocalStringProvider.current.getString(res)

@Composable
fun tr(res: StringResource, vararg args: Any): String = LocalStringProvider.current.getString(res, *args)
