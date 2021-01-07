package io.github.ksmail13.client;

import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface AsyncSocket {
    Publisher<ByteBuffer> read();
    CompletableFuture<Void> write(ByteBuffer buffer);
}
