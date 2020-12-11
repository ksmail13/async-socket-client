package io.github.ksmail13.client

import io.github.ksmail13.common.BufferFactory
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger

class AsyncSocketImplKt
@JvmOverloads constructor(
    internal val socket: SocketChannel,
    private val bufferFactory: BufferFactory = BufferFactory(1024),
    private val writeQueue: Queue<WriteInfo> = LinkedBlockingQueue()
) : AsyncSocket, Runnable {
    private val logger: Logger = Logger.getLogger(AsyncSocketImplKt::class.java.name)
    private val readFuture: CompletableFuture<ByteBuffer> = CompletableFuture()

    override fun read(): CompletableFuture<ByteBuffer> {
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

    override fun close(): CompletableFuture<Void> {
        socket.close()
        return CompletableFuture.completedFuture(null)
    }

    fun socketRead() {
        val buffer = bufferFactory.createBuffer()
        val read = socket.read(buffer)
        logger.fine { "Read $read bytes from ${socket.remoteAddress}" }
        readFuture.obtrudeValue(buffer)
    }

    override fun run() {
        while (socket.isOpen) {
            val (buf, future) = writeQueue.poll()

            val write = socket.write(buf)
            logger.fine { "Write $write bytes to ${socket.remoteAddress}" }
            future.complete(null)
        }
    }
}