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
            includeInStartupProfile = true,
            filterPredicate = ::isReleaseStartupProfileRule,
        ) {
            RealLoginPreparer.prepare(this)
            killProcess()
            pressHome()
            startActivityAndWait()
            RealLoginPreparer.requireMainScreen()
        }
    }
}

private fun isReleaseStartupProfileRule(rule: String): Boolean {
    val loginOnlyRules = listOf(
        "com/example/archshowcase/presentation/login",
    )
    return loginOnlyRules.none { rule.contains(it) }
}
