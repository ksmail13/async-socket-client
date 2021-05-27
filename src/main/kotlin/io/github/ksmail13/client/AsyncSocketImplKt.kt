package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.common.DefaultBufferFactory
import io.github.ksmail13.publisher.EmptyPublisher
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors

internal class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socketOption: AsyncTcpClientOption,
    private val socket: AsynchronousSocketChannel,
    bufferFactory: BufferFactory = DefaultBufferFactory,
) : AsyncSocket {

    private val readFuture: Publisher<DataBuffer> = AsyncSocketChannelReceivePublisher(
        AsyncSocketChannelPublisherOption(
            socketChannel = socket,
            socketOption = socketOption,
            bufferFactory = bufferFactory,
            closeOnCancel = false
        )
    )

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncSocketImplKt::class.java)
        private val closeHandler =
            Executors.newScheduledThreadPool(1) { runnable -> Thread(runnable, "AsyncSocketCloseHandler") }
    }

    val close: Boolean get() = !socket.isOpen

    init {
        if (!socket.isOpen) throw IllegalStateException("Init with closed socket")
    }

    override fun read(): Publisher<DataBuffer> {
        return readFuture
    }

    override fun write(buffer: DataBuffer?): Publisher<Void> {
        if (buffer == null) {
            return EmptyPublisher(true)
        }

        val future = EmptyPublisher()
        val data = buffer.toBuffer()
        logger.debug("try write data {} bytes", data.remaining())
        socket.write(
            data,
            socketOption.timeout,
            socketOption.timeoutUnit,
            future,
            WriteCompletionHandler(logger)
        )
        return future
    }

    override fun close(): Publisher<Void> {
        val future = EmptyPublisher()
        return try {
            socket.close()
            closeHandler.execute { future.complete() }
            future
        } catch (e: Exception) {
            future.error(e)
            future
        }
    }
}