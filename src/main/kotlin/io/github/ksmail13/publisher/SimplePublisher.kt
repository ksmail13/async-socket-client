package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 단순한 형태의 publisher
 */
class SimplePublisher<T> : Publisher<T> {

    private val subscriber: AtomicReference<Subscriber<in T>> = AtomicReference()
    private val value: AtomicReference<T> = AtomicReference()
    private val error: AtomicReference<Throwable> = AtomicReference()
    private val complete: AtomicBoolean = AtomicBoolean(false)

    @Synchronized
    override fun subscribe(s: Subscriber<in T>?) {
        if (subscriber.get() != null) {
            throw IllegalStateException("Already subscribe")
        }
        if (s == null) {
            return;
        }

        subscriber.set(s)
        s.onSubscribe(SimpleSubscription)
        if (complete.get()) {
            val v = value.get()
            if (v != null) {
                s.onNext(v)
            } else {
                s.onError(error.get())
            }
            s.onComplete()
        }
    }

    @Synchronized
    fun push(t: T): Unit =
        when (val s = subscriber.get()) {
            null -> {
                value.set(t)
            }
            else -> {
                s.onNext(t as Nothing?)
            }
        }

    @Synchronized
    fun error(t: Throwable?): Unit =
        when (val s = subscriber.get()) {
            null -> {
                error.set(t)
                complete.set(true)
            }
            else -> {
                s.onError(t)
            }
        }

    fun close(): Unit =
        when (val s = subscriber.get()) {
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