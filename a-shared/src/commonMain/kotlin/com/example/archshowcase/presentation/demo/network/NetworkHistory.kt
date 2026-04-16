package com.example.archshowcase.presentation.demo.network

import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.core.trace.restore.appendHistory
import com.example.archshowcase.replayable.Replayable
import kotlinx.serialization.Serializable

@Serializable
sealed interface NetworkHistoryType : JvmSerializable {
    @Serializable
    data class Request(val count: Int, val result: String) : NetworkHistoryType

    @Serializable
    data class RequestFailed(val error: String) : NetworkHistoryType
}

@Replayable(stateClass = NetworkDemoStore.State::class)
@Serializable
data class NetworkHistoryRecord(
    val type: NetworkHistoryType,
    val timestamp: Long
) : JvmSerializable {

    fun applyToState(prevState: NetworkDemoStore.State): NetworkDemoStore.State = when (type) {
        is NetworkHistoryType.Request -> prevState.copy(
            isLoading = false,
            requestCount = type.count,
            result = type.result,
            error = null,
            history = prevState.appendHistory(this)
        )
        is NetworkHistoryType.RequestFailed -> prevState.copy(
            isLoading = false,
            error = type.error,
            history = prevState.appendHistory(this)
        )
    }

    fun toIntent(): Any = when (type) {
        is NetworkHistoryType.Request -> NetworkDemoStore.Intent.MakeRequest
        is NetworkHistoryType.RequestFailed -> NetworkDemoStore.Intent.MakeRequest
    }
}
