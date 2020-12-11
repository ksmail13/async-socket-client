package io.github.ksmail13.common

import java.nio.ByteBuffer

class BufferFactory(private val size: Int) {

    fun createBuffer(): ByteBuffer {
        return ByteBuffer.allocate(size)
    }
}
