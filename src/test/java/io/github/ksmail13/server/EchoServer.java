package io.github.ksmail13.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(EchoServer.class);

    private final ServerSocket serverSocket;
    private boolean runnable = true;

    public EchoServer(int port) {
        try {
            serverSocket = new ServerSocket(port, 10);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Socket accept() {
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void run() {
        int cnt = 0;
        while(runnable) {
            Socket accept = accept();
            Thread thread = new Thread(new EchoWorker(accept), "worker-" + cnt++);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void off() {
        runnable = false;
    }

    public static class EchoWorker implements Runnable {

        private final Socket socket;

        public EchoWorker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            byte[] bytes = new byte[1024];
            while (!socket.isClosed()) {
                try {
                    int read = socket.getInputStream().read(bytes);
                    if (read == 0) break;
                    if (read < 0) {
                        socket.close();
                        break;
                    }
                    byte[] readed = new byte[read];
                    System.arraycopy(bytes, 0, readed, 0, read);
                    logger.info("server recv : {} {}", read, new String(readed));
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(readed);
                    outputStream.flush();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
