package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

private class CountFilteredSubscription(val subscription: Subscription, val filter: Function1<Long, Long>): Subscription {
    override fun request(n: Long) {
        subscription.request(filter(n))
    }

    override fun cancel() {
        subscription.cancel()
    }

}

private class CountFilteredSubscriber(private val filter: Function1<Long, Long>): Publisher<DataBuffer>, Subscriber<DataBuffer> {

    private val subscriber = AtomicReference<Subscriber<in DataBuffer>>()
    private val subscription = AtomicReference<Subscription>()

    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        requireNotNull(s) {"Empty subscriber"}
        if (!subscriber.compareAndSet(null, s)) {
            s.onError(IllegalStateException("Already subscribed"))
            return
        }
        s.onSubscribe(CountFilteredSubscription(subscription.get(), filter))
    }

    override fun onSubscribe(s: Subscription?) {
        subscription.set(s)
    }

    override fun onNext(t: DataBuffer?) {
        subscriber.get()?.onNext(t)
    }

    override fun onError(t: Throwable?) {
        subscriber.get()?.onError(t)
    }

    override fun onComplete() {
        subscriber.get()?.onComplete()
    }
}

private val logger = LoggerFactory.getLogger("io.github.ksmail13.client.ReadHelper")

fun Publisher<DataBuffer>.once(): Publisher<DataBuffer> =
    CountFilteredSubscriber { 1 }.also {
        logger.debug("set subscribe {} -> {}", it, this)
        this.subscribe(it)
    }

fun Publisher<DataBuffer>.infinite(): Publisher<DataBuffer> =
    CountFilteredSubscriber { Long.MAX_VALUE }.also { this.subscribe(it) }