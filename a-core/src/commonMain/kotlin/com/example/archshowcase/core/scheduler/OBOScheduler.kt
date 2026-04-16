package com.example.archshowcase.core.scheduler

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job

/**
 * OBO (One-By-One) Scheduler
 * 独创 by jdp
 * 任务逐个执行，每个任务后让出主线程，
 * 允许用户交互消息自然插队，保持 UI 响应。
 */
expect object OBOScheduler {
    /**
     * 提交任务，FIFO 执行
     *
     * @param tag 来源标识，用于诊断（三方 SDK 类名 / 业务模块名）
     * @param parentJob 关联的协程 Job，用于取消检测
     * @param task 要执行的任务
     */
    fun schedule(tag: String, parentJob: Job?, task: () -> Unit)

    /**
     * 提交任务（无 Job 版本）
     *
     * @param tag 来源标识
     */
    fun post(tag: String, task: () -> Unit)

    /**
     * 提交任务到队首，优先于已有任务执行
     *
     * @param tag 来源标识
     */
    fun postFirst(tag: String, task: () -> Unit)

    /**
     * 获取 OBO Dispatcher
     */
    val dispatcher: CoroutineDispatcher
}