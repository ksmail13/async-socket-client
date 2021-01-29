package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * 단순한 형태의 publisher
 */
class SimplePublisher<T> : Publisher<T> {

    @Volatile
    private var subscriber: Subscriber<in T>? = null
    private val SUBSCRIBER: AtomicReferenceFieldUpdater<SimplePublisher<*>, Subscriber<*>> =
        AtomicReferenceFieldUpdater.newUpdater(SimplePublisher::class.java, Subscriber::class.java, "subscriber")

    override fun subscribe(s: Subscriber<in T>?) {
        if (SUBSCRIBER.get(this) != null) {
            throw IllegalStateException("")
        }
        SUBSCRIBER.set(this, s)
        s?.onSubscribe(SimpleSubscription)
    }

    fun push(t: T) {
        subscriber?.onNext(t)
    }

    fun error(t: Throwable?) {
        subscriber?.onError(t)
    }

    fun close() {
        subscriber?.onComplete()
    }

    private object SimpleSubscription : Subscription {

        override fun request(n: Long) {

        }

        override fun cancel() {

        }

    }
}