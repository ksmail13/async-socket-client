package io.github.ksmail13.publisher

import org.reactivestreams.Processor
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicReference


class DisposablePublisher<T>: Disposable, Processor<T, T> {

    companion object {
        fun <T> Publisher<T>.toDisposable(): DisposablePublisher<T> {
            val disposablePublisher = DisposablePublisher<T>()
            this.subscribe(disposablePublisher)
            return disposablePublisher
        }
    }

    private val subscription = AtomicReference<Subscription>()
    private val subscriber = AtomicReference<Subscriber<in T>>()

    override fun dispose() {
        subscription.get()?.cancel()
    }

    override fun subscribe(s: Subscriber<in T>?) {
        val downstream = requireNotNull(s)
        subscriber.set(downstream)
        downstream.onSubscribe(BypassSubscription())
    }

    override fun onSubscribe(s: Subscription?) {
        subscription.set(requireNotNull(s))
    }

    override fun onNext(t: T) {
        subscriber.get()?.onNext(t)
    }

    override fun onError(t: Throwable?) {
        subscriber.get()?.onError(t)
    }

    override fun onComplete() {
        subscriber.get()?.onComplete()
    }

    private inner class BypassSubscription: Subscription {
        override fun request(n: Long) {
            subscription.get()?.request(n)
        }

        override fun cancel() {
            subscription.get()?.cancel()
        }

    }
}