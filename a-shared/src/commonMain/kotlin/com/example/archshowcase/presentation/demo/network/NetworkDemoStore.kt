package com.example.archshowcase.presentation.demo.network

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.utils.JvmSerializable
import com.example.archshowcase.compose.select.CustomState
import com.example.archshowcase.core.trace.restore.AppendOnlyHistory
import com.example.archshowcase.core.trace.restore.ReplayableState
import com.example.archshowcase.core.trace.user.UserTraceable
import com.example.archshowcase.replayable.VfResolvable
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.Intent
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.Label
import com.example.archshowcase.presentation.demo.network.NetworkDemoStore.State

@VfResolvable
interface NetworkDemoStore : Store<Intent, State, Label> {

    sealed interface Intent : JvmSerializable {
        data object MakeRequest : Intent
    }

    @CustomState
    data class State(
        val isLoading: Boolean = false,
        val requestCount: Int = 0,
        val result: String = "点击按钮发起请求",
        val error: String? = null,
        override val history: AppendOnlyHistory<NetworkHistoryRecord> = AppendOnlyHistory()
    ) : ReplayableState<NetworkHistoryRecord> {
        override fun hasValidData() = history.isNotEmpty()
        override fun createInitialState() = State()
    }

    sealed interface Label : JvmSerializable {
        data class RequestCompleted(val count: Int) : Label
        data class RequestFailed(val error: String) : Label
    }

    sealed interface Msg : JvmSerializable {
        data object LoadingStarted : Msg
        data class RequestSuccess(val count: Int, val result: String, val timestamp: Long) : Msg, UserTraceable {
            override fun toTraceString() = "RequestSuccess(count=$count)"
        }
        data class RequestFailed(val error: String) : Msg
    }
}
