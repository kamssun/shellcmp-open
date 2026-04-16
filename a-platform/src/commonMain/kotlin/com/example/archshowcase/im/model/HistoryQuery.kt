package com.example.archshowcase.im.model

data class HistoryQuery(
    val roomId: String,
    val beforeTimestamp: Long = 0L,
    val limit: Int = 50,
    val seqIdGte: Long = 0L,
    val seqIdLte: Long = 0L
)
