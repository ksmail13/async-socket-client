package io.github.ksmail13.client;

import io.github.ksmail13.buffer.DataBuffer;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface AsyncSocket {
    Publisher<DataBuffer> read();
    CompletableFuture<Void> write(DataBuffer buffer);
    CompletableFuture<Void> close();
}
