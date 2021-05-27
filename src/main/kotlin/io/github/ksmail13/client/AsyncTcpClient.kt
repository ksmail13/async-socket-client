package io.github.ksmail13.client

import io.github.ksmail13.publisher.SimplePublisher
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.net.SocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentHashMap

class AsyncTcpClient(private val option: AsyncTcpClientOption = AsyncTcpClientOption(2)) {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncTcpClient::class.java.name)
    }

    private val socketMap: MutableMap<SocketAddress, AsyncSocketImplKt> = ConcurrentHashMap()

    fun connect(addr: SocketAddress): Publisher<AsyncSocket> {
        val asocket = AsynchronousSocketChannel.open(option.asyncGroup)
        val pub = SimplePublisher<AsyncSocket>()
        asocket.connect(addr, pub to asocket, ConnectionCallback(option, socketMap, addr))
        return pub
    }

    fun close() {
        socketMap.forEach { (_, v) -> v.close() }
        socketMap.clear()
    }

    private class ConnectionCallback(
        val option: AsyncTcpClientOption,
        val socketMap: MutableMap<SocketAddress, AsyncSocketImplKt>,
        val addr: SocketAddress
    ) : CompletionHandler<Void, Pair<SimplePublisher<AsyncSocket>, AsynchronousSocketChannel>> {

        override fun completed(
            result: Void?,
            attachment: Pair<SimplePublisher<AsyncSocket>, AsynchronousSocketChannel>?
        ) {
            log.debug("connect with {}", addr)
            if (attachment == null) return
            val (pub, asocket) = attachment
            val socket = AsyncSocketImplKt(option, asocket)
            socketMap[addr] = socket
            pub.push(socket)
            pub.close()
        }

        override fun failed(
            exc: Throwable?,
            attachment: Pair<SimplePublisher<AsyncSocket>, AsynchronousSocketChannel>?
        ) {
            if (attachment == null) return
            val (pub, _) = attachment
            pub.error(exc)
        }
    }
}