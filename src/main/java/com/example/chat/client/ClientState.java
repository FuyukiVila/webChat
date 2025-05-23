package com.example.chat.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端状态类，封装所有可变状态
 */
@RequiredArgsConstructor
@Data
public class ClientState {
    private final String host;

    private final int port;

    private final Scanner scanner = new Scanner(System.in);

    private Socket socket;

    private ObjectInputStream input;

    private ObjectOutputStream output;

    private volatile String username;

    private volatile Optional<String> currentRoom = Optional.empty();

    private final AtomicBoolean running = new AtomicBoolean(false);

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean value) {
        running.set(value);
    }

    public void close() {
        setRunning(false);
        try {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
            if (socket != null)
                socket.close();
            scanner.close();
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }
}