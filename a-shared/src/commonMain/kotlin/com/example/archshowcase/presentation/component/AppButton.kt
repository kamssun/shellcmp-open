package com.example.archshowcase.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.core.analytics.appClickable
import com.example.archshowcase.presentation.theme.AppTheme

/** 填充式按钮，按压变暗效果 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colors.primary,
    contentColor: Color = AppTheme.colors.onPrimary,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) containerColor.copy(alpha = 0.8f) else containerColor
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(if (enabled) Modifier.appClickable(
                component = "AppButton",
                interactionSource = interactionSource,
                onClick = onClick,
            ) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .drawBehind { drawRect(bgColor) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** 文字按钮，无背景 */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val contentColor = AppTheme.colors.primary
    val displayColor = if (isPressed) contentColor.copy(alpha = 0.7f) else contentColor

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .defaultMinSize(minHeight = 40.dp)
            .then(if (enabled) Modifier.appClickable(
                component = "AppTextButton",
                interactionSource = interactionSource,
                onClick = onClick,
            ) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides displayColor) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** 描边按钮 */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val outlineColor = AppTheme.colors.outline
    val bgAlpha = if (isPressed) 0.08f else 0f
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .defaultMinSize(minHeight = 48.dp)
            .border(width = 1.dp, color = outlineColor, shape = shape)
            .clip(shape)
            .then(if (enabled) Modifier.appClickable(
                component = "AppOutlinedButton",
                interactionSource = interactionSource,
                onClick = onClick,
            ) else Modifier)
            .alpha(if (enabled) 1f else 0.38f)
            .background(outlineColor.copy(alpha = bgAlpha))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides AppTheme.colors.primary) {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** 图标按钮（最小触摸区域 48dp） */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .then(if (enabled) Modifier.appClickable(
                component = "AppIconButton",
                interactionSource = interactionSource,
                onClick = onClick,
            ) else Modifier)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview
@Composable
fun AppButtonPreview() = AppTheme {
    AppButton(onClick = {}) { AppText("Button") }
}

@Preview
@Composable
fun AppTextButtonPreview() = AppTheme {
    AppTextButton(onClick = {}) { AppText("Text Button") }
}

@Preview
@Composable
fun AppOutlinedButtonPreview() = AppTheme {
    AppOutlinedButton(onClick = {}) { AppText("Outlined") }
}
