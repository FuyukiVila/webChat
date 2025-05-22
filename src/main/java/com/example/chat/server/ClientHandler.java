package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 处理单个客户端连接的处理器
 */
@Slf4j
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ServerState serverState;
    private final ServerMessageProcessor messageProcessor;
    private final ReentrantLock sendLock = new ReentrantLock();

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ClientHandler(Socket clientSocket, ServerState serverState, ServerMessageProcessor messageProcessor) {
        this.clientSocket = clientSocket;
        this.serverState = serverState;
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void run() {
        try {
            if (initializeStreams()) {
                if (handleLogin()) {
                    processMessages();
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log.error("客户端连接异常: {}", e.getMessage());
            }
        } finally {
            close();
        }
    }

    /**
     * 初始化输入输出流
     */
    private boolean initializeStreams() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
            running.set(true);
            return true;
        } catch (IOException e) {
            log.error("初始化流失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 处理客户端登录
     */
    private boolean handleLogin() throws IOException, ClassNotFoundException {
        while (running.get()) {
            Message loginMessage = (Message) input.readObject();

            if (loginMessage.getType() != MessageType.LOGIN_REQUEST) {
                sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "请先登录!"));
                continue;
            }

            String requestedUsername = loginMessage.getSender();

            if (!isValidName(requestedUsername)) {
                sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "用户名只能包含大小写字母、数字和下划线"));
                continue;
            }

            if (serverState.addUser(requestedUsername, this)) {
                username = requestedUsername;

                // 发送登录成功消息，包含当前在线用户列表和可用聊天室列表
                Map<String, Object> loginData = new HashMap<>();
                loginData.put("users", serverState.getOnlineUserList());
                loginData.put("rooms", serverState.getChatRoomList());

                sendMessage(Message.builder()
                        .type(MessageType.LOGIN_SUCCESS)
                        .content("登录成功！")
                        .data(loginData)
                        .sender("SERVER")
                        .build());

                return true;
            } else {
                sendMessage(Message.createSystemMessage(
                        MessageType.LOGIN_FAILURE_USERNAME_TAKEN,
                        "用户名 '" + requestedUsername + "' 已被占用，请选择其他用户名"));
            }
        }
        return false;
    }

    /**
     * 处理消息循环
     */
    private void processMessages() {
        try {
            while (running.get()) {
                Message message = (Message) input.readObject();
                messageProcessor.processMessage(message, this);
            }
        } catch (EOFException | SocketException e) {
            if (running.get()) {
                // 客户端正常断开连接，不需要记录错误
                log.debug("客户端断开连接: {}", clientSocket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            if (running.get() && e.getMessage() != null) {
                log.error("接收消息失败: {}", e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            if (running.get()) {
                log.error("消息类型转换错误: {}", e.getMessage());
            }
        }
    }

    /**
     * 发送消息给客户端
     * 使用ReentrantLock确保消息发送的原子性和顺序性
     */
    public void sendMessage(Message message) {
        sendLock.lock();
        try {
            if (running.get() && output != null) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            if (running.get() && e.getMessage() != null) {
                log.error("发送消息失败: {}", e.getMessage());
            }
            close();
        } finally {
            sendLock.unlock();
        }
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        if (running.compareAndSet(true, false)) {
            if (username != null) {
                serverState.removeUser(username);
            }

            try {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException ignored) {
                        // 忽略关闭流时的异常
                    }
                    input = null;
                }

                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException ignored) {
                        // 忽略关闭流时的异常
                    }
                    output = null;
                }

                if (!clientSocket.isClosed()) {
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {
                        // 忽略关闭套接字时的异常
                    }
                }
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    log.error("关闭客户端连接时发生错误: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 验证名称格式（用户名或房间名）
     * 只允许使用大小写字母、数字和下划线
     */
    private boolean isValidName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }
}