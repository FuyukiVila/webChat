package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天服务器主类
 */
@Slf4j
public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    private final int port;

    // 在线用户列表，key是用户名，value是对应的处理器
    private final ConcurrentHashMap<String, ClientHandler> onlineUsers;
    // 聊天室列表，key是房间名称，value是聊天室对象
    private final ConcurrentHashMap<String, ChatRoom> chatRooms;
    // 线程池用于处理客户端连接
    private final ExecutorService executorService;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public ChatServer(int port) {
        this.port = port;
        this.onlineUsers = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        // 创建一个可根据需要创建新线程的线程池
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    /**
     * 启动服务器
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("聊天服务器启动成功，正在监听端口: " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("新的客户端连接：" + clientSocket.getRemoteSocketAddress());

                    // 为新客户端创建一个处理器并在线程池中执行
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    executorService.execute(clientHandler);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("接受客户端连接时发生错误: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭服务器套接字时发生错误: " + e.getMessage());
        }

        // 通知所有客户端服务器关闭
        broadcastSystemMessage(Message.createSystemMessage(
                MessageType.SERVER_SHUTDOWN_NOTIFICATION,
                "服务器即将关闭..."));

        // 关闭所有客户端连接
        onlineUsers.values().forEach(handler -> handler.close());
        onlineUsers.clear();

        // 清理所有聊天室
        chatRooms.clear();

        // 关闭线程池
        executorService.shutdown();
        System.out.println("服务器已关闭");
    }

    /**
     * 添加新的在线用户
     */
    public boolean addUser(String username, ClientHandler handler) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }

        onlineUsers.put(username, handler);
        // 广播新用户加入的消息
        broadcastSystemMessage(Message.createSystemMessage(
                MessageType.USER_JOINED_NOTIFICATION,
                "用户 " + username + " 已上线"));
        return true;
    }

    /**
     * 移除在线用户
     */
    public void removeUser(String username) {
        ClientHandler handler = onlineUsers.remove(username);
        if (handler != null) {
            // 处理用户在所有聊天室中的退出
            for (ChatRoom room : chatRooms.values()) {
                if (room.hasMember(username)) {
                    handleUserLeaveRoom(username, room.getName());
                }
            }

            handler.close();
            // 广播用户离开的消息
            broadcastSystemMessage(Message.createSystemMessage(
                    MessageType.USER_LEFT_NOTIFICATION,
                    "用户 " + username + " 已下线"));
        }
    }

    /**
     * 向所有在线用户广播系统消息
     */
    private void broadcastSystemMessage(Message message) {
        onlineUsers.values().forEach(handler -> handler.sendMessage(message));
    }

    /**
     * 发送私聊消息
     */
    public void sendPrivateMessage(Message message) {
        ClientHandler targetHandler = onlineUsers.get(message.getReceiver());
        ClientHandler senderHandler = onlineUsers.get(message.getSender());

        if (targetHandler != null) {
            // 发送给接收者
            Message deliveryMessage = Message.builder()
                    .type(MessageType.PRIVATE_MESSAGE_DELIVERY)
                    .content(message.getContent())
                    .sender(message.getSender())
                    .receiver(message.getReceiver())
                    .timestamp(message.getTimestamp())
                    .build();

            targetHandler.sendMessage(deliveryMessage);

            // 发送给发送者
            if (senderHandler != null) {
                senderHandler.sendMessage(deliveryMessage);
            }
        } else {
            // 如果目标用户不存在，发送错误消息给发送者
            if (senderHandler != null) {
                senderHandler.sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "用户 " + message.getReceiver() + " 不存在或已离线"));
            }
        }
    }

    /**
     * 获取在线用户列表
     */
    public List<String> getOnlineUserList() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    /**
     * 获取聊天室列表
     */
    public List<String> getChatRoomList() {
        return new ArrayList<>(chatRooms.keySet());
    }

    /**
     * 创建新的聊天室
     * @return true 如果创建成功，false 如果房间名已存在
     */
    public boolean createChatRoom(String roomName, String creator) {
        if (chatRooms.containsKey(roomName)) {
            return false;
        }

        ChatRoom newRoom = new ChatRoom(roomName, creator);
        chatRooms.put(roomName, newRoom);
        
        // 广播新聊天室创建的消息
        broadcastSystemMessage(Message.createSystemMessage(
            MessageType.CREATE_ROOM_SUCCESS,
            "新的聊天室 '" + roomName + "' 已创建"
        ));

        joinChatRoom(creator, roomName);

        return true;
    }

    /**
     * 用户加入聊天室
     */
    public boolean joinChatRoom(String username, String roomName) {
        ChatRoom room = chatRooms.get(roomName);
        if (room == null) {
            return false;
        }

        if (room.addMember(username)) {
            // 通知房间内所有成员有新用户加入
            broadcastToRoom(roomName, Message.builder()
                    .type(MessageType.USER_JOINED_ROOM_NOTIFICATION)
                    .content("用户 " + username + " 加入了聊天室")
                    .roomName(roomName)
                    .sender("[系统]")
                    .build());
            return true;
        }
        return false;
    }

    /**
     * 处理用户离开聊天室
     */
    public void handleUserLeaveRoom(String username, String roomName) {
        ChatRoom room = chatRooms.get(roomName);
        if (room != null && room.removeMember(username)) {
            // 通知房间内的其他成员
            broadcastToRoom(roomName, Message.builder()
                    .type(MessageType.USER_LEFT_ROOM_NOTIFICATION)
                    .content("用户 " + username + " 离开了聊天室")
                    .roomName(roomName)
                    .sender("[系统]")
                    .build());

            // 如果房间空了，就删除这个房间
            if (room.isEmpty()) {
                chatRooms.remove(roomName);
                broadcastSystemMessage(Message.createSystemMessage(
                        MessageType.ROOM_DESTROYED_NOTIFICATION,
                        "聊天室 '" + roomName + "' 已被销毁（没有活跃用户）"));
            }
        }
    }

    /**
     * 在聊天室内广播消息
     */
    public void broadcastToRoom(String roomName, Message message) {
        ChatRoom room = chatRooms.get(roomName);
        if (room != null) {
            Set<String> members = room.getMembers();
            for (String member : members) {
                ClientHandler handler = onlineUsers.get(member);
                if (handler != null) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    /**
     * 处理聊天室消息
     */
    public void handleRoomMessage(Message message) {
        String roomName = message.getRoomName();
        ChatRoom room = chatRooms.get(roomName);

        if (room == null) {
            // 聊天室不存在，通知发送者
            ClientHandler sender = onlineUsers.get(message.getSender());
            if (sender != null) {
                sender.sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "聊天室 '" + roomName + "' 不存在"));
            }
            return;
        }

        if (!room.hasMember(message.getSender())) {
            // 发送者不在聊天室中，发送错误消息
            ClientHandler sender = onlineUsers.get(message.getSender());
            if (sender != null) {
                sender.sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "您不是聊天室 '" + roomName + "' 的成员"));
            }
            return;
        }

        // 创建广播消息并发送给所有房间成员
        Message broadcastMessage = Message.builder()
                .type(MessageType.ROOM_MESSAGE_BROADCAST)
                .content(message.getContent())
                .sender(message.getSender())
                .roomName(roomName)
                .timestamp(message.getTimestamp())
                .build();

        broadcastToRoom(roomName, broadcastMessage);
    }

    /**
     * 启动服务器的主方法
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口参数无效，使用默认端口: " + DEFAULT_PORT);
            }
        }

        ChatServer server = new ChatServer(port);
        server.start();
    }
}