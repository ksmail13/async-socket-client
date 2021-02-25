package io.github.ksmail13.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(EchoServer.class);

    private ServerSocket serverSocket;
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
        while(runnable) {
            Socket accept = accept();
            byte[] bytes = new byte[1024];
            while (!accept.isClosed()) {
                try {
                    int read = accept.getInputStream().read(bytes);
                    if (read == 0) break;
                    if (read < 0) {
                        accept.close();
                        break;
                    }
                    byte[] readed = new byte[read];
                    System.arraycopy(bytes, 0, readed, 0, read);
                    logger.info("server recv : {} {}", read, new String(readed));
                    OutputStream outputStream = accept.getOutputStream();
                    outputStream.write(readed);
                    outputStream.flush();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public void off() {
        runnable = false;
    }
}
