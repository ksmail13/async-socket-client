package io.github.ksmail13.client;

import io.github.ksmail13.common.BufferFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncTcpClient implements AutoCloseable {
    private final Selector selector;
    private final Map<InetSocketAddress, AsyncSocket> socketMap;
    private final BufferFactory bufferFactory = new BufferFactory(1024);
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ExecutorService selectorThread = Executors.newSingleThreadExecutor();

    public AsyncTcpClient() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        socketMap = new ConcurrentHashMap<>();
        selectorThread.execute(new SelectorRunnable());
    }

    @Override
    public void close() throws Exception {
        selector.close();
        socketMap.forEach((k, v) -> v.close());
    }

    public CompletableFuture<ByteBuffer> read(InetSocketAddress address) {
        AsyncSocket socket = getSocket(address);
        return socket.read();
    }

    private AsyncSocket getSocket(InetSocketAddress address) {
        if (!socketMap.containsKey(address)) {
            try {
                socketMap.put(address, createSocket(address));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return socketMap.get(address);
    }

    @NotNull
    private AsyncSocket createSocket(InetSocketAddress address) throws IOException {
        SocketChannel socket = selector.provider().openSocketChannel();
        AsyncSocketImplKt asyncSocket = new AsyncSocketImplKt(socket, bufferFactory);
        socket.configureBlocking(false);
        socket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, asyncSocket);
        socket.connect(address);

        return asyncSocket;
    }

    public CompletableFuture<Void> write(InetSocketAddress address, ByteBuffer buffer) {
        return getSocket(address).write(buffer);
    }

    private class SelectorRunnable implements Runnable {

        @Override
        public void run() {
            try {
                selector.selectNow(key -> {
                    AsyncSocketImplKt attachment = (AsyncSocketImplKt) key.attachment();
                    if (key.isWritable()) {
                        attachment.run();
                    }
                    if (key.isReadable()) {
                        attachment.socketRead();
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
