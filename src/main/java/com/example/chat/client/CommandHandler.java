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
        commands.put("/passwd", this::handlePasswdCommand);
        commands.put("/clear", this::handleClearCommand);
        commands.put("/help", this::handleHelpCommand);
        commands.put("/exit", this::handleExitCommand);
        commands.put("/list", this::handleListCommand);
        commands.put("/rooms", this::handleRoomsCommand);
        commands.put("/create-room", this::handleCreateRoomCommand);
        commands.put("/join", this::handleJoinCommand);
        commands.put("/leave", this::handleLeaveCommand);
        commands.put("/room-info", this::handleRoomInfoCommand);
        commands.put("/pm", this::handlePmCommand);
    }

    /**
     * 处理修改密码命令
     */
    private boolean handlePasswdCommand(String[] args, ClientState state) {
        if (args.length < 2) {
            display.displayHint("修改房间密码格式：/passwd room-name <new-password>");
            return false;
        }
        try {
            String roomName = args[1];
            String newPassword = args.length > 2 ? args[2] : "";
            if (newPassword != null && !newPassword.isEmpty() && !isValidName(newPassword)) {
                display.displayError("密码只能包含大小写字母、数字和下划线！");
                return false;
            }
            messageHandler.sendMessage(Message.createChangePasswordRequest(roomName, state.getUsername(), newPassword));
            return true;
        } catch (IOException e) {
            display.displayError("修改密码失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理清屏命令
     */
    private boolean handleClearCommand(String[] args, ClientState state) {
        // 使用ANSI转义序列清屏
        System.out.print("\033[H\033[2J");
        System.out.flush();
        return true;
    }

    /**
     * 处理帮助命令
     */
    private boolean handleHelpCommand(String[] args, ClientState state) {
        display.displayHelp();
        return true;
    }

    /**
     * 处理退出命令
     */
    private boolean handleExitCommand(String[] args, ClientState state) {
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
    }

    /**
     * 处理用户列表命令
     */
    private boolean handleListCommand(String[] args, ClientState state) {
        try {
            messageHandler.sendMessage(Message.createUserListRequest(state.getUsername()));
            return true;
        } catch (IOException e) {
            display.displayError("获取用户列表失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理聊天室列表命令
     */
    private boolean handleRoomsCommand(String[] args, ClientState state) {
        try {
            messageHandler.sendMessage(Message.createListRoomsRequest(state.getUsername()));
            return true;
        } catch (IOException e) {
            display.displayError("获取聊天室列表失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理创建聊天室命令
     */
    private boolean handleCreateRoomCommand(String[] args, ClientState state) {
        if (args.length < 2) {
            display.displayHint("创建聊天室格式：/create-room room-name <password>");
            return false;
        }
        String password = args.length > 2 ? args[2] : "";
        return handleCreateRoom(args[1], password);
    }

    /**
     * 处理加入聊天室命令
     */
    private boolean handleJoinCommand(String[] args, ClientState state) {
        if (args.length < 2) {
            display.displayHint("加入聊天室格式：/join room-name <password>");
            return false;
        }
        String password = args.length > 2 ? args[2] : "";
        return handleJoinRoom(args[1], password);
    }

    /**
     * 处理离开聊天室命令
     */
    private boolean handleLeaveCommand(String[] args, ClientState state) {
        return state.getCurrentRoom()
                .map(this::handleLeaveRoom)
                .orElseGet(() -> {
                    display.displayError("您当前不在任何聊天室中");
                    return false;
                });
    }

    /**
     * 处理房间信息命令
     */
    private boolean handleRoomInfoCommand(String[] args, ClientState state) {
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
    }

    /**
     * 处理私聊命令
     */
    private boolean handlePmCommand(String[] args, ClientState state) {
        if (args.length < 3) {
            display.displayHint("私聊格式：/pm <用户名> <消息>");
            return false;
        }
        return handlePrivateMessage(args);
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

    private boolean handleCreateRoom(String roomName, String password) {
        if (!isValidName(roomName)) {
            display.displayError("房间名只能包含大小写字母、数字和下划线！");
            return false;
        }

        if (roomName.equals(state.getCurrentRoom().orElse(null))) {
            display.displayError("您已经在该聊天室中！");
            return false;
        }

        if (password != null && !password.isEmpty() && !isValidName(password)) {
            display.displayError("密码只能包含大小写字母、数字和下划线！");
            return false;
        }

        // 如果已经在某个聊天室中，则先离开当前聊天室
        state.getCurrentRoom().ifPresent(currentRoom -> {
            handleLeaveRoom(currentRoom);
        });

        try {
            messageHandler.sendMessage(Message.createCreateRoomRequest(roomName, state.getUsername(), password));
            return true;
        } catch (IOException e) {
            display.displayError("创建聊天室失败: " + e.getMessage());
            return false;
        }
    }

    private boolean handleJoinRoom(String roomName, String password) {
        if (!isValidName(roomName)) {
            display.displayError("房间名只能包含大小写字母、数字和下划线！");
            return false;
        }

        if (roomName.equals(state.getCurrentRoom().orElse(null))) {
            display.displayError("您已经在该聊天室中！");
            return false;
        }

        // 如果已经在某个聊天室中，则先离开当前聊天室
        state.getCurrentRoom().ifPresent(currentRoom -> {
            handleLeaveRoom(currentRoom);
        });

        try {
            messageHandler.sendMessage(Message.createJoinRoomRequest(roomName, state.getUsername(), password));
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
        return name != null && !name.isEmpty() && name.matches("^[a-zA-Z0-9_]+$");
    }
}