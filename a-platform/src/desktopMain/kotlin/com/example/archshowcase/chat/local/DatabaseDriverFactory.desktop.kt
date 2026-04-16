package com.example.archshowcase.chat.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.archshowcase.chat.db.ChatDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChatDatabase.Schema.create(driver)
        return driver
    }
}
