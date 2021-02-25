package io.github.ksmail13.client

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class AsyncTcpClient(private val option : AsyncTcpClientOption = AsyncTcpClientOption(2)) {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncTcpClient::class.java.name)
    }

    private val selectors = (0..option.ioThread).map { NioSelectorManager(it) }
    private val writers = (0..option.ioThread).map { NioWriterLooper(it) }

    private val curr = AtomicInteger()


    fun connect(addr: InetSocketAddress): AsyncSocket {
        val idx = curr.getAndUpdate { (it + 1) / option.ioThread }
        val socket = selectors[idx].newSocket(addr)
        writers[idx].add(socket)
        return socket
    }

    fun close() {
        selectors.forEach { it.close() }
    }

}