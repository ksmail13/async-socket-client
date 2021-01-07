package io.github.ksmail13.client;

import io.github.ksmail13.logging.LoggingKt;
import io.github.ksmail13.server.EchoServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

    @Test
    void test() throws IOException {
        try {
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
            AsyncSocket connect = client.connect(addr);
            CompletableFuture<ByteBuffer> read = connect.read();
            String test = "test";
            Void j = connect.write(ByteBuffer.wrap(test.getBytes())).join();

            int readed = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (readed < test.length()) {
                ByteBuffer join = read.join();
                baos.write(join.array());
                readed += join.position();
            }
            String msg = new String(baos.toByteArray()).trim();
            logger.info(() -> "recv: " + msg + ", " + msg.length());
            assertThat(msg).isEqualTo(test);
            logger.info("complete");
        } finally {
            target.off();
        }
    }
}