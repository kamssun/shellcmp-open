package com.example.archshowcase.core.scheduler

import com.example.archshowcase.core.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.CoroutineContext

private const val TAG = "OBOScheduler"

/**
 * OBO (One-By-One) Scheduler - iOS 实现
 *
 * 借鉴 DeliQueue 的写入/消费分离架构：
 * - 写入端：Treiber Stack（无锁，CAS 原子操作，O(1)）
 * - 消费端：drain 到本地 ArrayDeque（主线程单线程，零同步）
 */
actual object OBOScheduler {
    /** 写入端：无锁 Treiber Stack 栈顶 */
    private val stackTop = AtomicReference<Node?>(null)

    /** 消费端：主线程本地队列（单线程访问，无需同步） */
    private val localQueue = ArrayDeque<TaskWrapper>()

    /** 处理状态：0=idle, 1=running，CAS 控制 */
    private val running = AtomicInt(0)

    private class Node(val wrapper: TaskWrapper, val next: Node?)

    private class TaskWrapper(
        val tag: String,
        val job: Job?,
        val task: () -> Unit,
        val exceptionHandler: CoroutineExceptionHandler?,
        val context: CoroutineContext?
    )

    // ---- 写入端（任何线程，无锁） ----

    /** CAS 压入 Treiber Stack，O(1) */
    private fun push(wrapper: TaskWrapper) {
        while (true) {
            val oldTop = stackTop.value
            if (stackTop.compareAndSet(oldTop, Node(wrapper, oldTop))) return
        }
    }

    actual fun schedule(tag: String, parentJob: Job?, task: () -> Unit) {
        push(TaskWrapper(tag, parentJob, task, null, null))
        if (running.compareAndSet(0, 1)) {
            dispatchNext()
        }
    }

    actual fun post(tag: String, task: () -> Unit) {
        schedule(tag, null, task)
    }

    actual fun postFirst(tag: String, task: () -> Unit) {
        // iOS 无 Handler 拦截场景，走普通调度
        schedule(tag, null, task)
    }

    actual val dispatcher: CoroutineDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val exceptionHandler = context[CoroutineExceptionHandler]
            // job 传 null：协程的 DispatchedContinuation 即使 Job 已取消也必须 resume，
            // 否则协程永远挂起，无法执行 CancellationException 传播 / finally cleanup。
            push(TaskWrapper("coroutine", null, { block.run() }, exceptionHandler, context))
            if (running.compareAndSet(0, 1)) {
                dispatchNext()
            }
        }
    }

    // ---- 消费端（仅主线程） ----

    private fun dispatchNext() {
        dispatch_async(dispatch_get_main_queue()) { processNext() }
    }

    /** 原子地 drain 整个 Treiber Stack 到本地队列（LIFO → FIFO 反转） */
    private fun drainToLocal() {
        // CAS 取走整条链
        var head: Node?
        while (true) {
            head = stackTop.value ?: return
            if (stackTop.compareAndSet(head, null)) break
        }
        // 反转：Stack 是 LIFO，翻转为 FIFO
        val batch = mutableListOf<TaskWrapper>()
        var cur = head
        while (cur != null) {
            batch.add(cur.wrapper)
            cur = cur.next
        }
        for (i in batch.lastIndex downTo 0) {
            localQueue.addLast(batch[i])
        }
    }

    private fun processNext() {
        drainToLocal()

        while (true) {
            val wrapper = localQueue.removeFirstOrNull()
            if (wrapper == null) {
                // 本地队列空 → 尝试停止，失败则 drain 重试
                if (tryStop()) return
                drainToLocal()
                continue
            }
            // 跳过已取消的
            if (wrapper.job?.isActive == false) continue

            try {
                wrapper.task()
            } catch (e: Throwable) {
                handleException(wrapper, e)
            }
            break
        }

        if (localQueue.isNotEmpty() || stackTop.value != null) {
            dispatchNext()
        } else if (!tryStop()) {
            dispatchNext()
        }
    }

    /**
     * 尝试停止处理循环。
     * set(idle) 后 re-check，关闭竞态窗口。
     * @return true=安全停止，false=有新任务需继续
     */
    private fun tryStop(): Boolean {
        running.compareAndSet(1, 0)
        // re-check：关闭后有新任务进来？重新激活
        return !(stackTop.value != null && running.compareAndSet(0, 1))
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
