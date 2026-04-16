package com.example.archshowcase.core.trace.verification

/**
 * NetworkRecorder 的桥接接口
 *
 * a-shared 通过此桥接调用 a-platform 中的 NetworkRecorder，
 * 避免 a-shared 直接依赖 a-platform。
 * 由 a-platform 的 NetworkModule 初始化时注册实现。
 */
object NetworkRecorderBridge {

    private var delegate: Delegate? = null

    fun register(delegate: Delegate) {
        this.delegate = delegate
    }

    fun markStart() {
        delegate?.markStart()
    }

    fun markEnd(): NetworkTape? {
        return delegate?.markEnd()
    }

    interface Delegate {
        fun markStart()
        fun markEnd(): NetworkTape
    }
}
