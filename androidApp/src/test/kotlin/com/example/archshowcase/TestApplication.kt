package com.example.archshowcase

import android.app.Application

class TestApplication : Application() {
    // 测试环境不初始化 Koin，避免重复启动问题
}
