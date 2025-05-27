package com.example.chat.client;

import com.example.chat.client.shell.MessageDisplay;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import java.util.List;

import java.io.IOException;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 消息处理器，使用函数式方式处理不同类型的消息
 */
public class MessageHandler {
    private final ClientState state;
    private final Map<MessageType, BiConsumer<Message, ClientState>> handlers;
    private final MessageDisplay display;

    public MessageHandler(ClientState state) {
        this.state = state;
        this.handlers = new HashMap<>();
        this.display = new MessageDisplay();
        initializeHandlers();
    }

    private void initializeHandlers() {
        // 注册各种消息类型的处理器
        handlers.put(MessageType.ROOM_HISTORY_RESPONSE, (message, state) -> {
            display.displayInfo("=== 历史消息 ===");
            @SuppressWarnings("unchecked")
            List<Message> history = (List<Message>) message.getData();
            history.forEach(msg -> display.display(msg, state.getUsername()));
            display.displayInfo("=== 历史消息结束 ===");
        });

        handlers.put(MessageType.JOIN_ROOM_SUCCESS, (message, state) -> {
            state.setCurrentRoom(Optional.ofNullable(message.getRoomName()));
            display.display(message, state.getUsername());
        });

        handlers.put(MessageType.LEAVE_ROOM_SUCCESS, (message, state) -> {
            state.setCurrentRoom(Optional.empty());
            display.display(message, state.getUsername());
        });

        handlers.put(MessageType.ROOM_INFO_RESPONSE, (message, state) -> display.display(message, state.getUsername()));

        handlers.put(MessageType.LIST_ROOMS_RESPONSE,
                (message, state) -> display.display(message, state.getUsername()));

        handlers.put(MessageType.USER_LIST_RESPONSE, (message, state) -> display.display(message, state.getUsername()));

        handlers.put(MessageType.LOGOUT_CONFIRMATION, (message, state) -> {
            display.display(message, state.getUsername());
            state.close();
        });

        handlers.put(MessageType.SERVER_SHUTDOWN_NOTIFICATION, (message, state) -> {
            display.display(message, state.getUsername());
            state.close();
        });
    }

    /**
     * 处理接收到的消息
     */
    public void handleMessage(Message message) {
        handlers.getOrDefault(message.getType(),
                (msg, state) -> display.display(msg, state.getUsername()))
                .accept(message, state);
    }

    /**
     * 发送消息到服务器
     */
    public void sendMessage(Message message) throws IOException {
        synchronized (state) {
            state.getOutput().writeObject(message);
            state.getOutput().flush();
        }
    }
}