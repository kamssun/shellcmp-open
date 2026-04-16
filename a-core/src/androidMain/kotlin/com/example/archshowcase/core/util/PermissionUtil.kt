package com.example.archshowcase.core.util

import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.storage.STORAGE

suspend fun PermissionsController.requestStorageIfNeeded(): Boolean {
    return try {
        providePermission(Permission.STORAGE)
        true
    } catch (e: DeniedAlwaysException) {
        openAppSettings()
        false
    } catch (e: DeniedException) {
        false
    }
}
