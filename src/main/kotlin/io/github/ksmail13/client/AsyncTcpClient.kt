package io.github.ksmail13.client

import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.net.SocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap

class AsyncTcpClient(private val option: AsyncTcpClientOption = AsyncTcpClientOption(2)) {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncTcpClient::class.java.name)
    }

    private val socketMap: MutableMap<SocketAddress, AsyncSocketImplKt> = ConcurrentHashMap()

    fun connect(addr: SocketAddress): Publisher<AsyncSocket> {
        val asocket = AsynchronousSocketChannel.open(option.asyncGroup)
        return AsyncSocketChannelConnectPublisher(addr,
            option,
            AsyncSocketChannelPublisherOption(asocket, option))
    }

    fun close() {
        socketMap.forEach { (_, v) -> v.close() }
        socketMap.clear()
    }

}