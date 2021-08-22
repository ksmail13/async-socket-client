package io.github.ksmail13.publisher

import io.github.ksmail13.logging.Logging
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class DisposableSubscriber<T> private constructor(
    private val next: Consumer<T>,
    private val error: Consumer<Throwable?>,
    private val complete: Runnable
) : Subscriber<T>, Disposable {

    companion object : Logging {

        @JvmOverloads
        fun <T> Publisher<T>.toDisposable(
            next: Consumer<T> = Consumer { log.trace("onNext {}", it) },
            error: Consumer<Throwable?> = Consumer { log.trace("onError", it) },
            complete: Runnable = Runnable { log.trace("onComplete") }
        ): Disposable =
            DisposableSubscriber(next, error, complete).also { this.subscribe(it) }
    }

    private val subscription = AtomicReference<Subscription>()

    override fun onSubscribe(s: Subscription?) {
        subscription.set(s)
        s?.request(Long.MAX_VALUE)
    }

    override fun onNext(t: T) = next.accept(t)

    override fun onError(t: Throwable?) = error.accept(t)

    override fun onComplete() = complete.run()

    override fun dispose() {
        subscription.get().cancel()
    }
}