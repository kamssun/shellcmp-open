package com.example.archshowcase.presentation.component

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * 组件 API 签名验证测试。
 * Compose UI 行为通过 Phase 6 集成编译和运行时验证。
 */
class ComponentApiTest {

    @Test
    fun appButtonModuleExists() {
        // 验证 AppButton 顶层函数可引用（编译级验证）
        assertNotNull(::AppButton)
    }

    @Test
    fun appTextButtonModuleExists() {
        assertNotNull(::AppTextButton)
    }

    @Test
    fun appOutlinedButtonModuleExists() {
        assertNotNull(::AppOutlinedButton)
    }

    @Test
    fun appTextModuleExists() {
        assertNotNull(::AppText)
    }

    @Test
    fun appCardModuleExists() {
        assertNotNull(::AppCard)
    }

    @Test
    fun appCircularProgressModuleExists() {
        assertNotNull(::AppCircularProgress)
    }

    @Test
    fun appLinearProgressModuleExists() {
        assertNotNull(::AppLinearProgress)
    }
}
