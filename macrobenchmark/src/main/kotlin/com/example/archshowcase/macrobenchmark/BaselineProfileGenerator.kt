package com.example.archshowcase.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = "com.example.archshowcase",
        ) {
            pressHome()
            startActivityAndWait()
            // TODO: 添加关键用户路径交互以扩大 profile 覆盖面
        }
    }
}
