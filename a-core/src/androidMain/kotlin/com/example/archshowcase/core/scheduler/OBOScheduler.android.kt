package com.example.archshowcase.core.scheduler

import android.os.Handler
import android.os.Looper
import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private const val TAG = "OBOScheduler"

/**
 * OBO (One-By-One) Scheduler - Android 实现
 *
 * 使用 Handler 实现任务队列，每个任务执行完后 post 下一个，
 * 让用户交互消息有机会插队。
 */
actual object OBOScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private val queue = ConcurrentLinkedDeque<TaskWrapper>()
    private val isRunning = AtomicBoolean(false)
    private val _queueDepth = AtomicInteger(0)

    /** 当前正在执行的任务 tag（仅主线程读写，供 MessageMonitor 集成） */
    var currentTaskTag: String? = null
        internal set

    private val triggerNext = Runnable { processNext() }

    private class TaskWrapper(
        val tag: String,
        val job: Job?,
        val task: () -> Unit,
        val exceptionHandler: CoroutineExceptionHandler?,
        val context: CoroutineContext?
    )

    actual fun schedule(tag: String, parentJob: Job?, task: () -> Unit) {
        queue.offer(TaskWrapper(tag, parentJob, task, null, null))
        _queueDepth.incrementAndGet()
        if (isRunning.compareAndSet(false, true)) {
            handler.post(triggerNext)
        }
    }

    actual fun post(tag: String, task: () -> Unit) {
        schedule(tag, null, task)
    }

    actual fun postFirst(tag: String, task: () -> Unit) {
        queue.offerFirst(TaskWrapper(tag, null, task, null, null))
        _queueDepth.incrementAndGet()
        if (isRunning.compareAndSet(false, true)) {
            handler.post(triggerNext)
        }
    }

    actual val dispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val tag = context[CoroutineName]?.name ?: "coroutine"
            val exceptionHandler = context[CoroutineExceptionHandler]
            // job 传 null：协程 dispatch 的 Runnable 是 DispatchedContinuation，
            // 即使 Job 已取消也必须 resume 才能执行 CancellationException 传播 / finally cleanup。
            // 由 OBO skip 掉会导致协程永远挂起（collectLatest 内部 cancel 子 Job 触发此问题）。
            queue.offer(TaskWrapper(tag, null, { block.run() }, exceptionHandler, context))
            _queueDepth.incrementAndGet()
            if (isRunning.compareAndSet(false, true)) {
                handler.post(triggerNext)
            }
        }
    }

    private fun processNext() {
        while (true) {
            val wrapper = queue.poll()
            if (wrapper == null) {
                // 队列空 → 尝试停止，失败则重试
                if (tryStop()) return
                continue
            }

            // 跳过已取消的，继续下一个
            if (wrapper.job?.isActive == false) {
                _queueDepth.decrementAndGet()
                continue
            }

            _queueDepth.decrementAndGet()

            // 暴露 tag 给 MessageMonitor（<<<<< Finished 时读取）
            currentTaskTag = wrapper.tag

            // 执行任务，捕获异常
            if (AppConfig.enableOBODiagnostics) {
                val startNs = System.nanoTime()
                try {
                    wrapper.task()
                } catch (e: Throwable) {
                    handleException(wrapper, e)
                }
                OBODiagnostics.record(wrapper.tag, System.nanoTime() - startNs, _queueDepth.get())
            } else {
                try {
                    wrapper.task()
                } catch (e: Throwable) {
                    handleException(wrapper, e)
                }
            }
            break
        }

        if (queue.isNotEmpty()) {
            handler.post(triggerNext)
        } else if (!tryStop()) {
            handler.post(triggerNext)
        }
    }

    /**
     * 尝试停止处理循环。
     * set(false) 后 re-check，关闭竞态窗口。
     * @return true=安全停止，false=有新任务需继续
     */
    private fun tryStop(): Boolean {
        isRunning.set(false)
        return !(queue.isNotEmpty() && isRunning.compareAndSet(false, true))
    }

    private fun handleException(wrapper: TaskWrapper, e: Throwable) {
        // 尝试使用协程的 ExceptionHandler
        val handler = wrapper.exceptionHandler
        if (handler != null && wrapper.context != null) {
            try {
                handler.handleException(wrapper.context, e)
                return
            } catch (_: Throwable) {
                // handler 失败，继续下面的 fallback
            }
        }

        // 没有 handler 或 handler 失败，记录并继续
        Log.e(TAG, e) { "Task exception: ${e.message}" }
    }

}