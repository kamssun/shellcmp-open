package com.example.archshowcase.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.tooling.CompositionErrorContext
import androidx.compose.runtime.tooling.LocalCompositionErrorContext
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.scheduler.OBOScheduler
import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TAG = "OBOEffect"

/**
 * OBO 版 LaunchedEffect 的实现类
 *
 * 参考 LaunchedEffectImpl，使用 RememberObserver 管理生命周期：
 * - onRemembered: 进入 composition 时启动协程
 * - onForgotten: 离开 composition 时取消协程
 * - onAbandoned: composition 失败时取消协程
 */
private class OBOLaunchedEffectImpl(
    private val parentContext: CoroutineContext,
    private val errorContext: CompositionErrorContext?,
    private val debugTag: String?,
    private val task: suspend CoroutineScope.() -> Unit
) : RememberObserver, CoroutineExceptionHandler {

    // scope 使用 Compose dispatcher 而非 OBOScheduler.dispatcher，
    // 因为 scrollToItem 等帧耦合 API 依赖 MonotonicFrameClock 触发布局刷新，如果被OBO编排进队列，可能导致帧信号与实际执行脱节、视觉不更新。
    // 现在OBO 只管编排任务启动时机（onRemembered → OBOScheduler.schedule）；
    // 如果是挂起恢复后走 Compose dispatcher 主线程 immediate 执行的代码，如需让出 CPU 在挂起点内部再用 OBO 包裹即可。
    private val scope = CoroutineScope(
        parentContext + this + CoroutineName(debugTag ?: "effect")
    )
    private var job: Job? = null
    private var forgotten = false
    private val tag get() = "${debugTag ?: "effect"}-${hashCode()}"

    override fun onRemembered() {
        job?.cancel(CancellationException("Old job was still running!"))
        forgotten = false
        // OBO 调度启动时机：进入复杂页面时多个 Effect 不会同时 launch
        OBOScheduler.schedule(tag, scope.coroutineContext[Job]) {
            if (!forgotten) {
                job = scope.launch(block = task)
            }
        }
    }

    override fun onForgotten() {
        forgotten = true
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }

    override fun onAbandoned() {
        forgotten = true
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }

    // CoroutineExceptionHandler 实现
    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler.Key

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        errorContext?.run { exception.attachComposeStackTrace(this@OBOLaunchedEffectImpl) }
        Log.e(TAG, exception) { "[$tag] exception: ${exception.message}" }
    }
}

/**
 * 离开 composition 时的取消异常
 */
private class LeftCompositionCancellationException :
    CancellationException("The coroutine scope left the composition")

/**
 * OBO 版本的 LaunchedEffect
 *
 * 当 [AppConfig.useOBOScheduler] 开启时，使用 OBO 调度；
 * 否则走系统默认调度。
 *
 * @param key1 Effect 的 key，key 变化时会重新启动
 * @param debugTag 可选的调试标识，崩溃时会显示在日志中
 * @param block 要执行的挂起函数
 */
@Composable
fun OBOLaunchedEffect(
    key1: Any?,
    debugTag: String? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    if (AppConfig.useOBOScheduler) {
        val scope = rememberCoroutineScope()
        val errorContext = LocalCompositionErrorContext.current
        remember(key1) {
            OBOLaunchedEffectImpl(scope.coroutineContext, errorContext, debugTag, block)
        }
    } else {
        LaunchedEffect(key1, block)
    }
}

/**
 * OBO 版本的 LaunchedEffect（双 key）
 */
@Composable
fun OBOLaunchedEffect(
    key1: Any?,
    key2: Any?,
    debugTag: String? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    if (AppConfig.useOBOScheduler) {
        val scope = rememberCoroutineScope()
        val errorContext = LocalCompositionErrorContext.current
        remember(key1, key2) {
            OBOLaunchedEffectImpl(scope.coroutineContext, errorContext, debugTag, block)
        }
    } else {
        LaunchedEffect(key1, key2, block)
    }
}

/**
 * OBO 版本的 LaunchedEffect（三 key）
 */
@Composable
fun OBOLaunchedEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    debugTag: String? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    if (AppConfig.useOBOScheduler) {
        val scope = rememberCoroutineScope()
        val errorContext = LocalCompositionErrorContext.current
        remember(key1, key2, key3) {
            OBOLaunchedEffectImpl(scope.coroutineContext, errorContext, debugTag, block)
        }
    } else {
        LaunchedEffect(key1, key2, key3, block)
    }
}

/**
 * OBO 版本的 LaunchedEffect（vararg keys）
 */
@Composable
fun OBOLaunchedEffect(
    vararg keys: Any?,
    debugTag: String? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    if (AppConfig.useOBOScheduler) {
        val scope = rememberCoroutineScope()
        val errorContext = LocalCompositionErrorContext.current
        remember(*keys) {
            OBOLaunchedEffectImpl(scope.coroutineContext, errorContext, debugTag, block)
        }
    } else {
        LaunchedEffect(keys = keys, block = block)
    }
}
