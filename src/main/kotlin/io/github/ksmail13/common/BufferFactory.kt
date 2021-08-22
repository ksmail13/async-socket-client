package io.github.ksmail13.common

import java.nio.ByteBuffer

interface BufferFactory {
    fun createBuffer(): ByteBuffer
}

class SimpleBufferFactory(private val size: Int): BufferFactory {

    override fun createBuffer(): ByteBuffer {
        return ByteBuffer.allocate(size)
    }
}

val DefaultBufferFactory = SimpleBufferFactory(2048)

