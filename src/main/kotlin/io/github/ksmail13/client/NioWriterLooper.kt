package io.github.ksmail13.client

import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

class NioWriterLooper(id: Int): Closeable {

    private val writeSockets : MutableSet<AsyncSocketImplKt> = CopyOnWriteArraySet()
    private val loop = WriterLoop(writeSockets)

    init {
        val thread = Thread(loop, "NioWriterLooper-$id")
        thread.isDaemon = true
        thread.start()
    }

    fun add(socket: AsyncSocketImplKt) = writeSockets.add(socket)

    private class WriterLoop(private val writeSockets : MutableSet<AsyncSocketImplKt>) : Runnable {
        private var run = true

        override fun run() {
            while (run) {
                for(socket in writeSockets) {
                    when {
                        socket.isClose() -> writeSockets.remove(socket)
                        else -> socket.socketWrite()
                    }
                }
            }
        }

        fun off() {
            run = false
        }
    }

    override fun close() {
        loop.off()
    }

}