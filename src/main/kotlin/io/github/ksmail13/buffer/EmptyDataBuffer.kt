package io.github.ksmail13.buffer

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * get empty buffer
 */
fun emptyBuffer() = EmptyDataBuffer

/**
 * singleton empty data
 */
object EmptyDataBuffer : DataBuffer {
    private val empty = ImmutableDataBuffer()

    override fun append(`val`: Int): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: Byte): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: Char): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: Short): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: Float): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: Double): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: String?): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(`val`: ByteArray?): DataBuffer {
        return empty.append(`val`)
    }

    override fun append(buffer: ByteBuffer?): DataBuffer {
        return empty.append(buffer)
    }

    override fun toBuffer(): ByteBuffer {
        return empty.toBuffer()
    }

    override fun toBuffer(charset: Charset): ByteBuffer {
        return empty.toBuffer(charset)
    }

}