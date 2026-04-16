package com.example.archshowcase

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.archshowcase.presentation.preview.PreviewWrapper
import com.example.archshowcase.presentation.root.DefaultRootComponent
import com.example.archshowcase.presentation.root.RootComponent
import com.example.archshowcase.presentation.root.RootContent

@Composable
fun MainView(root: RootComponent) {
    RootContent(root)
}

@Preview
@Composable
fun MainViewPreview() = PreviewWrapper { componentContext ->
    MainView(DefaultRootComponent(componentContext))
}
