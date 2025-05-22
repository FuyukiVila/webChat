package com.example.chat.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息类，用于客户端和服务器之间的所有通信
 * 实现 Serializable 接口以支持网络传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {
    private static final long serialVersionUID = 3L; // 更新版本号，因为添加了新字段
    
    private MessageType type;          // 消息类型
    private String content;            // 消息内容
    private String sender;             // 发送者用户名
    private String receiver;           // 接收者用户名（仅用于私聊）
    private String roomName;           // 聊天室名称（用于聊天室相关消息）
    private Object data;               // 附加数据（如用户列表、聊天室列表、房间密码等）
    @Builder.Default
    private Date timestamp = new Date(); // 消息时间戳

    /**
     * 创建一个系统消息（如错误消息、通知等）
     */
    public static Message createSystemMessage(MessageType type, String content) {
        return Message.builder()
                .type(type)
                .content(content)
                .sender("SERVER")
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个私聊消息
     */
    public static Message createPrivateMessage(String content, String sender, String receiver) {
        return Message.builder()
                .type(MessageType.PRIVATE_MESSAGE_REQUEST)
                .content(content)
                .sender(sender)
                .receiver(receiver)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个聊天室消息
     */
    public static Message createRoomMessage(String content, String sender, String roomName) {
        return Message.builder()
                .type(MessageType.ROOM_MESSAGE_REQUEST)
                .content(content)
                .sender(sender)
                .roomName(roomName)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个创建聊天室的请求消息
     */
    public static Message createCreateRoomRequest(String roomName, String username, String password) {
        return Message.builder()
                .type(MessageType.CREATE_ROOM_REQUEST)
                .sender(username)
                .roomName(roomName)
                .data(password)  // 使用data字段存储密码
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个加入聊天室的请求消息
     */
    public static Message createJoinRoomRequest(String roomName, String username, String password) {
        return Message.builder()
                .type(MessageType.JOIN_ROOM_REQUEST)
                .sender(username)
                .roomName(roomName)
                .data(password)  // 使用data字段存储密码
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个离开聊天室的请求消息
     */
    public static Message createLeaveRoomRequest(String roomName, String username) {
        return Message.builder()
                .type(MessageType.LEAVE_ROOM_REQUEST)
                .sender(username)
                .roomName(roomName)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个登录请求消息
     */
    public static Message createLoginRequest(String username) {
        return Message.builder()
                .type(MessageType.LOGIN_REQUEST)
                .sender(username)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个请求用户列表的消息
     */
    public static Message createUserListRequest(String username) {
        return Message.builder()
                .type(MessageType.USER_LIST_REQUEST)
                .sender(username)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个请求聊天室列表的消息
     */
    public static Message createListRoomsRequest(String username) {
        return Message.builder()
                .type(MessageType.LIST_ROOMS_REQUEST)
                .sender(username)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建一个请求房间信息的消息
     */
    public static Message createRoomInfoRequest(String username, String roomName) {
        return Message.builder()
                .type(MessageType.ROOM_INFO_REQUEST)
                .sender(username)
                .roomName(roomName)
                .timestamp(new Date())
                .build();
    }
    
    /**
     * 创建一个修改房间密码的请求消息
     */
    public static Message createChangePasswordRequest(String roomName, String username, String newPassword) {
        return Message.builder()
                .type(MessageType.CHANGE_ROOM_PASSWORD_REQUEST)
                .sender(username)
                .roomName(roomName)
                .data(newPassword)  // 使用data字段存储新密码
                .timestamp(new Date())
                .build();
    }
}