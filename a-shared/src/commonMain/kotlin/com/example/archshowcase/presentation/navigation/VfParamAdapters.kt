package com.example.archshowcase.presentation.navigation

import com.example.archshowcase.replayable.VfParamAdapter
import com.example.archshowcase.chat.model.WindowAnchor

@VfParamAdapter(forType = Route::class, fromString = "Route.fromSerialName")
@VfParamAdapter(forType = WindowAnchor::class, fromString = "WindowAnchor.fromString")
object VfParamAdapters
