package io.github.ksmail13.client;

import io.github.ksmail13.buffer.DataBuffer;
import org.reactivestreams.Publisher;

public interface AsyncSocket {
    Publisher<DataBuffer> read();
    Publisher<Integer> write(DataBuffer buffer);
    Publisher<Void> close();
}
