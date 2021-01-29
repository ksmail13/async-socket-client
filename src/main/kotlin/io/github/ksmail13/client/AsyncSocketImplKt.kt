package io.github.ksmail13.client

import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.publisher.SimplePublisher
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue

class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socket: SocketChannel,
    private val bufferFactory: BufferFactory = BufferFactory(1024),
    internal val writeQueue: Queue<WriteInfo> = LinkedBlockingQueue()
) : AsyncSocket {
    private val logger = LoggerFactory.getLogger(AsyncSocketImplKt::class.java)
    private val readFuture: SimplePublisher<ByteBuffer> = SimplePublisher()

    override fun read(): Publisher<ByteBuffer> {
        return readFuture
    }

    override fun write(buffer: ByteBuffer?): CompletableFuture<Void> {
        if (buffer == null) {
            return CompletableFuture.completedFuture(null)
        }

        val future = CompletableFuture<Void>()
        writeQueue.add(buffer to future)
        return future
    }

    override fun close(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        return try {
            socket.close()
            readFuture.close()
            future.complete(null)
            future
        } catch(e: Exception) {
            future.completeExceptionally(e)
            future
        }
    }

    internal fun socketRead() {
        try {
            val buffer = bufferFactory.createBuffer()
            val read = socket.read(buffer)
            if (read <= 0) {
                logger.debug("Closed by server")
                socket.close()
                readFuture.close()
                return
            }

            logger.debug("Read $read bytes from ${socket.remoteAddress}")
            readFuture.push(buffer.limit(read) as ByteBuffer)
        } catch (e: Throwable) {
            readFuture.error(e)
        }
    }

    internal fun socketWrite(): Boolean {
        if (writeQueue.isEmpty()) return true
        if (!socket.isOpen || !socket.isConnected) {
            logger.info("socket closed")
            return false
        }
        val (byteBuffer, completableFuture) = writeQueue.peek()
        val write = socket.write(byteBuffer)
        logger.info("write $write bytes to ${socket.remoteAddress}")

        if (!byteBuffer.hasRemaining()) {
            completableFuture.complete(null)
            writeQueue.poll()
        }

        return true
    }
}