package io.github.ksmail13.client

import org.reactivestreams.Subscriber
import org.slf4j.Logger
import java.nio.channels.CompletionHandler

internal class WriteCompletionHandler(
    private val logger: Logger
): CompletionHandler<Int, Subscriber<in Void>> {

    override fun completed(result: Int?, attachment: Subscriber<in Void>?) {
        logger.debug("write success {} bytes", result)
        attachment?.onNext(null)
        attachment?.onComplete()
    }

    override fun failed(exc: Throwable?, attachment: Subscriber<in Void>?) {
        logger.error("complete with fail", exc)
        if (exc == null) return
        attachment?.onError(exc)
    }
}