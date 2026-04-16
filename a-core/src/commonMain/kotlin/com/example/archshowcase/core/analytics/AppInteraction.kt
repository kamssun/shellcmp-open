package com.example.archshowcase.core.analytics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.archshowcase.core.analytics.model.GestureType

/**
 * 替代 Modifier.clickable，自动标记 InteractionContext。
 * 业务层禁止裸用 .clickable，统一走 appClickable。
 */
@Composable
fun Modifier.appClickable(
    component: String = "",
    gestureType: GestureType = GestureType.TAP,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    role = role,
    onClick = { withUserGesture(component, gestureType) { onClick() } },
)

/**
 * 替代 Modifier.combinedClickable，onClick / onLongClick / onDoubleClick 自动标记。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.appCombinedClickable(
    component: String = "",
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = null,
    role: Role? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
): Modifier = this.combinedClickable(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    role = role,
    onClick = { withUserGesture(component) { onClick() } },
    onLongClick = onLongClick?.let { { withUserGesture(component, GestureType.LONG_PRESS) { it() } } },
    onDoubleClick = onDoubleClick?.let { { withUserGesture(component, GestureType.DOUBLE_TAP) { it() } } },
)

/**
 * 替代 KeyboardActions，IME 动作自动标记 InteractionContext。
 */
fun appKeyboardActions(
    component: String = "",
    onDone: (() -> Unit)? = null,
    onGo: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onSend: (() -> Unit)? = null,
): KeyboardActions = KeyboardActions(
    onDone = onDone?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
    onGo = onGo?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
    onNext = onNext?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
    onPrevious = onPrevious?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
    onSearch = onSearch?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
    onSend = onSend?.let { { withUserGesture(component, GestureType.IME_ACTION) { it() } } },
)
