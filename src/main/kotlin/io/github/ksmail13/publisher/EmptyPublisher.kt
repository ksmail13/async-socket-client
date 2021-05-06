package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Void 값을 반환하는 Publisher
 */
class EmptyPublisher(complete: Boolean = false, error: Throwable? = null) : Publisher<Void> {

    private val subscriber: AtomicReference<Subscriber<in Void>?> = AtomicReference()

    private val complete: AtomicBoolean = AtomicBoolean(complete)
    private val error: AtomicReference<Throwable?> = AtomicReference(error)


    override fun subscribe(s: Subscriber<in Void>?) {
        if (subscriber.get() != null) {
            throw IllegalStateException("EmptyPublisher is not support multi subscribe")
        }

        subscriber.set(requireNotNull(s) { "Subscriber must exist" })

        if (complete.get()) {
            when (val existError: Throwable? = error.get()) {
                null -> s.onComplete()
                else -> s.onError(existError)
            }
        }
    }

    fun complete() {
        subscriber.get()?.onComplete()
        complete.set(true)
    }

    fun error(t: Throwable) {
        subscriber.get()?.onError(t)
        error.set(t)
    }
}