package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.emptyBuffer
import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.publisher.EmptyPublisher
import io.github.ksmail13.publisher.SimplePublisher
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socket: SocketChannel,
    private val bufferFactory: BufferFactory = BufferFactory(1024),
    internal val writeQueue: Queue<WriteInfo> = LinkedBlockingQueue()
) : AsyncSocket {
    private val logger = LoggerFactory.getLogger(AsyncSocketImplKt::class.java)
    private val readFuture: SimplePublisher<DataBuffer> = SimplePublisher()

    fun isClose() = !socket.isOpen

    override fun read(): Publisher<DataBuffer> {
        return readFuture
    }

    override fun write(buffer: DataBuffer?): Publisher<Void> {
        if (buffer == null) {
            return EmptyPublisher(true)
        }

        val future = EmptyPublisher()
        writeQueue.add(buffer.toBuffer() to future)
        return future
    }

    override fun close(): Publisher<Void> {
        val future = EmptyPublisher()
        return try {
            socket.close()
            readFuture.close()
            EmptyPublisher()
        } catch(e: Exception) {
            future.error(e)
            future
        }
    }

    internal fun socketRead() {
        try {
            val buffer = bufferFactory.createBuffer()
            val read = socket.read(buffer)
            when {
                read <= 0 -> {
                    logger.debug("Closed by server ({})", read)
                    socket.close()
                    readFuture.close()
                    return
                }
                else -> {
                    logger.debug("Read $read bytes from ${socket.remoteAddress}")
                    readFuture.push(emptyBuffer().append(buffer.limit(read) as ByteBuffer))
                }
            }
        } catch (e: Throwable) {
            readFuture.error(e)
        }
    }

    internal fun socketWrite(): Boolean {
        if (writeQueue.isEmpty()) return true
        if (!socket.isOpen || !socket.isConnected) {
            logger.debug("socket closed")
            return false
        }
        val (byteBuffer, completableFuture) = writeQueue.peek()
        val write = socket.write(byteBuffer)
        logger.trace("write $write bytes to ${socket.remoteAddress}")

        if (!byteBuffer.hasRemaining()) {
            logger.debug("complete write")
            completableFuture.complete()
            writeQueue.poll()
        }

        return true
    }
}