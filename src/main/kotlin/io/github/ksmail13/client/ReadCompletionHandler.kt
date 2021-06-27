package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.EmptyDataBuffer
import org.reactivestreams.Subscriber
import org.slf4j.Logger
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException

internal class ReadCompletionHandler(private val logger: Logger, private val socket: AsynchronousSocketChannel):
    CompletionHandler<Int, Pair<ByteBuffer, Subscriber<in DataBuffer>>> {

    override fun completed(result: Int?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
        if (attachment == null) return
        val (buf, sub) = attachment

        val readByte = result ?: -1
        if (readByte < 0) {
            logger.debug("close by server")
            sub.onNext(EmptyDataBuffer)
            sub.onComplete()
            socket.close()
            return
        }

        logger.debug("read {} bytes", result)
        sub.onNext(EmptyDataBuffer.append(ByteBuffer.wrap(buf.array(), 0, readByte)))
        sub.onComplete()
    }

    override fun failed(exc: Throwable?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
        when (exc) {
            is InterruptedByTimeoutException -> {
                logger.debug("data not found")
            }
            else -> {
                logger.error("read fail", exc)
                val sub = attachment?.second
                sub?.onError(exc)
            }
        }
    }

}