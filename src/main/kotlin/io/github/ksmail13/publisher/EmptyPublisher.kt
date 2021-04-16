package io.github.ksmail13.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

class EmptyPublisher : Publisher<Void> {

    private val subscriber: AtomicReference<Subscriber<in Void>?> = AtomicReference()

    private val complete: AtomicBoolean
    private val error: AtomicReference<Throwable?>

    constructor(complete: Boolean = false, error: Throwable? = null) {
        this.complete = AtomicBoolean(complete)
        this.error = AtomicReference(error)
    }


    override fun subscribe(s: Subscriber<in Void>?) {
        if (subscriber.get() != null) {
            throw IllegalStateException("EmptyPublisher is not support multi subscribe")
        }

        subscriber.set(requireNotNull(s))

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