package io.github.ksmail13.utils

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CompletableFuture

/**
 * join 하여 값을 가져올 수 있는 subscriber<br />
 * <b>not thread safe</b>
 */
class JoinableSubscriber<T> : Subscriber<T> {
    val future: CompletableFuture<T> = CompletableFuture()
    var lastValue: T? = null
    override fun onSubscribe(s: Subscription?) {
        s?.request(Long.MAX_VALUE)
    }

    override fun onNext(t: T) {
        future.obtrudeValue(t)
        lastValue = t
    }

    override fun onError(t: Throwable?) {
        future.obtrudeException(t)
    }

    override fun onComplete() {
        future.complete(lastValue)
    }

    fun join(): T = future.join()
}