package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.EmptyDataBuffer
import org.reactivestreams.Subscriber
import org.slf4j.Logger
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException

internal class ReadCompletionHandler(private val logger: Logger):
    CompletionHandler<Int, Pair<ByteBuffer, Subscriber<in DataBuffer>>> {

    override fun completed(result: Int?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
        logger.debug("read {} bytes", result)
        if (attachment == null) return
        val (buf, sub) = attachment
        sub.onNext(EmptyDataBuffer.append(buf))
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