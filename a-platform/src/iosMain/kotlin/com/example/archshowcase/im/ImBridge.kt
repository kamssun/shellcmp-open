package com.example.archshowcase.im

interface ImBridgeCallback {
    fun onSuccess(message: String)
    fun onError(code: Int, message: String)
}

interface ImMessageCallback {
    fun onMessage(
        id: String,
        content: String,
        senderId: String,
        roomId: String?,
        timestamp: Long,
        typeOrdinal: Int,
        rawJson: String?
    )
}

interface ImStatusCallback {
    fun onStatusChanged(statusOrdinal: Int)
}

interface ImRoomStatusCallback {
    fun onRoomStatusChanged(statusType: String, roomId: String?, message: String?)
}

interface ImHistoryCallback {
    fun onMessages(messagesJson: String)
    fun onError(code: Int, message: String)
}

interface ImBridge {
    fun initialize(
        isDebug: Boolean,
        apiKey: String,
        codeTag: String,
        xlogKey: String,
        memberId: String,
        nickName: String,
        token: String,
        imConfig: String,
        deviceId: String
    )

    fun login(callback: ImBridgeCallback)
    fun logout()
    fun isLoggedIn(): Boolean
    fun destroy()

    fun setStatusCallback(callback: ImStatusCallback?)
    fun setMessageCallback(callback: ImMessageCallback?)
    fun setRoomStatusCallback(callback: ImRoomStatusCallback?)

    fun enterRoom(roomId: String, callback: ImBridgeCallback)
    fun leaveRoom(roomId: String)

    fun pullHistory(convId: String, seqIdGte: Long, seqIdLte: Long, callback: ImHistoryCallback)
}
