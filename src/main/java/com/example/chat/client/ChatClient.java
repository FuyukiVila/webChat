package com.example.chat.client;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 聊天客户端
 */
@Slf4j
public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private final ClientState state;
    private final MessageHandler messageHandler;
    private final CommandHandler commandHandler;
    private final MessageDisplay display;

    public ChatClient(String host, int port) {
        this.state = new ClientState(host, port);
        this.messageHandler = new MessageHandler(state);
        this.commandHandler = new CommandHandler(state, messageHandler);
        this.display = new MessageDisplay();
    }

    public ChatClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * 启动客户端
     */
    public void start() {
        display.displayInfo("\n=== 聊天客户端启动 ===\n* 服务器地址: " + state.getHost() + 
                             "\n* 端口: " + state.getPort() + 
                             "\n* 按 Ctrl+C 退出程序\n===================\n");

        try {
            display.displayInfo(String.format("正在连接到服务器 %s:%d...", state.getHost(), state.getPort()));

            initializeConnection()
                    .flatMap(this::handleLogin)
                    .ifPresent(success -> {
                        if (success) {
                            startMessageReceiver();
                            processUserInput();
                        }
                    });
        } catch (IOException e) {
            log.error("连接服务器失败: {}", e.getMessage());
        } finally {
            state.close();
        }
    }

    /**
     * 初始化连接
     */
    private Optional<Boolean> initializeConnection() throws IOException {
        Socket socket = new Socket(state.getHost(), state.getPort());
        state.setSocket(socket);
        state.setOutput(new ObjectOutputStream(socket.getOutputStream()));
        state.setInput(new ObjectInputStream(socket.getInputStream()));
        state.setRunning(true);
        display.displayInfo("已成功连接到服务器");
        return Optional.of(true);
    }

    /**
     * 处理登录
     */
    private Optional<Boolean> handleLogin(boolean connected) {
        try {
            while (true) {  // 修改为无限循环，直到成功登录或发生异常
                display.displayInfo("请输入您的用户名：");
                String input = state.getScanner().nextLine().trim();

                if (!validateUsername(input)) {
                    continue;
                }

                messageHandler.sendMessage(Message.createLoginRequest(input));

                while (state.isRunning()) {
                    Message response = (Message) state.getInput().readObject();
                    if (handleLoginResponse(response, input)) {
                        return Optional.of(true);
                    }
                    if (response.getType() == MessageType.LOGIN_FAILURE_USERNAME_TAKEN) {
                        break;  // 用户名被占用，跳出内层循环重新输入
                    }
                }

                state.setRunning(true);  // 重置运行状态，允许重新尝试
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("登录过程中发生错误: {}", e.getMessage());
            display.displayError("连接异常，请重试...");
        }
        return Optional.of(false);
    }

    /**
     * 验证用户名
     */
    private boolean validateUsername(String username) {
        if (username.isEmpty()) {
            display.displayError("用户名不能为空，请重新输入！");
            return false;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            display.displayError("用户名只能包含大小写字母、数字和下划线，请重新输入！");
            return false;
        }
        return true;
    }

    /**
     * 处理登录响应
     */
    @SuppressWarnings("unchecked")
    private boolean handleLoginResponse(Message response, String username) {
        switch (response.getType()) {
            case LOGIN_SUCCESS:
                state.setUsername(username);
                display.displayInfo("登录成功！");

                // 显示在线用户列表和可用聊天室
                Map<String, Object> loginData = (Map<String, Object>) response.getData();
                Optional.ofNullable(loginData.get("users"))
                        .map(users -> (List<String>) users)
                        .filter(users -> !users.isEmpty())
                        .ifPresent(users -> display.displayInfo("当前在线用户：" + String.join(", ", users)));

                Optional.ofNullable(loginData.get("rooms"))
                        .map(rooms -> (List<String>) rooms)
                        .filter(rooms -> !rooms.isEmpty())
                        .ifPresent(rooms -> display.displayInfo("当前可用聊天室：" + String.join(", ", rooms)));

                return true;

            case LOGIN_FAILURE_USERNAME_TAKEN:
                display.display(response, null);
                state.setRunning(false); // 中断内部循环
                return false;

            case ERROR_MESSAGE:
                display.display(response, null);
                return true;

            case USER_JOINED_NOTIFICATION:
            case USER_LEFT_NOTIFICATION:
                display.display(response, username);
                return false;

            default:
                log.warn("登录过程中收到意外的消息类型：{}", response.getType());
                return false;
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        Thread messageReceiver = new Thread(() -> {
            try {
                while (state.isRunning()) {
                    Message message = (Message) state.getInput().readObject();
                    messageHandler.handleMessage(message);
                }
            } catch (IOException e) {
                if (state.isRunning()) {
                    log.error("接收消息失败: {}", e.getMessage());
                }
            } catch (ClassNotFoundException e) {
                log.error("消息类型转换错误: {}", e.getMessage());
            }
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    /**
     * 处理用户输入
     */
    private void processUserInput() {
        display.displayHelp();

        while (state.isRunning()) {
            String input = state.getScanner().nextLine().trim();
            if (!input.isEmpty()) {
                commandHandler.handleInput(input);
            }
        }
    }

    /**
     * 启动客户端的主方法
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("端口参数无效，使用默认端口: " + DEFAULT_PORT);
            }
        }

        ChatClient client = new ChatClient(host, port);

        // 添加关闭钩子，确保在Ctrl+C时正确退出
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client.state.isRunning()) {
                try {
                    // 发送登出消息
                    client.messageHandler.sendMessage(Message.builder()
                            .type(MessageType.LOGOUT_REQUEST)
                            .sender(client.state.getUsername())
                            .timestamp(new Date())
                            .build());
                    // 等待服务器响应
                    Thread.sleep(100);
                } catch (Exception e) {
                    // 忽略关闭过程中的异常
                } finally {
                    client.state.close();
                }
            }
        }));

        client.start();
    }
}