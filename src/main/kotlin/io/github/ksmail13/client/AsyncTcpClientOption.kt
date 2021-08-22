package io.github.ksmail13.client

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class AsyncTcpClientOption @JvmOverloads constructor(
    var executorService: ScheduledExecutorService,
    val timeout: Long = 2000L,
    val timeoutUnit: TimeUnit = TimeUnit.MILLISECONDS
) {

    @JvmOverloads
    constructor(thread: Int, timeout: Long = 2000L, timeoutUnit: TimeUnit = TimeUnit.MILLISECONDS) :
            this(Executors.newScheduledThreadPool(thread), timeout, timeoutUnit)

    val asyncGroup: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executorService)
}
