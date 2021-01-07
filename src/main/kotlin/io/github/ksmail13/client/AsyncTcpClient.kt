package io.github.ksmail13.client

import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.*
import java.util.logging.Logger

class AsyncTcpClient {

    companion object {
        private val log: Logger = Logger.getLogger(AsyncTcpClient::class.java.name)
    }

    private val map: MutableMap<InetSocketAddress, AsyncSocketImplKt> = ConcurrentHashMap()
    private val selector: Selector = Selector.open()

    private val selectExecutor: Executor = Executors.newSingleThreadExecutor()
    private val writeExecutor: Executor = Executors.newWorkStealingPool()

    init {
        selectExecutor.execute(SelectRunnable(selector))
    }

    private fun newSocket(addr: InetSocketAddress): AsyncSocketImplKt {
        log.fine { "create socket $addr" }
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
        return map[addr]?: newSocket(addr)
    }

    private class SelectRunnable(val selector: Selector) : Runnable {
        private var running = true

        override fun run() {
            while(running) {
                val selectedKeyCnt = selector.selectNow()
                val selectedKeys = selector.selectedKeys()
                log.finest { "Selected key $selectedKeyCnt" }

                for (selectedKey in selectedKeys) {
                    val attachment = selectedKey.attachment() as AsyncSocketImplKt
                    val socketChannel = selectedKey.channel() as SocketChannel
                    if (selectedKey.isValid && selectedKey.isReadable) {
                        attachment.socketRead()
                    }
                    if (selectedKey.isValid && selectedKey.isWritable) {
                        val writeQueue = attachment.writeQueue
                        if (writeQueue.isEmpty()) continue
                        val (byteBuffer, completableFuture) = writeQueue.peek()
                        val write = socketChannel.write(byteBuffer)
                        log.finest {"write $write bytes to ${socketChannel.remoteAddress}"}

                        if (!byteBuffer.hasRemaining()) {
                            completableFuture.complete(null)
                            writeQueue.poll()
                        }
                    }
                }

            }
        }

        fun off() {
            running = false
        }

    }
 }