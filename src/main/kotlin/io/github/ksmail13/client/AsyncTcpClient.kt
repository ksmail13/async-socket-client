package io.github.ksmail13.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer
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

    private val queue: Queue<RunnableFuture<InetSocketAddress>> = LinkedBlockingQueue(1024)

    init {
        selectExecutor.execute(SelectRunnable(selector))
    }

    fun read(addr: InetSocketAddress): CompletableFuture<ByteBuffer> {
        if (addr !in map) {
            map[addr] = newSocket(addr)
        }
        return map[addr]!!.read()
    }

    fun write(addr: InetSocketAddress, buf: ByteBuffer): CompletableFuture<Void> {
        if (addr !in map) {
            map[addr] = newSocket(addr)
        }
        val future = CompletableFuture<Void>()
        writeExecutor.execute {
            val socket = map[addr]
            val write = socket?.socket?.write(buf)
            println("Write $write bytes")
            future.complete(null)
        }

        return future
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

        return asyncSocketImplKt
    }

    private class SelectRunnable(val selector: Selector) : Runnable {
        private var running = true

        override fun run() {
            while(running) {
                val selectedKeyCnt = selector.selectNow()
                val selectedKeys = selector.selectedKeys()
                log.fine { "Selected key $selectedKeyCnt" }

                for (selectedKey in selectedKeys) {
                    val attachment = selectedKey.attachment() as AsyncSocketImplKt
                    val socketChannel = selectedKey.channel() as SocketChannel
                    if (selectedKey.isReadable) {
                        attachment.socketRead()
                    }
                    if (selectedKey.isConnectable) {
                        log.fine { "Connect $socketChannel" }
                    }
                }

            }
        }

        fun off() {
            running = false
        }

    }
 }