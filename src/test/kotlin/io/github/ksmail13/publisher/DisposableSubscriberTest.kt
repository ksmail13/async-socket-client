package io.github.ksmail13.publisher

import io.github.ksmail13.logging.Logging
import io.github.ksmail13.publisher.DisposableSubscriber.Companion.toDisposable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.core.Flowable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DisposableSubscriberTest : FunSpec({
    test("Disposable Test") {
        val canceled = AtomicBoolean()
        val single = Flowable.just(1, 2, 3, 4, 5, 6)
            .delay(100, TimeUnit.MILLISECONDS)
            .doOnComplete {
                log.debug("complete")
            }
            .doOnCancel { canceled.set(true) }
            .toDisposable()

        single.dispose()
        canceled.get() shouldBe true
    }

}) {
    companion object: Logging
}
