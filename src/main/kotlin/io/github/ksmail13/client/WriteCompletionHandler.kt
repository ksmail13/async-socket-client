package io.github.ksmail13.client

import io.github.ksmail13.publisher.EmptyPublisher
import org.slf4j.Logger
import java.nio.channels.CompletionHandler

internal class WriteCompletionHandler(private val logger: Logger): CompletionHandler<Int, EmptyPublisher> {

    override fun completed(result: Int?, attachment: EmptyPublisher?) {
        logger.debug("write success {} bytes", result)
        attachment?.complete()
    }

    override fun failed(exc: Throwable?, attachment: EmptyPublisher?) {
        logger.error("complete with fail", exc)
        if (exc == null) return
        attachment?.error(exc)
    }
}