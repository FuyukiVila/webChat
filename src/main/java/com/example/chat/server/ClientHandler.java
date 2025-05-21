package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理单个客户端连接的处理器
 */
@Slf4j
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private volatile boolean running;

    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // 初始化输入输出流
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            this.input = new ObjectInputStream(clientSocket.getInputStream());

            // 处理登录
            handleLogin();

            // 主消息处理循环
            while (running) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }
        } catch (IOException e) {
            log.error("客户端连接异常: {}", e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error("消息类型转换错误: {}", e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * 处理客户端登录
     */
    private void handleLogin() throws IOException, ClassNotFoundException {
        while (running) {
            Message loginMessage = (Message) input.readObject();
            if (loginMessage.getType() != MessageType.LOGIN_REQUEST) {
                sendMessage(Message.createSystemMessage(
                    MessageType.ERROR_MESSAGE,
                    "请先登录!"
                ));
                continue;
            }

            String requestedUsername = loginMessage.getSender();
            if (server.addUser(requestedUsername, this)) {
                this.username = requestedUsername;
                // 发送登录成功消息，包含当前在线用户列表和可用聊天室列表
                Map<String, Object> loginData = new HashMap<>();
                loginData.put("users", server.getOnlineUserList());
                loginData.put("rooms", server.getChatRoomList());
                
                Message loginSuccess = Message.builder()
                    .type(MessageType.LOGIN_SUCCESS)
                    .content("登录成功！")
                    .data(loginData)
                    .build();
                sendMessage(loginSuccess);
                break;
            } else {
                sendMessage(Message.createSystemMessage(
                    MessageType.LOGIN_FAILURE_USERNAME_TAKEN,
                    "用户名 '" + requestedUsername + "' 已被占用，请选择其他用户名"
                ));
            }
        }
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(Message message) {
        if (!running) return;

        switch (message.getType()) {
            case LOGIN_REQUEST:
                // 如果已经登录，返回错误消息
                if (username != null) {
                    sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "您已经登录，不能重复登录"
                    ));
                }
                break;

            case PRIVATE_MESSAGE_REQUEST:
                server.sendPrivateMessage(message);
                break;

            case USER_LIST_REQUEST:
                List<String> userList = server.getOnlineUserList();
                sendMessage(Message.builder()
                    .type(MessageType.USER_LIST_RESPONSE)
                    .data(userList)
                    .build());
                break;

            case CREATE_ROOM_REQUEST:
                handleCreateRoom(message);
                break;

            case JOIN_ROOM_REQUEST:
                handleJoinRoom(message);
                break;

            case LEAVE_ROOM_REQUEST:
                handleLeaveRoom(message);
                break;

            case ROOM_MESSAGE_REQUEST:
                if (message.getRoomName() == null) {
                    sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "消息缺少聊天室名称"
                    ));
                    return;
                }
                server.handleRoomMessage(message);
                break;

            case LIST_ROOMS_REQUEST:
                List<String> roomList = server.getChatRoomList();
                sendMessage(Message.builder()
                    .type(MessageType.LIST_ROOMS_RESPONSE)
                    .data(roomList)
                    .build());
                break;

            case LOGOUT_REQUEST:
                sendMessage(Message.createSystemMessage(
                    MessageType.LOGOUT_CONFIRMATION,
                    "您已成功登出"
                ));
                close();
                break;

            default:
                log.warn("收到未知类型的消息: {}", message.getType());
                break;
        }
    }

    /**
     * 处理创建聊天室请求
     */
    private void handleCreateRoom(Message message) {
        String roomName = message.getRoomName();
        if (roomName == null || roomName.trim().isEmpty()) {
            sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "聊天室名称不能为空"
            ));
            return;
        }

        if (server.createChatRoom(roomName, username)) {
            // 创建成功后自动加入
            // 创建者在 ChatRoom 构造时已自动加入。
            // 直接发送成功消息，让客户端更新状态。
            sendMessage(Message.builder()
                .type(MessageType.JOIN_ROOM_SUCCESS) // Client uses this to set currentRoom
                .content("聊天室 '" + roomName + "' 创建成功并已自动加入")
                .roomName(roomName)
                .build());
        } else {
            sendMessage(Message.createSystemMessage(
                MessageType.CREATE_ROOM_FAILURE,
                "创建聊天室失败：名称 '" + roomName + "' 已被使用"
            ));
        }
    }

    /**
     * 处理加入聊天室请求
     */
    private void handleJoinRoom(Message message) {
        String roomName = message.getRoomName();
        if (roomName == null || roomName.trim().isEmpty()) {
            sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "聊天室名称不能为空"
            ));
            return;
        }

        if (server.joinChatRoom(username, roomName)) {
            sendMessage(Message.builder()
                .type(MessageType.JOIN_ROOM_SUCCESS)
                .content("成功加入聊天室 '" + roomName + "'")
                .roomName(roomName) // Explicitly set roomName
                .build());
        } else {
            sendMessage(Message.createSystemMessage(
                MessageType.JOIN_ROOM_FAILURE,
                "加入聊天室失败：聊天室 '" + roomName + "' 不存在"
            ));
        }
    }

    /**
     * 处理离开聊天室请求
     */
    private void handleLeaveRoom(Message message) {
        String roomName = message.getRoomName();
        if (roomName == null || roomName.trim().isEmpty()) {
            sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "聊天室名称不能为空"
            ));
            return;
        }

        server.handleUserLeaveRoom(username, roomName);
        sendMessage(Message.createSystemMessage(
            MessageType.LEAVE_ROOM_SUCCESS,
            "已离开聊天室 '" + roomName + "'"
        ));
    }

    /**
     * 发送消息给客户端
     */
    public synchronized void sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
            close();
        }
    }

    /**
     * 关闭客户端连接
     */
    public void close() {
        if (!running) return;
        running = false;

        if (username != null) {
            server.removeUser(username);
        }

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            log.error("关闭客户端连接时发生错误: {}", e.getMessage());
        }
    }
}