package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.publisher.EmptyPublisher
import io.github.ksmail13.publisher.ReadCompletionHandler
import io.github.ksmail13.publisher.SimplePublisher
import io.github.ksmail13.publisher.WriteCompletionHandler
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors

class AsyncSocketImplKt
@JvmOverloads constructor(
    private val socketOption: AsyncTcpClientOption,
    private val socket: AsynchronousSocketChannel,
    private val bufferFactory: BufferFactory = BufferFactory(1024),
) : AsyncSocket {

    private val readFuture: SimplePublisher<DataBuffer> = SimplePublisher()

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncSocketImplKt::class.java)
        private val closeHandler = Executors.newScheduledThreadPool(1) {
                runnable -> Thread(runnable, "AsyncSocketCloseHandler")}
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
            WriteCompletionHandler(logger))
        return future
    }

    override fun close(): Publisher<Void> {
        val future = EmptyPublisher()
        return try {
            socket.close()
            readFuture.close()
            closeHandler.execute { future.complete() }
            future
        } catch(e: Exception) {
            future.error(e)
            future
        }
    }

    internal fun socketRead() {
        try {
            val buffer = bufferFactory.createBuffer()
            socket.read(buffer,
                socketOption.timeout,
                socketOption.timeoutUnit,
                buffer to readFuture,
                ReadCompletionHandler(logger))
        } catch (e: Throwable) {
            readFuture.error(e)
        }
    }

}