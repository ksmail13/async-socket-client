package io.github.ksmail13.client;

import io.github.ksmail13.buffer.DataBuffer;
import io.github.ksmail13.buffer.EmptyDataBuffer;
import io.github.ksmail13.server.EchoServer;
import io.github.ksmail13.utils.JoinableSubscriber;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;


class AsyncTcpClientTest {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTcpClient.class.getName());
    public static final int CNT = 10;
    public static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        final AtomicInteger idx = new AtomicInteger();

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "executor-" + idx.getAndIncrement());
            thread.setDaemon(true);

            return thread;
        }
    };

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
            JoinableSubscriber<Void> joinableSubscriber = new JoinableSubscriber<>();
            connect.write(EmptyDataBuffer.INSTANCE.append(test.getBytes())).subscribe(joinableSubscriber);
            joinableSubscriber.join();
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
        JoinableSubscriber<Void> subscriber = new JoinableSubscriber<>();
        connect.close().subscribe(subscriber);
        emptyFuture.join();
    }

    @Test
    void testMulti() throws InterruptedException {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        List<Pair<Integer, AsyncSocket>> collect = IntStream.range(0, CNT)
                .mapToObj(i -> new Pair<>(i, client.connect(addr))).collect(Collectors.toList());
        ExecutorService executorService = Executors.newFixedThreadPool(CNT, THREAD_FACTORY);

        List<Future<Boolean>> futures = collect.stream()
                .map(p -> {
                    AsyncSocket connect = p.getSecond();
                    int idx = p.getFirst();
                    return (Callable<Boolean>) () -> {
                        try {
                            test();
                            return true;
                        } catch (Exception e) {
                            logger.error("Fail test", e);
                            return false;
                        }
                    };
                })
                .map(executorService::submit)
                .collect(Collectors.toList());

        assertThat(futures.stream().map(f -> {
            try {
                return f.get(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new IllegalStateException(e);
            }
        })).allMatch(Boolean.TRUE::equals);
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