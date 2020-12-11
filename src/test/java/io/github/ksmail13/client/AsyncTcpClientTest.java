package io.github.ksmail13.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private ServerRunnable target;

    @BeforeEach
    public void init() {
        target = new ServerRunnable();
        serverThread = new Thread(target);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    class ServerRunnable implements Runnable {

        private AtomicBoolean run = new AtomicBoolean(true);

        public void off() {
            run.set(false);
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(false);

                serverSocket.bind(new InetSocketAddress("0.0.0.0", 35000), 10);
                while (run.get()) {
                    Socket accept = serverSocket.accept();
                    InputStream inputStream = accept.getInputStream();
                    OutputStream outputStream = accept.getOutputStream();
                    byte[] buf = new byte[1024];
                    int read = inputStream.read(buf);
                    if (read == 0) {
                        accept.close();
                        break;
                    }
                    System.out.println(new String(buf));
                    outputStream.write(buf);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Test
    void test() {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 35000);
        CompletableFuture<ByteBuffer> read = client.read(addr);
        client.write(addr, ByteBuffer.wrap("test".getBytes())).completeAsync(() -> {
            System.out.println("write complete");
            return null;
        });

        ByteBuffer join = read.join();
        String msg = new String(join.array());
        assert "test".equals(msg);
        System.out.println("complete");
        target.off();
    }
}