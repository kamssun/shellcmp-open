package com.example.archshowcase.core.scheduler

import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.CoroutineName
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

private const val TAG = "OBOScheduler"

/**
 * OBO (One-By-One) Scheduler - Desktop 实现
 *
 * 使用 SwingUtilities.invokeLater 实现任务队列
 */
actual object OBOScheduler {
    private val queue = ConcurrentLinkedDeque<TaskWrapper>()
    private val isRunning = AtomicBoolean(false)

    private class TaskWrapper(
        val tag: String,
        val job: Job?,
        val task: () -> Unit,
        val exceptionHandler: CoroutineExceptionHandler?,
        val context: CoroutineContext?
    )

    actual fun schedule(tag: String, parentJob: Job?, task: () -> Unit) {
        queue.offer(TaskWrapper(tag, parentJob, task, null, null))
        if (isRunning.compareAndSet(false, true)) {
            SwingUtilities.invokeLater { processNext() }
        }
    }

    actual fun post(tag: String, task: () -> Unit) {
        schedule(tag, null, task)
    }

    actual fun postFirst(tag: String, task: () -> Unit) {
        queue.offerFirst(TaskWrapper(tag, null, task, null, null))
        if (isRunning.compareAndSet(false, true)) {
            SwingUtilities.invokeLater { processNext() }
        }
    }

    actual val dispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val tag = context[CoroutineName]?.name ?: "coroutine"
            val exceptionHandler = context[CoroutineExceptionHandler]
            // job 传 null：协程的 DispatchedContinuation 即使 Job 已取消也必须 resume，
            // 否则协程永远挂起，无法执行 CancellationException 传播 / finally cleanup。
            queue.offer(TaskWrapper(tag, null, { block.run() }, exceptionHandler, context))
            if (isRunning.compareAndSet(false, true)) {
                SwingUtilities.invokeLater { processNext() }
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

            if (wrapper.job?.isActive == false) continue

            try {
                wrapper.task()
            } catch (e: Throwable) {
                handleException(wrapper, e)
            }
            break
        }

        if (queue.isNotEmpty()) {
            SwingUtilities.invokeLater { processNext() }
        } else if (!tryStop()) {
            SwingUtilities.invokeLater { processNext() }
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
        val handler = wrapper.exceptionHandler
        if (handler != null && wrapper.context != null) {
            try {
                handler.handleException(wrapper.context, e)
                return
            } catch (_: Throwable) {
            }
        }
        Log.e(TAG, e) { "Task exception: ${e.message}" }
    }
}
