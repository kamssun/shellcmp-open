package com.example.archshowcase.core.analytics

/**
 * 固定容量环形缓冲区。满时丢弃最旧元素。
 *
 * 非线程安全，仅主线程使用。
 */
class RingBuffer<T>(private val capacity: Int) {

    init {
        require(capacity in 1..100_000) { "RingBuffer capacity must be in 1..100_000, was $capacity" }
    }

    private val buffer = arrayOfNulls<Any>(capacity)
    private var head = 0
    private var tail = 0
    private var _size = 0

    val size: Int get() = _size
    val isEmpty: Boolean get() = _size == 0

    fun add(element: T) {
        buffer[tail] = element
        tail = (tail + 1) % capacity
        if (_size == capacity) {
            head = (head + 1) % capacity // 丢弃最旧
        } else {
            _size++
        }
    }

    /** 取出所有元素并清空 */
    @Suppress("UNCHECKED_CAST")
    fun drain(): List<T> {
        if (_size == 0) return emptyList()
        val result = ArrayList<T>(_size)
        repeat(_size) { i ->
            result.add(buffer[(head + i) % capacity] as T)
        }
        clear()
        return result
    }

    fun clear() {
        buffer.fill(null)
        head = 0
        tail = 0
        _size = 0
    }
}
