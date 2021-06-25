package io.github.ksmail13.client

import io.github.ksmail13.common.BufferFactory
import io.github.ksmail13.common.DefaultBufferFactory
import java.nio.channels.AsynchronousSocketChannel

internal data class AsyncSocketChannelPublisherOption(
    val socketChannel: AsynchronousSocketChannel,
    val socketOption: AsyncTcpClientOption,
    val bufferFactory: BufferFactory = DefaultBufferFactory,
    val closeOnCancel: Boolean = false
)