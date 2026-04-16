package com.example.archshowcase.im.model

data class ImConfig(
    val isDebug: Boolean,
    val apiKey: String,
    val codeTag: String,
    val xlogKey: String,
    val memberId: String,
    val nickName: String
)
