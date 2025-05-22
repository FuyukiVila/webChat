package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 服务器消息处理器，使用函数式方式处理不同类型的消息
 */
@Slf4j
@RequiredArgsConstructor
public class ServerMessageProcessor {
    private final ServerState serverState;
    private final Map<MessageType, BiConsumer<Message, ClientHandler>> handlers;

    public ServerMessageProcessor(ServerState serverState) {
        this.serverState = serverState;
        this.handlers = new HashMap<>();
        initializeHandlers();
    }

    private void initializeHandlers() {
        handlers.put(MessageType.PRIVATE_MESSAGE_REQUEST, this::handlePrivateMessage);
        handlers.put(MessageType.USER_LIST_REQUEST, this::handleUserListRequest);
        handlers.put(MessageType.CREATE_ROOM_REQUEST, this::handleCreateRoom);
        handlers.put(MessageType.JOIN_ROOM_REQUEST, this::handleJoinRoom);
        handlers.put(MessageType.LEAVE_ROOM_REQUEST, this::handleLeaveRoom);
        handlers.put(MessageType.ROOM_MESSAGE_REQUEST, this::handleRoomMessage);
        handlers.put(MessageType.LIST_ROOMS_REQUEST, this::handleListRoomsRequest);
        handlers.put(MessageType.ROOM_INFO_REQUEST, this::handleRoomInfoRequest);
        handlers.put(MessageType.LOGOUT_REQUEST, this::handleLogout);
    }

