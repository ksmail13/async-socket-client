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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AsyncSocketChannelReceivePublisher::class.java)
    }

    private val subscriber: AtomicReference<Subscriber<in DataBuffer>> = AtomicReference()
    private val subscription: AtomicReference<Subscription> = AtomicReference()

    private val request: AtomicLong = AtomicLong()

    private val lock = ReentrantLock()

    override fun subscribe(s: Subscriber<in DataBuffer>?) {
        if (s == null) return
        lock.withLock {
            if (!subscriber.compareAndSet(null, s)) {
                complete()
                subscriber.set(s)
            }
            val socketSubscription = SocketSubscription(this)
            this.onSubscribe(socketSubscription)
            s.onSubscribe(socketSubscription)
        }
    }

    override fun onSubscribe(s: Subscription?) {
        subscription.set(s)
    }

    override fun onNext(t: DataBuffer?) {
        lock.withLock {
            requireNotNull(subscriber.get()) {"subscriber empty"}.onNext(t)
            val remain = request.updateAndGet { if (it == Long.MAX_VALUE) it else it - 1 }
            if (remain > 0) {
                logger.debug("call downstream {}", remain)
                // 재요청하면서 다시 필요 처리량이 늘지 않도록 0으로 호출
                subscription.get().request(0)
            } else {
                logger.debug("clear downstream {}", remain)
                complete()
            }
        }
    }

    override fun onError(t: Throwable?) {
        lock.withLock {
            subscriber.get().onError(t)
            subscriber.set(null)
            request.set(0)
        }
    }

    override fun onComplete() {
        lock.withLock {
            complete()
        }
    }

    private fun complete() {
        subscriber.get().onComplete()
        subscriber.set(null)
        request.set(0)
    }

    private inner class SocketSubscription(private val subscriber: Subscriber<in DataBuffer>) :
        Subscription, CompletionHandler<Int, Pair<ByteBuffer, Subscriber<in DataBuffer>>> {

        private val running: AtomicBoolean = AtomicBoolean(true)

        override fun request(n: Long) {
            val remain = request.updateAndGet { u ->
                when {
                    n == Long.MAX_VALUE || u == Long.MAX_VALUE -> Long.MAX_VALUE
                    (u + n) < 0 -> Long.MAX_VALUE - 2
                    else -> u + n - 1
                }
            }

            logger.debug("request {}, {} reads remain", n, remain)
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
            request.set(0)
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