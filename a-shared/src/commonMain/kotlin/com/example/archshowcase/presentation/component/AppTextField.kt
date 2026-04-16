package com.example.archshowcase.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.theme.AppTheme

/** 替代 Material3 OutlinedTextField */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)

    val borderColor = when {
        isError -> AppTheme.colors.error
        isFocused -> AppTheme.colors.primary
        else -> AppTheme.colors.outline
    }

    Column(modifier = modifier) {
        label?.let {
            AppText(
                text = it,
                style = AppTheme.typography.bodySmall,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enabled = enabled,
            textStyle = AppTheme.typography.bodyMedium.copy(color = AppTheme.colors.onSurface),
            cursorBrush = SolidColor(AppTheme.colors.primary),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        AppText(
                            text = placeholder,
                            color = AppTheme.colors.onSurfaceVariant.copy(alpha = 0.6f),
                            style = AppTheme.typography.bodyMedium,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Preview
@Composable
fun AppTextFieldPreview() = AppTheme {
    AppTextField(value = "Hello", onValueChange = {}, label = "Label", placeholder = "Placeholder")
}
