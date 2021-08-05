package io.github.ksmail13.client

import io.github.ksmail13.buffer.DataBuffer
import io.github.ksmail13.buffer.EmptyDataBuffer
import io.github.ksmail13.exception.ReadTimeoutException
import io.github.ksmail13.logging.Logging
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * [AsynchronousSocketChannel]의 Read 연산 결과를 publish하는 [Publisher]
 *
 * @property option 내부 옵션
 * @see AsynchronousSocketChannel
 * @see AsyncSocketChannelPublisherOption
 */
internal class AsyncSocketChannelReceivePublisher(
    private val option: AsyncSocketChannelPublisherOption
) : Publisher<DataBuffer>, Subscriber<DataBuffer> {

    companion object : Logging

    @Volatile
    private var s: Subscriber<in DataBuffer>? = null
    private val subscriber: AtomicReferenceFieldUpdater<AsyncSocketChannelReceivePublisher,Subscriber<*>>
    = AtomicReferenceFieldUpdater.newUpdater(AsyncSocketChannelReceivePublisher::class.java, Subscriber::class.java, "s")
    private val subscription: AtomicReference<Subscription> = AtomicReference()

    private val remainRequest: AtomicLong = AtomicLong()

    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        if (s == null) return
        setSubscriber(s)
        val socketSubscription = SocketSubscription(this)
        this.onSubscribe(socketSubscription)
        s.onSubscribe(socketSubscription)
    }

    override fun onSubscribe(s: Subscription?) {
        subscription.set(s)
    }

    override fun onNext(t: DataBuffer?) {
        logger.trace("onNext {}", this)
        val sub = getSubscriber()
        sub.onNext(t)
        val remain = remainRequest.updateAndGet { if (it == Long.MAX_VALUE) it else it - 1 }
        if (remain > 0) {
            logger.trace("call downstream {}", remain)
            // 재요청하면서 다시 필요 처리량이 늘지 않도록 0으로 호출
            subscription.get().request(0)
        } else {
            logger.trace("clear downstream {}", remain)
            complete(sub)
        }
    }

    override fun onError(t: Throwable?) {
        getSubscriber().also { clear() }.onError(t)
    }

    override fun onComplete() {
        complete(getSubscriber())
    }

    private fun clear() {
        setSubscriber(null)
        remainRequest.set(0)
    }

    private fun complete(subscriber: Subscriber<DataBuffer>) {
        // complete 전에 미리 초기화 하여 멀티스레드 환경에서 초기화가 꼬이지 않게 처리
        clear()
        subscriber.onComplete()
    }

    private fun setSubscriber(sub: Subscriber<in DataBuffer>?) {
        if (sub == null) {
            if (log.isTraceEnabled) {
                log.trace("clear subscriber")
            }
            subscriber.set(this, sub)
            return
        }

        if (!subscriber.compareAndSet(this, null, sub)) {
            logger.trace("try subscribe but still exist")
            complete(getSubscriber())
            subscriber.set(this, sub)
        }

        if (log.isTraceEnabled) {
            log.trace("set new subscriber {} -> {}, {}", sub, s, subscriber.get(this))
        }
    }

    private fun getSubscriber(): Subscriber<DataBuffer> {
        val get = subscriber.get(this)
        logger.trace("get subscriber {}, {}, {}", this, get, s)

        return requireNotNull(get) { "Subscriber is empty" } as Subscriber<DataBuffer>
    }


    private inner class SocketSubscription(
        private val subscriber: Subscriber<in DataBuffer>
    ) :
        Subscription, CompletionHandler<Int, Pair<ByteBuffer, Subscriber<in DataBuffer>>> {

        private val running: AtomicBoolean = AtomicBoolean(true)

        override fun request(n: Long) {
            val remain = remainRequest.updateAndGet { u ->
                when {
                    n == Long.MAX_VALUE || u == Long.MAX_VALUE -> Long.MAX_VALUE
                    (u + n) < 0 -> Long.MAX_VALUE - 2
                    else -> u + n - 1
                }
            }

            logger.trace("request {}, {} reads remain", n, remain)
            if (remain >= 0) {
                requestRead()
            }
        }

        private fun requestRead() {
            val (socketChannel, _, bufferFactory) = option
            val createBuffer = bufferFactory.createBuffer()

            if (!socketChannel.isOpen) {
                cancel()
                return
            }
            logger.trace("read request")
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
            remainRequest.set(0)
        }


        override fun completed(result: Int?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
            if (attachment == null) return
            val (buf, sub) = attachment

            val readByte = result ?: -1
            if (readByte < 0) {
                logger.trace("close by server")
                sub.onNext(EmptyDataBuffer)
                sub.onComplete()
                option.socketChannel.close()
                return
            }

            logger.trace("read {} bytes", result)
            sub.onNext(EmptyDataBuffer.append(ByteBuffer.wrap(buf.array(), 0, readByte)))
        }

        override fun failed(exc: Throwable?, attachment: Pair<ByteBuffer, Subscriber<in DataBuffer>>?) {
            val sub = attachment?.second
            sub?.onError(ReadTimeoutException(cause = exc))
        }
    }
}