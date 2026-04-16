package com.example.archshowcase.chat.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.archshowcase.chat.db.ChatDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(ChatDatabase.Schema, "chat.db")
}
