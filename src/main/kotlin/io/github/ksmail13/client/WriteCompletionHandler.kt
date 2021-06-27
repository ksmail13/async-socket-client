package io.github.ksmail13.client

import org.reactivestreams.Subscriber
import org.slf4j.Logger
import java.nio.channels.CompletionHandler

internal class WriteCompletionHandler(
    private val logger: Logger
): CompletionHandler<Int, Subscriber<in Int>> {

    override fun completed(result: Int?, attachment: Subscriber<in Int>?) {
        logger.debug("write success {} bytes", result)
        attachment?.onNext(result)
        attachment?.onComplete()
    }

    override fun failed(exc: Throwable?, attachment: Subscriber<in Int>?) {
        logger.error("complete with fail", exc)
        if (exc == null) return
        attachment?.onError(exc)
    }
}