package com.example.archshowcase.core.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RingBufferTest {

    @Test
    fun add_andDrain_returnsAllElements() {
        val buffer = RingBuffer<Int>(10)
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)
        assertEquals(listOf(1, 2, 3), buffer.drain())
    }

    @Test
    fun drain_clearsBuffer() {
        val buffer = RingBuffer<Int>(10)
        buffer.add(1)
        buffer.drain()
        assertTrue(buffer.isEmpty)
        assertEquals(0, buffer.size)
    }

    @Test
    fun overCapacity_dropsOldest() {
        val buffer = RingBuffer<Int>(3)
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)
        buffer.add(4) // 丢弃 1
        assertEquals(3, buffer.size)
        assertEquals(listOf(2, 3, 4), buffer.drain())
    }

    @Test
    fun overCapacity_multipleOverflows() {
        val buffer = RingBuffer<Int>(2)
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)
        buffer.add(4)
        assertEquals(listOf(3, 4), buffer.drain())
    }

    @Test
    fun emptyDrain_returnsEmptyList() {
        val buffer = RingBuffer<Int>(10)
        assertTrue(buffer.drain().isEmpty())
    }

    @Test
    fun capacity1_overwrite_keepsLatest() {
        val buffer = RingBuffer<Int>(1)
        buffer.add(1)
        buffer.add(2)
        assertEquals(1, buffer.size)
        assertEquals(listOf(2), buffer.drain())
    }

    @Test
    fun clear_resetsBuffer() {
        val buffer = RingBuffer<Int>(10)
        buffer.add(1)
        buffer.add(2)
        buffer.clear()
        assertEquals(0, buffer.size)
        assertTrue(buffer.isEmpty)
    }
}
