package io.github.ksmail13.publisher

import io.github.ksmail13.logging.Logging
import io.github.ksmail13.publisher.DisposablePublisher.Companion.toDisposable
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.core.Flowable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DisposablePublisherTest : FunSpec({
    test("Disposable Test") {
        val disposablePublisher = Flowable.just(1, 2, 3, 4, 5, 6)
            .toDisposable()

        val single = Flowable.fromPublisher(disposablePublisher)
            .doOnNext {
                if (it > 3) disposablePublisher.dispose()
                log.debug("{}", it)
            }
            .doOnComplete {
                log.debug("complete")
            }
            .toList()


        try {
            single.toFuture().get(500, TimeUnit.MILLISECONDS) // must fail
        } catch (e: TimeoutException) {
            return@test
        }

        fail("test must timeout")
    }

}) {
    companion object : Logging
}
