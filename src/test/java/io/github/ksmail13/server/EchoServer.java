package io.github.ksmail13.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer implements Runnable {

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
            try {
                int read = accept.getInputStream().read(bytes);
                System.out.printf("server : %d %s\n", read, new String(bytes));
                OutputStream outputStream = accept.getOutputStream();
                outputStream.write(read);
                outputStream.flush();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void off() {
        runnable = false;
    }
}
