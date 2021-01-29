package io.github.ksmail13.client

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.*

class AsyncTcpClient {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncTcpClient::class.java.name)
    }

    private val map: MutableMap<InetSocketAddress, AsyncSocketImplKt> = ConcurrentHashMap()
    private val selector: Selector = Selector.open()
    private val selectExecutor: Executor = Executors.newSingleThreadExecutor()
    private val selectRunnable = SelectRunnable(selector)

    init {
        selectExecutor.execute(selectRunnable)
    }

    private fun newSocket(addr: InetSocketAddress): AsyncSocketImplKt {
        log.debug("create socket $addr")
        val socket = SocketChannel.open(addr)
        socket.configureBlocking(false)
        val asyncSocketImplKt = AsyncSocketImplKt(socket)
        socket.register(
            selector,
            SelectionKey.OP_READ or SelectionKey.OP_WRITE or SelectionKey.OP_CONNECT,
            asyncSocketImplKt
        )
        map[addr] = asyncSocketImplKt
        return asyncSocketImplKt
    }

    fun connect(addr: InetSocketAddress): AsyncSocket {
        return map[addr] ?: newSocket(addr)
    }

    fun close() {
        selector.close()
        selectRunnable.off()
    }

    private class SelectRunnable(val selector: Selector) : Runnable {
        private var running = true
        private val writableClient: MutableSet<AsyncSocketImplKt> = CopyOnWriteArraySet()

        override fun run() {
            while (running && selector.isOpen) {
                val selectedKeyCnt = selector.selectNow()
                if (!selector.isOpen) break
                val selectedKeys = selector.selectedKeys()
                log.trace("Selected key {}", selectedKeyCnt)

                for (selectedKey in selectedKeys) {
                    val attachment = selectedKey.attachment() as AsyncSocketImplKt
                    if (selectedKey.isValid && selectedKey.isReadable) {
                        attachment.socketRead()
                    }

                    if (selectedKey.isValid && ((selectedKey.readyOps() and SelectionKey.OP_WRITE) > 0)) {
                        if (selectedKey.isWritable) {
                            writableClient.add(attachment)
                        } else {
                            writableClient.remove(attachment)
                        }
                    }
                }

                writableClient
                    .filter { s -> !s.socketWrite() }
                    .forEach { s -> writableClient.remove(s) }
            }
        }

        fun off() {
            running = false
        }

    }
}