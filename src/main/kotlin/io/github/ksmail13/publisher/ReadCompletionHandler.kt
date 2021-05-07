package io.github.ksmail13.publisher

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.EmptyDataBuffer
import org.slf4j.Logger
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException

class ReadCompletionHandler(private val logger: Logger) :
    CompletionHandler<Int, Pair<ByteBuffer, SimplePublisher<DataBuffer>>> {

    override fun completed(result: Int?, attachment: Pair<ByteBuffer, SimplePublisher<DataBuffer>>?) {
        logger.debug("read {} bytes", result)
        if (attachment == null) return
        val (buf, pub) = attachment
        pub.push(EmptyDataBuffer.append(buf))
    }

    override fun failed(exc: Throwable?, attachment: Pair<ByteBuffer, SimplePublisher<DataBuffer>>?) {
        when (exc) {
            is InterruptedByTimeoutException -> {
                logger.debug("data not found")
            }
            else -> {
                logger.error("read fail", exc)
                val pub = attachment?.second
                pub?.error(exc)
            }
        }
    }

}