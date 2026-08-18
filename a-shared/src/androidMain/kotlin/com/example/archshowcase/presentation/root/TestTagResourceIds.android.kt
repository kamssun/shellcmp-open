package com.example.archshowcase.presentation.root

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.exposeTestTagsAsResourceIds(): Modifier =
    semantics { testTagsAsResourceId = true }
