package com.example.archshowcase.network.header

expect class HeaderProvider() {
    fun getHeaders(): Map<String, String>
}
