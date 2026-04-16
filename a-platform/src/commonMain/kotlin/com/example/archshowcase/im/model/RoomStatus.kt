package com.example.archshowcase.im.model

sealed class RoomStatus {
    data object Idle : RoomStatus()
    data object Joining : RoomStatus()
    data class Joined(val roomId: String) : RoomStatus()
    data class Error(val message: String) : RoomStatus()
    data class KickedOut(val roomId: String, val reason: String) : RoomStatus()
}
