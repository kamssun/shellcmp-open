package com.example.archshowcase.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Flow 层字段级选择器：只在 [selector] 返回值变化时触发重组。
 *
 * 用法：`val count by component.state.selectAsState { images.size }`
 * - 去重在 Flow 层完成，Compose 侧拿到的 State 只在值真正变化时才更新
 */
@Composable
fun <T, R> StateFlow<T>.selectAsState(selector: T.() -> R): State<R> =
    remember(this) { map { it.selector() }.distinctUntilChanged() }
        .collectAsState(value.selector())

/**
 * 引用比较版本：只在 [selector] 返回的引用变化时触发重组。
 *
 * 适用于非原始数值类型（Boolean、String、data class、集合等），
 * 前提是 Reducer 遵守不可变数据规范（内容没变就不创建新对象）。
 * 原始数值类型（Int/Long/Double 等）装箱后 === 不可靠，应用 [selectAsState]。
 */
@Composable
fun <T, R> StateFlow<T>.selectRefAsState(selector: T.() -> R): State<R> =
    remember(this) { map { it.selector() }.distinctUntilChanged { old, new -> old === new } }
        .collectAsState(value.selector())
