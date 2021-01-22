package io.github.ksmail13.client;

import io.github.ksmail13.logging.LoggingKt;
import io.github.ksmail13.server.EchoServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;


class AsyncTcpClientTest {
    private static final Logger logger = Logger.getLogger(AsyncTcpClient.class.getName());

    private AsyncTcpClient client = new AsyncTcpClient();

    Thread serverThread;
    private EchoServer target;

    @BeforeEach
    public void init() {
        target = new EchoServer(35000);
        serverThread = new Thread(target);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    public void clear() {
        target.off();
        client.close();
    }

    @Test
    void test() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        AsyncSocket connect = client.connect(addr);
        ByteArraySubscriber byteArraySubscriber = new ByteArraySubscriber();
        Publisher<ByteBuffer> read = connect.read();
        read.subscribe(byteArraySubscriber);
        String test = "test";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            Void j = connect.write(ByteBuffer.wrap(test.getBytes())).join();
            String msg = new String(byteArraySubscriber.getFuture().join()).trim();
            logger.info(() -> "recv Message from: " + msg + ", " + msg.length());
            sb.append(msg);
            assertThat(msg).isEqualTo(test);
        }
        assertThat(sb.toString()).isEqualTo("testtesttesttesttesttesttesttesttesttest");
        logger.info("complete");
    }

    static class ByteArraySubscriber implements Subscriber<ByteBuffer> {

        private final CompletableFuture<byte[]> future = new CompletableFuture<>();

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            future.obtrudeValue(byteBuffer.compact().array());
        }

        @Override
        public void onError(Throwable t) {
            future.completeExceptionally(t);
        }

        @Override
        public void onComplete() {

        }

        public CompletableFuture<byte[]> getFuture() {
            return future;
        }
    }
}