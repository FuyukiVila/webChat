package com.example.chat.client;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.Arrays;

/**
 * 命令处理器，使用函数式方式处理不同的命令
 */
public class CommandHandler {
    private final ClientState state;
    private final MessageHandler messageHandler;
    private final MessageDisplay display;
    private final Map<String, BiFunction<String[], ClientState, Boolean>> commands;

    public CommandHandler(ClientState state, MessageHandler messageHandler) {
        this.state = state;
        this.messageHandler = messageHandler;
        this.display = new MessageDisplay();
        this.commands = new HashMap<>();
        initializeCommands();
    }

    private void initializeCommands() {
        commands.put("/clear", (args, state) -> {
            // 使用ANSI转义序列清屏
            System.out.print("\033[H\033[2J");
            System.out.flush();
            return true;
        });

        commands.put("/help", (args, state) -> {
            display.displayHelp();
            return true;
        });

        commands.put("/exit", (args, state) -> {
            try {
                messageHandler.sendMessage(Message.builder()
                        .type(MessageType.LOGOUT_REQUEST)
                        .sender(state.getUsername())
                        .build());
                return true;
            } catch (IOException e) {
                display.displayError("发送退出请求失败: " + e.getMessage());
                return false;
            }
        });

        commands.put("/list", (args, state) -> {
            try {
                messageHandler.sendMessage(Message.createUserListRequest(state.getUsername()));
                return true;
            } catch (IOException e) {
                display.displayError("获取用户列表失败: " + e.getMessage());
                return false;
            }
        });

        commands.put("/rooms", (args, state) -> {
            try {
                messageHandler.sendMessage(Message.createListRoomsRequest(state.getUsername()));
                return true;
            } catch (IOException e) {
                display.displayError("获取聊天室列表失败: " + e.getMessage());
                return false;
            }
        });

        commands.put("/create-room", (args, state) -> {
            if (args.length < 2) {
                display.displayHint("创建聊天室格式：/create-room <房间名>");
                return false;
            }
            return handleCreateRoom(args[1]);
        });

        commands.put("/join", (args, state) -> {
            if (args.length < 2) {
                display.displayHint("加入聊天室格式：/join <房间名>");
                return false;
            }
            return handleJoinRoom(args[1]);
        });

        commands.put("/leave", (args, state) -> {
            return state.getCurrentRoom()
                    .map(this::handleLeaveRoom)
                    .orElseGet(() -> {
                        display.displayError("您当前不在任何聊天室中");
                        return false;
                    });
        });

        commands.put("/room-info", (args, state) -> {
            return state.getCurrentRoom()
                    .map(room -> {
                        try {
                            messageHandler.sendMessage(Message.createRoomInfoRequest(state.getUsername(), room));
                            return true;
                        } catch (IOException e) {
                            display.displayError("获取房间信息失败: " + e.getMessage());
                            return false;
                        }
                    })
                    .orElseGet(() -> {
                        display.displayError("您当前不在任何聊天室中");
                        return false;
                    });
        });

        commands.put("/pm", (args, state) -> {
            if (args.length < 3) {
                display.displayHint("私聊格式：/pm <用户名> <消息>");
                return false;
            }
            return handlePrivateMessage(args);
        });
    }

    /**
     * 处理用户输入
     */
    public void handleInput(String input) {
        if (input.isEmpty()) {
            return;
        }

        // 清除用户输入的命令行
        System.out.print("\033[1A"); // 光标上移一行
        System.out.print("\033[2K"); // 清除整行

        if (input.startsWith("/")) {
            String[] args = input.split(" ");
            String command = args[0];

            commands.getOrDefault(command, (cmdArgs, state) -> {
                display.displayError("无效的命令。使用 /help 查看可用命令。");
                return false;
            }).apply(args, state);
        } else {
            // 非命令消息发送到当前聊天室
            state.getCurrentRoom()
                .map(room -> {
                    try {
                        messageHandler.sendMessage(Message.createRoomMessage(input, state.getUsername(), room));
                        return true;
                    } catch (IOException e) {
                        display.displayError("发送消息失败: " + e.getMessage());
                        return false;
                    }
                })
                .orElseGet(() -> {
                    display.displayHint("请先加入一个聊天室再发送消息（使用 /join <房间名>）");
                    return false;
                });
        }
    }

    private boolean handleCreateRoom(String roomName) {
        if (!isValidName(roomName)) {
            display.displayError("房间名只能包含大小写字母、数字和下划线！");
            return false;
        }

        // 如果已经在某个聊天室中，不允许创建新聊天室
        if (state.getCurrentRoom().isPresent()) {
            display.displayError("您已经在聊天室中，请先使用 /leave 命令退出当前聊天室");
            return false;
        }

        try {
            messageHandler.sendMessage(Message.createCreateRoomRequest(roomName, state.getUsername()));
            return true;
        } catch (IOException e) {
            display.displayError("创建聊天室失败: " + e.getMessage());
            return false;
        }
    }

    private boolean handleJoinRoom(String roomName) {
        if (!isValidName(roomName)) {
            display.displayError("房间名只能包含大小写字母、数字和下划线！");
            return false;
        }

        // 如果已经在某个聊天室中，不允许加入新聊天室
        if (state.getCurrentRoom().isPresent()) {
            display.displayError("您已经在聊天室中，请先使用 /leave 命令退出当前聊天室");
            return false;
        }

        try {
            messageHandler.sendMessage(Message.createJoinRoomRequest(roomName, state.getUsername()));
            return true;
        } catch (IOException e) {
            display.displayError("加入聊天室失败: " + e.getMessage());
            return false;
        }
    }

    private boolean handleLeaveRoom(String roomName) {
        try {
            messageHandler.sendMessage(Message.createLeaveRoomRequest(roomName, state.getUsername()));
            return true;
        } catch (IOException e) {
            display.displayError("离开聊天室失败: " + e.getMessage());
            return false;
        }
    }

    private boolean handlePrivateMessage(String[] args) {
        String targetUser = args[1];
        String content = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // 检查是否试图给自己发送私聊消息
        if (targetUser.equals(state.getUsername())) {
            display.displayError("不能向自己发送私聊消息");
            return false;
        }

        try {
            messageHandler.sendMessage(Message.createPrivateMessage(content, state.getUsername(), targetUser));
            return true;
        } catch (IOException e) {
            display.displayError("发送私聊消息失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 验证名称格式（用户名或房间名）
     * 只允许使用大小写字母、数字和下划线
     */
    private boolean isValidName(String name) {
        return name.matches("^[a-zA-Z0-9_]+$");
    }
}