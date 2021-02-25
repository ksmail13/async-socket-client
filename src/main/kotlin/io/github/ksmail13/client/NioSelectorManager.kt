package io.github.ksmail13.client

import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

internal class NioSelectorManager(id: Int) : Closeable {

    companion object {
        private val log = LoggerFactory.getLogger(NioSelectorManager::class.java)
    }

    private val thread: Thread
    private val selector = Selector.open()
    private val selectRunnable = SelectRunnable(selector)

    init {
        thread = Thread(selectRunnable, "NioSelectorManager-$id")
        thread.isDaemon = true
        thread.start()
    }

    internal fun newSocket(addr: InetSocketAddress): AsyncSocketImplKt {
        log.debug("create socket $addr")
        val socket = SocketChannel.open(addr)
        socket.configureBlocking(false)
        val asyncSocketImplKt = AsyncSocketImplKt(socket)
        socket.register(selector, SelectionKey.OP_READ, asyncSocketImplKt)
        return asyncSocketImplKt
    }


    private class SelectRunnable(val selector: Selector) : Runnable {
        private var running = true

        override fun run() {
            while (running && selector.isOpen) {
                val selectedKeyCnt = selector.selectNow()
                if (!selector.isOpen) break
                val selectedKeys = selector.selectedKeys()

                if (selectedKeyCnt > 0) {
                    log.debug("Selected key {}", selectedKeyCnt)
                    selectedKeys.forEach {
                        if (it.isValid && it.isReadable) {
                            val attachment = it.attachment() as AsyncSocketImplKt
                            attachment.socketRead()
                        }
                    }
                }
            }
        }

        fun off() {
            running = false
        }

    }

    override fun close() {
        selector.close()
        selectRunnable.off()
    }

}