package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.lang.IllegalStateException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * 단순한 형태의 publisher
 */
class SimplePublisher<T> : Publisher<T> {

    @Volatile
    private var subscriber: Subscriber<T>? = null
    private val SUBSCRIBER: AtomicReferenceFieldUpdater<SimplePublisher<*>, Subscriber<*>> =
        AtomicReferenceFieldUpdater.newUpdater(SimplePublisher::class.java, Subscriber::class.java, "subscriber")

    private val value: AtomicReference<T> = AtomicReference()
    private val error: AtomicReference<Throwable> = AtomicReference()
    private val complete: AtomicBoolean = AtomicBoolean(false)

    override fun subscribe(s: Subscriber<in T>?) {
        if (SUBSCRIBER.get(this) != null) {
            throw IllegalStateException("Already subscribe")
        }
        SUBSCRIBER.set(this, s)
        s?.onSubscribe(SimpleSubscription)
        if (complete.get()) {
            val v = value.get()
            if (v != null) {
                s?.onNext(v)
            } else {
                s?.onError(error.get())
            }
            s?.onComplete()
        }
    }

    fun push(t: T): Unit =
        when (val s = SUBSCRIBER.get(this)) {
            null -> {
                value.set(t)
            }
            else -> {
                s.onNext(t as Nothing?)
            }
        }

    fun error(t: Throwable?): Unit =
        when (val s = SUBSCRIBER.get(this)) {
            null -> {
                error.set(t)
                complete.set(true)
            }
            else -> {
                s.onError(t)
            }
        }

    fun close(): Unit =
        when (val s = SUBSCRIBER.get(this)) {
            null -> complete.set(true)
            else -> {
                s.onComplete()
            }
        }

    private object SimpleSubscription: Subscription {

        override fun request(n: Long) {

        }

        override fun cancel() {
        }

    }
}