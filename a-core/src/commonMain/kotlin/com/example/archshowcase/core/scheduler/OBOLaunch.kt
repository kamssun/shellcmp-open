package com.example.archshowcase.core.scheduler

import com.example.archshowcase.core.AppConfig
import com.example.archshowcase.core.AppRuntimeState
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * OBO 版本的 scope.launch
 *
 * 与 OBOLaunchedEffect 遵循相同思想：
 * 当 [AppConfig.useOBOScheduler] 开启时，通过 OBO 调度器逐个执行，
 * 每个任务后让出主线程，允许用户交互插队；
 * 关闭则走系统默认调度。
 *
 * @param tag 来源标识，用于 OBO 诊断日志
 */
fun CoroutineScope.oboLaunch(
    tag: String,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val context = if (AppConfig.useOBOScheduler && !AppRuntimeState.isInPreview) {
        OBOScheduler.dispatcher + CoroutineName(tag)
    } else EmptyCoroutineContext
    return launch(context, block = block)
}
