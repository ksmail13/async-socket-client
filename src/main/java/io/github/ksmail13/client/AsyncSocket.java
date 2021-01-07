package io.github.ksmail13.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface AsyncSocket {
    CompletableFuture<ByteBuffer> read();
    CompletableFuture<Void> write(ByteBuffer buffer);
}
