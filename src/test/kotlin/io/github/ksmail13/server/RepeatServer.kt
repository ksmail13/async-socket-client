package io.github.ksmail13.server

import io.github.ksmail13.logging.Logging
import kotlinx.coroutines.Runnable
import java.net.InetSocketAddress
import java.net.ServerSocket

class RepeatServer private constructor(port: Int, private val repeat: Int) : Runnable {
    companion object: Logging {
        fun open(port: Int, repeat: Int = 10): RepeatServer {
            val repeatServer = RepeatServer(port, repeat)
            val thread = Thread(repeatServer)
            thread.isDaemon = true
            thread.start()
            return repeatServer
        }
    }

    private val server: ServerSocket = ServerSocket()

    init {
        server.reuseAddress = false
        server.bind(InetSocketAddress(port), 10)
    }

    override fun run() {
        val accept = server.accept()

        val output = accept.getOutputStream().bufferedWriter()
        val input = accept.getInputStream().bufferedReader()

        val msg = input.readLine()
        for (i in 0 until repeat) {
            if (accept.isClosed) {
                log.debug("socket closed")
                break
            }
            output.append(msg).appendLine()
            output.flush()
        }

        accept.close()
    }

    fun close() = server.close()
}