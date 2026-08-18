package com.example.archshowcase.core

// 运行时状态，由代码自动设置，勿手动修改
object AppRuntimeState {

    // 预览环境标记，由 PreviewWrapper 设置
    var isInPreview: Boolean = false

    // Android Studio 预览默认渲染图片；截图测试可显式关闭以稳定基线
    var previewRenderImages: Boolean = true

    // VF 验证模式：Store 使用预填充状态跳过 Bootstrapper
    var verificationMode: Boolean = false

    // VF 录制模式：关闭 mock 自动回复等非确定性后台行为
    var vfRecordingMode: Boolean = false
}
