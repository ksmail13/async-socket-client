package io.github.ksmail13.client;

import io.github.ksmail13.buffer.DataBuffer;
import io.github.ksmail13.buffer.EmptyDataBuffer;
import io.github.ksmail13.server.EchoServer;
import kotlin.ranges.IntRange;
import kotlin.text.StringsKt;
import org.junit.jupiter.api.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;


class AsyncTcpClientTest {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTcpClient.class.getName());
    public static final int CNT = 2;

    private AsyncTcpClient client;

    static Thread serverThread;
    private static EchoServer target;

    @BeforeAll
    public static void initServer() {
        target = new EchoServer(35000);
        serverThread = new Thread(target);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterAll
    public static void clearServer() {
        target.off();
    }

    @BeforeEach
    public void init() {
        client = new AsyncTcpClient();
    }

    @AfterEach
    public void clear() {
        client.close();
    }

    @Test
    void test() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        AsyncSocket connect = client.connect(addr);
        DataBufferSubscriber dataBufferSubscriber = new DataBufferSubscriber();
        Publisher<DataBuffer> read = connect.read();
        read.subscribe(dataBufferSubscriber);
        String test = "test";
        StringBuilder sb = new StringBuilder();
        int cnt = 10;
        for (int i = 0; i < cnt; i++) {
            Void j = connect.write(EmptyDataBuffer.INSTANCE.append(test.getBytes())).join();
            String msg = new String(dataBufferSubscriber.getFuture().join()).trim();
            logger.info("recv Message from: {}({})", msg, msg.length());
            sb.append(msg);
            assertThat(test).isEqualTo(msg);
        }
        assertThat(sb.toString()).isEqualTo(StringsKt.repeat("test", cnt));
        logger.info("complete");
    }

    @Test
    @Timeout(value = 1)
    void closeTest() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        AsyncSocket connect = client.connect(addr);
        DataBufferSubscriber dataBufferSubscriber = new DataBufferSubscriber();
        Publisher<DataBuffer> read = connect.read();
        CompletableFuture<Void> emptyFuture = new CompletableFuture<>();
        read.subscribe(new Subscriber<DataBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(DataBuffer byteBuffer) {

            }

            @Override
            public void onError(Throwable t) {
                emptyFuture.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                emptyFuture.complete(null);
            }
        });

        connect.close().join();
        emptyFuture.join();
    }

    @Test
    void testMulti() throws InterruptedException {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        CountDownLatch countDownLatch = new CountDownLatch(CNT);
        List<AsyncSocket> collect = IntStream.range(0, CNT)
                .mapToObj(i -> client.connect(addr)).collect(Collectors.toList());

        for (AsyncSocket connect : collect) {
            Thread thread = new Thread(() -> {
                DataBufferSubscriber dataBufferSubscriber = new DataBufferSubscriber();
                Publisher<DataBuffer> read = connect.read();
                read.subscribe(dataBufferSubscriber);
                String test = "test";
                StringBuilder sb = new StringBuilder();
                int cnt = 10;
                for (int i = 0; i < cnt; i++) {

                    Void j = connect.write(EmptyDataBuffer.INSTANCE.append(test)).join();
                    String msg = new String(dataBufferSubscriber.getFuture().join()).trim();
                    logger.info("recv Message from: {}({})", msg, msg.length());
                    sb.append(msg);
                    assertThat(test).isEqualTo(msg);
                }
                assertThat(sb.toString()).isEqualTo(StringsKt.repeat("test", cnt));
                logger.info("complete");
                connect.close().join();
                countDownLatch.countDown();
            });

            thread.setDaemon(true);
            thread.start();
        }

        countDownLatch.await();
    }

    static class DataBufferSubscriber implements Subscriber<DataBuffer> {

        private final CompletableFuture<byte[]> future = new CompletableFuture<>();

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(DataBuffer byteBuffer) {
            ByteBuffer buffer = byteBuffer.toBuffer();
            byte[] array = buffer.compact().array();
            future.obtrudeValue(array);
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