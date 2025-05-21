package com.example.chat.client;

import com.example.chat.common.Message;

/**
 * 消息格式化工具类，负责消息的颜色和格式处理
 */
public class MessageFormatter {
    // ANSI 颜色代码
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";     // 错误消息
    private static final String ANSI_GREEN = "\u001B[32m";   // 系统消息
    private static final String ANSI_YELLOW = "\u001B[33m";  // 接收的私聊
    private static final String ANSI_BLUE = "\u001B[34m";    // 聊天室消息
    private static final String ANSI_MAGENTA = "\u001B[35m"; // 用户名
    private static final String ANSI_CYAN = "\u001B[36m";    // 发出的私聊
    private static final String ANSI_BRIGHT_CYAN = "\u001B[36;1m"; // 聊天室名称

    /**
     * 格式化消息用于显示
     * @param message 要格式化的消息
     * @param currentUsername 当前用户名（用于判断消息的发送/接收状态）
     * @return 格式化后的消息字符串
     */
    public static String formatMessage(Message message, String currentUsername) {
        if (message == null) {
            return "";
        }

        switch (message.getType()) {
            // 聊天室消息
            case ROOM_MESSAGE_BROADCAST:
                return String.format("%s[%s%s%s] %s%s%s: %s%s",
                    ANSI_BLUE, ANSI_BRIGHT_CYAN, message.getRoomName(), ANSI_BLUE,
                    ANSI_MAGENTA, message.getSender(), ANSI_BLUE,
                    message.getContent(), ANSI_RESET);

            // 私聊消息
            case PRIVATE_MESSAGE_DELIVERY:
                String color = message.getSender().equals(currentUsername) ? ANSI_CYAN : ANSI_YELLOW;
                return String.format("%s[私聊] %s%s%s: %s%s",
                    color, ANSI_MAGENTA, message.getSender(), color,
                    message.getContent(), ANSI_RESET);

            // 聊天室操作结果
            case CREATE_ROOM_SUCCESS:
            case JOIN_ROOM_SUCCESS:
            case LEAVE_ROOM_SUCCESS:
                return String.format("%s[系统] %s%s",
                    ANSI_GREEN, message.getContent(), ANSI_RESET);

            case CREATE_ROOM_FAILURE:
            case JOIN_ROOM_FAILURE:
                return String.format("%s[错误] %s%s",
                    ANSI_RED, message.getContent(), ANSI_RESET);

            // 聊天室状态变更通知
            case USER_JOINED_ROOM_NOTIFICATION:
            case USER_LEFT_ROOM_NOTIFICATION:
                return String.format("%s[%s%s%s] %s%s",
                    ANSI_GREEN, ANSI_BRIGHT_CYAN, message.getRoomName(), ANSI_GREEN,
                    message.getContent(), ANSI_RESET);

            case ROOM_DESTROYED_NOTIFICATION:
                return String.format("%s[系统] %s%s",
                    ANSI_RED, message.getContent(), ANSI_RESET);

            // 用户列表
            case USER_LIST_RESPONSE:
                @SuppressWarnings("unchecked")
                java.util.List<String> users = (java.util.List<String>) message.getData();
                String coloredUsers = users.stream()
                    .map(user -> ANSI_MAGENTA + user + ANSI_GREEN)
                    .collect(java.util.stream.Collectors.joining(", "));
                return String.format("%s当前在线用户：%s%s",
                    ANSI_GREEN, coloredUsers, ANSI_RESET);

            // 聊天室列表
            case LIST_ROOMS_RESPONSE:
                @SuppressWarnings("unchecked")
                java.util.List<String> rooms = (java.util.List<String>) message.getData();
                if (rooms.isEmpty()) {
                    return String.format("%s当前没有可用的聊天室%s",
                        ANSI_GREEN, ANSI_RESET);
                }
                String coloredRooms = rooms.stream()
                    .map(room -> ANSI_BRIGHT_CYAN + room + ANSI_GREEN)
                    .collect(java.util.stream.Collectors.joining(", "));
                return String.format("%s可用聊天室：%s%s",
                    ANSI_GREEN, coloredRooms, ANSI_RESET);

            // 用户状态通知
            case USER_JOINED_NOTIFICATION:
            case USER_LEFT_NOTIFICATION:
            case SERVER_SHUTDOWN_NOTIFICATION:
                return String.format("%s[系统] %s%s",
                    ANSI_GREEN, message.getContent(), ANSI_RESET);

            // 错误消息
            case ERROR_MESSAGE:
            case LOGIN_FAILURE_USERNAME_TAKEN:
                return String.format("%s[错误] %s%s",
                    ANSI_RED, message.getContent(), ANSI_RESET);

            default:
                return String.format("%s[系统] %s%s",
                    ANSI_BLUE, message.getContent(), ANSI_RESET);
        }
    }
}