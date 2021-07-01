package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.EmptyDataBuffer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
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
) : Publisher<DataBuffer> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AsyncSocketChannelReceivePublisher::class.java)
    }

    private val subscriber: AtomicReference<Subscriber<in DataBuffer>> = AtomicReference()

    @Volatile
    private var subscribing: Boolean = false

    @Synchronized
    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        if (s == null) return
        if (subscribing) s.onError(IllegalStateException("Already subscribing"))
        val repeatableSubscriber = RepeatableSubscriber()
        subscriber.set(repeatableSubscriber)
        repeatableSubscriber.onSubscribe(SocketSubscription(s))
        repeatableSubscriber.subscribe(s)
        subscribing = true
    }

    private inner class SocketSubscription(private val subscriber: Subscriber<in DataBuffer>) :
        Subscription, CompletionHandler<Int, Pair<ByteBuffer, Subscriber<in DataBuffer>>> {

        private val running: AtomicBoolean = AtomicBoolean(true)

        override fun request(n: Long) {
            requestRead()
        }

        private fun requestRead() {
            val (socketChannel, _, bufferFactory) = option;
            val createBuffer = bufferFactory.createBuffer()

            if (!socketChannel.isOpen) {
                cancel()
                return
            }
            logger.debug("read request")
            socketChannel.read(
                createBuffer,
                option.socketOption.timeout,
                option.socketOption.timeoutUnit,
                createBuffer to subscriber,
                this
            )
        }

        override fun cancel() {
            running.set(false)
            if (option.closeOnCancel) {
                option.socketChannel.close()
            }
            subscriber.onComplete()
        }


        override fun completed(result: Int?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
            if (attachment == null) return
            val (buf, sub) = attachment

            val readByte = result ?: -1
            if (readByte < 0) {
                logger.debug("close by server")
                sub.onNext(EmptyDataBuffer)
                sub.onComplete()
                option.socketChannel.close()
                return
            }

            logger.debug("read {} bytes", result)
            sub.onNext(EmptyDataBuffer.append(ByteBuffer.wrap(buf.array(), 0, readByte)))
        }

        override fun failed(exc: Throwable?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
            when (exc) {
                is InterruptedByTimeoutException -> {
                    logger.debug("data not found")
                }
                else -> {
                    logger.error("read fail", exc)
                    val sub = attachment?.second
                    sub?.onError(exc)
                }
            }
        }


    }
}

class RepeatableSubscriber : Publisher<DataBuffer>, Subscriber<DataBuffer> {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RepeatableSubscriber::class.java)
    }

    private val subscriber: AtomicReference<Subscriber<in DataBuffer>> = AtomicReference()
    private val subscription: AtomicReference<Subscription> = AtomicReference()

    private val request: AtomicLong = AtomicLong()

    override fun onSubscribe(s: Subscription?) {
        subscription.set(s)
    }

    @Synchronized
    override fun onNext(t: DataBuffer?) {
        subscriber.get().onNext(t)
        if (request.getAndDecrement() > 0) {
            subscription.get().request(1)
        } else {
            onComplete()
        }
    }

    override fun onError(t: Throwable?) {
        subscriber.get().onError(t)
        subscriber.set(null)
    }

    override fun onComplete() {
        subscriber.get().onComplete()
        subscriber.set(null)
    }

    //================== publisher =========================//

    @Synchronized
    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        if (subscriber.get() != null) {
            s?.onError(IllegalStateException("Already subscribe"))
            return
        }

        subscriber.set(s)
        s?.onSubscribe(RepeatSubscription())
    }

    inner class RepeatSubscription : Subscription {
        override fun request(n: Long) {
            subscription.get().request(1)
            logger.debug("request {} times read", n)
            request.updateAndGet { u -> -1 + if ((u + n) < 0) Long.MAX_VALUE - 1 else u + n }
        }

        override fun cancel() {
            subscription.get().cancel()
            request.set(0)
        }

    }
}