package io.github.ksmail13.buffer

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Immutable implementation of DataBuffer
 */
internal class ImmutableDataBuffer internal constructor (private val dataList: List<Any> = listOf()) : DataBuffer {

    private fun appendInternal(value: Any) = ImmutableDataBuffer(dataList.toMutableList().apply { add(value) }.toList())

    override fun append(`val`: Int) = appendInternal(`val`)

    override fun append(`val`: Byte) = appendInternal(`val`)

    override fun append(`val`: Char) = appendInternal(`val`)

    override fun append(`val`: Short) = appendInternal(`val`)

    override fun append(`val`: Float) = appendInternal(`val`)

    override fun append(`val`: Double) = appendInternal(`val`)

    override fun append(value: String?) = value?.let { appendInternal(it) } ?: this

    override fun append(value: ByteArray?) = value?.let { appendInternal(it) } ?: this

    override fun append(buffer: ByteBuffer?) = buffer?.let { appendInternal(it.position(0)) } ?: this

    /**
     * make ByteBuffer from data with UTF-8
     *
     * @return new bytebuffer
     */
    override fun toBuffer() = toBuffer(StandardCharsets.UTF_8)

    /**
     * make ByteBuffer from data with charset
     *
     * @return new bytebuffer from utf-8
     */
    override fun toBuffer(charset: Charset): ByteBuffer {
        val size = dataList.map {
            when (it) {
                is Int -> 4
                is Char -> 2
                is Short -> 2
                is Byte -> 1
                is Float -> 4
                is Double -> 8
                is Long -> 8
                is String -> it.length
                is ByteArray -> it.size
                is ByteBuffer -> it.remaining()
                else -> 0
            }
        }.sum()
        val buffer = ByteBuffer.allocate(size)

        dataList.forEach {
            when (it) {
                is Int -> buffer.putInt(it)
                is Char -> buffer.putChar(it)
                is Short -> buffer.putShort(it)
                is Byte -> buffer.put(it)
                is Float -> buffer.putFloat(it)
                is Double -> buffer.putDouble(it)
                is Long -> buffer.putLong(it)
                is String -> buffer.put(it.toByteArray(charset))
                is ByteArray -> buffer.put(it)
                is ByteBuffer -> buffer.put(it)
            }
        }

        return buffer.position(0) as ByteBuffer
    }
}

