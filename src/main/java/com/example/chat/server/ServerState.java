package com.example.chat.server;

import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务器状态类，使用不可变数据和函数式方法管理服务器状态
 */
@Getter
public class ServerState {
    private final int port;
    private final ConcurrentHashMap<String, ClientHandler> onlineUsers;
    private final ConcurrentHashMap<String, ChatRoom> chatRooms;
    private final ExecutorService executorService;
    
    @Setter
    private ServerSocket serverSocket;
    
    private final AtomicBoolean running;

    public ServerState(int port) {
        this.port = port;
        this.onlineUsers = new ConcurrentHashMap<>();
        this.chatRooms = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.running = new AtomicBoolean(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean value) {
        running.set(value);
    }

    /**
     * 获取在线用户列表
     */
    public List<String> getOnlineUserList() {
        return List.copyOf(onlineUsers.keySet());
    }

    /**
     * 获取聊天室列表
     */
    public List<String> getChatRoomList() {
        return List.copyOf(chatRooms.keySet());
    }

    /**
     * 添加在线用户
     */
    public boolean addUser(String username, ClientHandler handler) {
        if (username == null || handler == null) {
            return false;
        }
        return onlineUsers.putIfAbsent(username, handler) == null;
    }

    /**
     * 移除在线用户并获取其处理器
     */
    public Optional<ClientHandler> removeUser(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(onlineUsers.remove(username));
    }

    /**
     * 获取用户处理器
     */
    public Optional<ClientHandler> getClientHandler(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(onlineUsers.get(username));
    }

    /**
     * 获取聊天室
     */
    public Optional<ChatRoom> getChatRoom(String roomName) {
        if (roomName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(chatRooms.get(roomName));
    }

    /**
     * 添加聊天室
     */
    public boolean addChatRoom(String roomName, ChatRoom room) {
        if (roomName == null || room == null) {
            return false;
        }
        return chatRooms.putIfAbsent(roomName, room) == null;
    }

    /**
     * 移除聊天室
     */
    public Optional<ChatRoom> removeChatRoom(String roomName) {
        if (roomName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(chatRooms.remove(roomName));
    }

    /**
     * 获取聊天室信息
     */
    public Optional<Map<String, Object>> getRoomInfo(String roomName) {
        return getChatRoom(roomName).map(room -> Map.of(
            "name", room.getName(),
            "creator", room.getCreator(),
            "creationTime", room.getCreationTime(),
            "members", room.getMembers()
        ));
    }

    /**
     * 获取所有聊天室的不可变视图
     */
    public Map<String, ChatRoom> getChatRooms() {
        return Map.copyOf(chatRooms);
    }

    /**
     * 获取所有在线用户的不可变视图
     */
    public Map<String, ClientHandler> getOnlineUsers() {
        return Map.copyOf(onlineUsers);
    }

    /**
     * 关闭服务器状态
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            onlineUsers.values().forEach(ClientHandler::close);
            onlineUsers.clear();
            chatRooms.clear();
            executorService.shutdown();
        }
    }
}