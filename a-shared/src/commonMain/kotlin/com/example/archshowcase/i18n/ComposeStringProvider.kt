package com.example.archshowcase.i18n

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

class ComposeStringProvider : StringProvider {

    @Composable
    override fun getString(resource: StringResource): String = stringResource(resource)

    @Composable
    override fun getString(resource: StringResource, vararg args: Any): String =
        stringResource(resource, *args)
}
