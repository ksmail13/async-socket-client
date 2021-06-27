package io.github.ksmail13.client;

import io.github.ksmail13.buffer.EmptyDataBuffer;
import io.github.ksmail13.server.EchoServer;
import io.github.ksmail13.utils.JoinableSubscriber;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
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

        List<String> compares = Single.fromPublisher(client.connect(addr))
                .flatMap(socket ->
                        Flowable.range(0, 1)
                                .flatMap((i) -> socket.write(EmptyDataBuffer.INSTANCE.append("test")))
                                .flatMap((v) -> socket.read())
                                .map(buf -> new String(buf.toBuffer().array()))
                                .repeat(10)
                                .toList())
                .blockingGet();

        assertThat(compares).containsOnly("test");
    }

    @Test
    @Timeout(value = 1)
    void closeTest() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        AsyncSocket connect = Single.fromPublisher(client.connect(addr)).blockingGet();
        Single.fromPublisher(connect.write(EmptyDataBuffer.INSTANCE.append("quit"))).blockingGet();
        Observable.fromPublisher(connect.read()).blockingSubscribe();
        JoinableSubscriber<Void> s = new JoinableSubscriber<>();
        connect.close().subscribe(s);
        s.join();
    }

    @Test
    void testMulti() throws InterruptedException {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        List<Pair<Integer, AsyncSocket>> collect = Flowable.fromStream(IntStream.range(0, CNT).boxed())
                .flatMap(i -> Single.fromPublisher(client.connect(addr)).map(socket -> new Pair<>(i, socket)).toFlowable())
                .collect(Collectors.toList()).blockingGet();
        ExecutorService executorService = Executors.newFixedThreadPool(CNT, THREAD_FACTORY);

        List<Boolean> futures = collect.parallelStream()
                .map(p -> {
                    AsyncSocket connect = p.getSecond();
                    int idx = p.getFirst();
                    String target = "test" + idx;
                    try {
                        List<String> strings = Flowable.just(0)
                                .flatMap(i -> connect.write(EmptyDataBuffer.INSTANCE.append(target)))
                                .flatMap(i -> connect.read())
                                .map(r -> new String(r.toBuffer().array()))
                                .repeat(10)
                                .toList()
                                .blockingGet();
                        strings.forEach(s -> assertThat(s).isEqualTo(target));
                        return true;
                    } catch (Exception e) {
                        logger.error("Fail test", e);
                        return false;
                    }
                })
                .collect(Collectors.toList());

        assertThat(futures).allMatch(Boolean.TRUE::equals);
    }

}