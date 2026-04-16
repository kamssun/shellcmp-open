package com.example.archshowcase.core

// 运行时状态，由代码自动设置，勿手动修改
object AppRuntimeState {

    // 预览环境标记，由 PreviewWrapper 设置
    var isInPreview: Boolean = false

    // VF 验证模式：Store 使用预填充状态跳过 Bootstrapper
    var verificationMode: Boolean = false
}
