package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * [AsynchronousSocketChannel]의 Read 연산 결과를 publish하는 [Publisher]
 *
 * @property option 내부 옵션
 * @see AsynchronousSocketChannel
 * @see AsyncSocketChannelPublisherOption
 */
internal class AsyncSocketChannelReceivePublisher(
    private val option: AsyncSocketChannelPublisherOption
    ): Publisher<DataBuffer> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AsyncSocketChannelReceivePublisher::class.java)
    }

    private val subscriber: AtomicReference<Subscriber<in DataBuffer>> = AtomicReference()

    @Volatile
    private var subscribing: Boolean = false

    @Synchronized
    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        if (s == null) return;
        if (subscribing) s.onError(IllegalStateException("Already subscribing"))
        subscriber.set(s)
        s.onSubscribe(SocketSubscription(s))
        subscribing = true;
    }

    private inner class SocketSubscription(
        private val subscriber: Subscriber<in DataBuffer>,
    ): Subscription {

        private val running: AtomicBoolean = AtomicBoolean(true)

        override fun request(n: Long) {
            if (running.getAndSet(false)) {
                requestRead()
            }
        }

        private fun requestRead() {
            val (socketChannel, _, bufferFactory) = option;
            val createBuffer = bufferFactory.createBuffer()

            if (!socketChannel.isOpen) {
                cancel()
                return
            }
            logger.debug("read request")
            socketChannel.read(createBuffer,
                option.socketOption.timeout,
                option.socketOption.timeoutUnit,
                createBuffer to subscriber,
                ReadCompletionHandler(logger, socketChannel)
            )
        }

        override fun cancel() {
            running.set(false)
            if (option.closeOnCancel) {
                option.socketChannel.close()
            }
            subscriber.onComplete()
        }

    }
}