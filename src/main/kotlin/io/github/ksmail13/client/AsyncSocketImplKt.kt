package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.common.DefaultBufferFactory
import io.github.ksmail13.publisher.EmptyPublisher
import io.github.ksmail13.publisher.SimplePublisher
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors

internal class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socketOption: AsyncTcpClientOption,
    private val socket: AsynchronousSocketChannel,
    private val bufferFactory: BufferFactory = DefaultBufferFactory,
) : AsyncSocket {

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncSocketImplKt::class.java)
        private val closeHandler =
            Executors.newScheduledThreadPool(1) { runnable -> Thread(runnable, "AsyncSocketCloseHandler") }
    }

    val close: Boolean get() = !socket.isOpen

    init {
        if (!socket.isOpen) throw IllegalStateException("Init with closed socket")
    }

    private val readPublisher = AsyncSocketChannelReceivePublisher(
        AsyncSocketChannelPublisherOption(
            socketChannel = socket,
            socketOption = socketOption,
            bufferFactory = bufferFactory,
            closeOnCancel = false
        )
    )

    override fun read(): Publisher<DataBuffer> {
        return readPublisher
    }

    override fun write(buffer: DataBuffer?): Publisher<Int> {
        if (buffer == null) {
            val simplePublisher = SimplePublisher<Int>()
            simplePublisher.push(0)
            simplePublisher.close()
            return simplePublisher
        }

        val data = buffer.toBuffer()
        logger.debug("try write data {} bytes", data.remaining())

        return AsyncSocketChannelSendPublisher(AsyncSocketChannelPublisherOption(
            socketChannel = socket,
            socketOption = socketOption,
            bufferFactory = bufferFactory,
            closeOnCancel = false
        ), buffer)
    }

    override fun close(): Publisher<Void> {
        val future = EmptyPublisher()
        return try {
            if (!close) {
                socket.close()
            }
            closeHandler.execute { future.complete() }
            future
        } catch (e: Exception) {
            future.error(e)
            future
        }
    }
}