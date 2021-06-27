package io.github.ksmail13.client

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class AsyncTcpClientOption(
    private val thread: Int = 0,
    private var executorService: ExecutorService? = null,
    internal val timeout: Long = 2000L,
    internal val timeoutUnit: TimeUnit = TimeUnit.MILLISECONDS
    ) {

    init {
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(thread)
        }
    }

    val executor: ExecutorService get() = executorService!!

    internal val asyncGroup: AsynchronousChannelGroup
        get() = AsynchronousChannelGroup.withThreadPool(executorService)
}
