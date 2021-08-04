package io.github.ksmail13.client

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.net.SocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class AsyncSocketChannelConnectPublisher(
    private val addr: SocketAddress,
    private val socketOption: AsyncTcpClientOption,
    private val option: AsyncSocketChannelPublisherOption
): Publisher<AsyncSocket> {

    private val subscriber = AtomicReference<Subscriber<in AsyncSocket>>()


    @Synchronized
    override fun subscribe(s: Subscriber<in AsyncSocket>?) {
        subscriber.set(s)
        s?.onSubscribe(ConnectionSubscription(addr, socketOption, option, s))
    }

    internal class ConnectionSubscription(
        private val addr: SocketAddress,
        private val socketOption: AsyncTcpClientOption,
        private val option: AsyncSocketChannelPublisherOption,
        private val subscriber: Subscriber<in AsyncSocket>
    ) : Subscription, CompletionHandler<Void, ConnectionAttachment> {

        private val done = AtomicBoolean(false)

        @Synchronized
        override fun request(n: Long) {
            if (done.get()) return

            val (socketChannel) = option
            socketChannel.connect(addr,
                socketChannel to socketOption,
                this)

            done.set(true)
        }

        override fun cancel() {
            done.set(true)
        }

        override fun completed(p0: Void?, p1: ConnectionAttachment?) {
            if (p1 == null) return

            val (socket, socketOption) = p1
            subscriber.onNext(AsyncSocketImplKt(socketOption, socket))
            subscriber.onComplete()
        }

        override fun failed(p0: Throwable?, p1: ConnectionAttachment?) {
            subscriber.onError(p0)
        }

    }


}

typealias ConnectionAttachment = Pair<AsynchronousSocketChannel, AsyncTcpClientOption>