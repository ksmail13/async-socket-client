package io.github.ksmail13.client;

import io.github.ksmail13.server.EchoServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


class AsyncTcpClientTest {

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
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        CompletableFuture<ByteBuffer> read = client.read(addr);
        String test = "test";
        Void j = client.write(addr, ByteBuffer.wrap(test.getBytes())).join();

        int readed = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (readed < test.length()) {
            ByteBuffer join = read.join();
            baos.write(join.array());
            readed += join.position();
        }
        String msg = new String(baos.toByteArray());
        System.out.println("recv: " + msg);
        assert test.equals(msg);
        System.out.println("complete");
        target.off();
    }
}