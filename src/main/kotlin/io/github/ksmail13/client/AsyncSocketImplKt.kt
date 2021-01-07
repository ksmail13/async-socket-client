package io.github.ksmail13.client

import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.logging.initLog
import io.github.ksmail13.publisher.SimplePublisher
import org.reactivestreams.Publisher
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger

class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socket: SocketChannel,
    private val bufferFactory: BufferFactory = BufferFactory(1024),
    internal val writeQueue: Queue<WriteInfo> = LinkedBlockingQueue()
) : AsyncSocket {
    private val logger: Logger = initLog(Logger.getLogger(AsyncSocketImplKt::class.java.name))
    private val readFuture: SimplePublisher<ByteBuffer> = SimplePublisher()

    override fun read(): Publisher<ByteBuffer> {
        return readFuture;
    }

    override fun write(buffer: ByteBuffer?): CompletableFuture<Void> {
        if (buffer == null) {
            return CompletableFuture.completedFuture(null)
        }

        val future = CompletableFuture<Void>()
        writeQueue.add(buffer to future)
        return future
    }

    fun close(): CompletableFuture<Void> {
        socket.close()
        readFuture.close()
        return CompletableFuture.completedFuture(null)
    }

    fun socketRead() {
        try {
            val buffer = bufferFactory.createBuffer()
            val read = socket.read(buffer)
            if (read <= 0) {
                logger.finest { "Closed by server" }
                socket.close()
                readFuture.close()
                return
            }

            logger.finest { "Read $read bytes from ${socket.remoteAddress}" }
            readFuture.push(buffer.limit(read))
        } catch (e: Throwable) {
            readFuture.error(e)
        }
    }

    fun run() {
        while (socket.isOpen) {
            val (buf, future) = writeQueue.poll()

            val write = socket.write(buf)
            logger.fine { "Write $write bytes to ${socket.remoteAddress}" }
            future.complete(null)
        }
    }
}