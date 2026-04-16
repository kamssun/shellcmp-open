package com.example.archshowcase.core.trace.user

interface UserTraceable {
    fun toTraceString(): String
    fun toDebugString(): String = toString()
}
