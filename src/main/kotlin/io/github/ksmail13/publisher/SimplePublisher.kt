package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * 단순한 형태의 publisher
 */
class SimplePublisher<T> : Publisher<T> {
    var subscriber: Subscriber<in T>? = null

    override fun subscribe(s: Subscriber<in T>?) {
        subscriber = s
        s?.onSubscribe(SimpleSubscription())
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

    inner class SimpleSubscription : Subscription {

        override fun request(n: Long) {

        }

        override fun cancel() {

        }

    }
}