package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class AsyncSocketChannelSendPublisher(
    private val option: AsyncSocketChannelPublisherOption,
    private val data: DataBuffer
    ): Publisher<Int> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AsyncSocketChannelSendPublisher::class.java)
    }

    private val subscriber = AtomicReference<Subscriber<in Int>>()
    private val done = AtomicBoolean(false)

    @Synchronized
    override fun subscribe(s: Subscriber<in Int>?) {
//        if (subscriber.get() == null) throw IllegalStateException("Already subscribe")

        subscriber.set(s)
        s?.onSubscribe(WriteSubscription())
    }

    private inner class WriteSubscription: Subscription {

        @Synchronized
        override fun request(n: Long) {
            if (done.get()) {
                return
            }
            logger.debug("write buffer")
            done.set(true)
            option.socketChannel.write(data.toBuffer(),
                option.socketOption.timeout,
                option.socketOption.timeoutUnit,
                subscriber.get(),
                WriteCompletionHandler(logger))
        }

        override fun cancel() {
            logger.debug("canceled {}", this)
            if (option.closeOnCancel) {
                option.socketChannel.close()
            }
            done.set(true)
        }

    }
}