    /**
     * 处理消息
     */
    public void processMessage(Message message, ClientHandler handler) {
        handlers.getOrDefault(message.getType(), (msg, h) -> 
            log.warn("收到未知类型的消息: {}", msg.getType())
        ).accept(message, handler);
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(Message message, ClientHandler sender) {
        Optional<ClientHandler> targetHandler = serverState.getClientHandler(message.getReceiver());
        Optional<ClientHandler> senderHandler = serverState.getClientHandler(message.getSender());

        Message deliveryMessage = Message.builder()
                .type(MessageType.PRIVATE_MESSAGE_DELIVERY)
                .content(message.getContent())
                .sender(message.getSender())
                .receiver(message.getReceiver())
                .timestamp(message.getTimestamp())
                .build();

        targetHandler.ifPresentOrElse(
            handler -> {
                handler.sendMessage(deliveryMessage);
                senderHandler.ifPresent(sh -> sh.sendMessage(deliveryMessage));
            },
            () -> senderHandler.ifPresent(sh -> sh.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "用户 " + message.getReceiver() + " 不存在或已离线"
            )))
        );
    }

    /**
     * 处理用户列表请求
     */
    private void handleUserListRequest(Message message, ClientHandler handler) {
        List<String> userList = serverState.getOnlineUserList();
        handler.sendMessage(Message.builder()
                .type(MessageType.USER_LIST_RESPONSE)
                .data(userList)
                .sender("SERVER")
                .build());
    }

    /**
     * 处理创建聊天室请求
     */
    private void handleCreateRoom(Message message, ClientHandler handler) {
        String roomName = message.getRoomName();
        String username = message.getSender();

        if (!isValidName(roomName)) {
            handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "房间名只能包含大小写字母、数字和下划线"
            ));
            return;
        }

        ChatRoom newRoom = new ChatRoom(roomName, username);
        if (serverState.addChatRoom(roomName, newRoom)) {
            broadcastSystemMessage(Message.createSystemMessage(
                MessageType.CREATE_ROOM_SUCCESS,
                "新的聊天室 '" + roomName + "' 已创建"
            ));

            // 创建者自动加入房间
            handleJoinRoom(message, handler);
        } else {
            handler.sendMessage(Message.createSystemMessage(
                MessageType.CREATE_ROOM_FAILURE,
                "创建聊天室失败：名称 '" + roomName + "' 已被使用"
            ));
        }
    }

    /**
     * 处理加入聊天室请求
     */
    private void handleJoinRoom(Message message, ClientHandler handler) {
        String roomName = message.getRoomName();
        String username = message.getSender();

        if (!isValidName(roomName)) {
            handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "房间名只能包含大小写字母、数字和下划线"
            ));
            return;
        }

        serverState.getChatRoom(roomName).ifPresentOrElse(
            room -> {
                if (room.addMember(username)) {
                    // 通知房间内所有成员有新用户加入
                    broadcastToRoom(roomName, Message.builder()
                            .type(MessageType.USER_JOINED_ROOM_NOTIFICATION)
                            .content("用户 " + username + " 加入了聊天室")
                            .roomName(roomName)
                            .sender("SERVER")
                            .build());

                    handler.sendMessage(Message.builder()
                            .type(MessageType.JOIN_ROOM_SUCCESS)
                            .content("成功加入聊天室 '" + roomName + "'")
                            .roomName(roomName)
                            .sender("SERVER")
                            .build());
                } else {
                    handler.sendMessage(Message.createSystemMessage(
                        MessageType.JOIN_ROOM_FAILURE,
                        "加入聊天室失败：您已在房间中"
                    ));
                }
            },
            () -> handler.sendMessage(Message.createSystemMessage(
                MessageType.JOIN_ROOM_FAILURE,
                "加入聊天室失败：聊天室 '" + roomName + "' 不存在"
            ))
        );
    }

    /**
     * 处理离开聊天室请求
     */
    private void handleLeaveRoom(Message message, ClientHandler handler) {
        String roomName = message.getRoomName();
        String username = message.getSender();

        serverState.getChatRoom(roomName).ifPresent(room -> {
            if (room.removeMember(username)) {
                // 通知房间内的其他成员
                broadcastToRoom(roomName, Message.builder()
                        .type(MessageType.USER_LEFT_ROOM_NOTIFICATION)
                        .content("用户 " + username + " 离开了聊天室")
                        .roomName(roomName)
                        .sender("SERVER")
                        .build());

                handler.sendMessage(Message.createSystemMessage(
                    MessageType.LEAVE_ROOM_SUCCESS,
                    "已离开聊天室 '" + roomName + "'"
                ));

                // 如果房间空了，就删除这个房间
                if (room.isEmpty()) {
                    serverState.removeChatRoom(roomName);
                    broadcastSystemMessage(Message.createSystemMessage(
                        MessageType.ROOM_DESTROYED_NOTIFICATION,
                        "聊天室 '" + roomName + "' 已被销毁（没有活跃用户）"
                    ));
                }
            }
        });
    }

    /**
     * 处理聊天室消息
     */
    private void handleRoomMessage(Message message, ClientHandler handler) {
        String roomName = message.getRoomName();
        String username = message.getSender();

        if (roomName == null) {
            handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "消息缺少聊天室名称"
            ));
            return;
        }

        serverState.getChatRoom(roomName).ifPresentOrElse(
            room -> {
                if (!room.hasMember(username)) {
                    handler.sendMessage(Message.createSystemMessage(
                        MessageType.ERROR_MESSAGE,
                        "您不是聊天室 '" + roomName + "' 的成员"
                    ));
                    return;
                }

                Message broadcastMessage = Message.builder()
                        .type(MessageType.ROOM_MESSAGE_BROADCAST)
                        .content(message.getContent())
                        .sender(username)
                        .roomName(roomName)
                        .timestamp(message.getTimestamp())
                        .build();

                broadcastToRoom(roomName, broadcastMessage);
            },
            () -> handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "聊天室 '" + roomName + "' 不存在"
            ))
        );
    }

    /**
     * 处理列出聊天室请求
     */
    private void handleListRoomsRequest(Message message, ClientHandler handler) {
        List<String> roomList = serverState.getChatRoomList();
        handler.sendMessage(Message.builder()
                .type(MessageType.LIST_ROOMS_RESPONSE)
                .data(roomList)
                .build());
    }

    /**
     * 处理房间信息请求
     */
    private void handleRoomInfoRequest(Message message, ClientHandler handler) {
        String roomName = message.getRoomName();
        if (roomName == null) {
            handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "请求房间信息失败：聊天室名称不能为空"
            ));
            return;
        }

        serverState.getChatRoom(roomName).ifPresentOrElse(
            room -> {
                Map<String, Object> roomInfo = new HashMap<>();
                roomInfo.put("name", room.getName());
                roomInfo.put("creator", room.getCreator());
                roomInfo.put("creationTime", room.getCreationTime());
                roomInfo.put("members", new ArrayList<>(room.getMembers()));

                handler.sendMessage(Message.builder()
                        .type(MessageType.ROOM_INFO_RESPONSE)
                        .data(roomInfo)
                        .roomName(roomName)
                        .build());
            },
            () -> handler.sendMessage(Message.createSystemMessage(
                MessageType.ERROR_MESSAGE,
                "请求房间信息失败：聊天室 '" + roomName + "' 不存在"
            ))
        );
    }

    /**
     * 处理登出请求
     */
    private void handleLogout(Message message, ClientHandler handler) {
        String username = message.getSender();
        
        // 获取用户所在的所有房间
        List<String> userRooms = serverState.getChatRooms().entrySet().stream()
            .filter(entry -> entry.getValue().hasMember(username))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 让用户离开所有房间
        userRooms.forEach(roomName -> 
            handleLeaveRoom(Message.builder()
                .type(MessageType.LEAVE_ROOM_REQUEST)
                .sender(username)
                .roomName(roomName)
                .build(), 
                handler)
        );

        // 发送登出确认消息
        handler.sendMessage(Message.createSystemMessage(
            MessageType.LOGOUT_CONFIRMATION,
            "您已成功登出"
        ));
        
        // 关闭连接
        handler.close();
    }

    /**
     * 向所有在线用户广播系统消息
     */
    private void broadcastSystemMessage(Message message) {
        serverState.getOnlineUsers().values()
            .forEach(handler -> handler.sendMessage(message));
    }

    /**
     * 在聊天室内广播消息
     */
    private void broadcastToRoom(String roomName, Message message) {
        serverState.getChatRoom(roomName).ifPresent(room ->
            room.getMembers().stream()
                .map(username -> serverState.getClientHandler(username))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(handler -> handler.sendMessage(message))
        );
    }

    /**
     * 验证名称格式（用户名或房间名）
     * 只允许使用大小写字母、数字和下划线
     */
    private boolean isValidName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }
}