package com.example.chat.client;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 聊天客户端
 */
@Slf4j
public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private String currentRoom;  // 当前所在的聊天室
    private volatile boolean running;
    private final Scanner scanner;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.scanner = new Scanner(System.in);
        this.running = false;
        this.currentRoom = null;
    }

    public ChatClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * 启动客户端
     */
    public void start() {
        try {
            // 连接到服务器
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            running = true;

            // 处理登录
            if (!login()) {
                return;
            }

            // 启动消息接收线程
            Thread messageReceiver = new Thread(this::receiveMessages);
            messageReceiver.setDaemon(true);
            messageReceiver.start();

            // 处理用户输入
            processUserInput();
        } catch (IOException e) {
            log.error("连接服务器失败: {}", e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * 处理登录
     */
    @SuppressWarnings("unchecked")
    private boolean login() {
        try {
            while (running) {
                System.out.println("请输入您的用户名：");
                String input = scanner.nextLine().trim();
                
                // 验证用户名格式
                if (input.isEmpty()) {
                    System.out.println("用户名不能为空，请重新输入！");
                    continue;
                }
                if (input.startsWith("/")) {
                    System.out.println("用户名不能以'/'开头，请重新输入！");
                    continue;
                }
                if (input.contains(" ")) {
                    System.out.println("用户名不能包含空格，请重新输入！");
                    continue;
                }

                // 发送登录请求
                sendMessage(Message.createLoginRequest(input));
                
                // 等待登录响应或其他系统消息
                while (running) {
                    Message response = (Message) this.input.readObject();
                    
                    switch (response.getType()) {
                        case LOGIN_SUCCESS:
                            this.username = input;
                            System.out.println("登录成功！");
                            
                            // 显示在线用户列表和可用聊天室
                            Map<String, Object> loginData = (Map<String, Object>) response.getData();
                            List<String> onlineUsers = (List<String>) loginData.get("users");
                            List<String> availableRooms = (List<String>) loginData.get("rooms");
                            
                            if (onlineUsers != null && !onlineUsers.isEmpty()) {
                                System.out.println("当前在线用户：" + String.join(", ", onlineUsers));
                            }
                            if (availableRooms != null && !availableRooms.isEmpty()) {
                                System.out.println("当前可用聊天室：" + String.join(", ", availableRooms));
                            }
                            return true;

                        case LOGIN_FAILURE_USERNAME_TAKEN:
                            System.out.println(MessageFormatter.formatMessage(response, null));
                            break;  // 退出内层循环，继续外层循环重新输入用户名

                        case ERROR_MESSAGE:
                            System.out.println(MessageFormatter.formatMessage(response, null));
                            return false;  // 其他错误消息，退出登录流程

                        case USER_JOINED_NOTIFICATION:
                        case USER_LEFT_NOTIFICATION:
                            // 在登录过程中收到的系统通知，直接显示
                            System.out.println(MessageFormatter.formatMessage(response, input));
                            continue;  // 继续等待登录响应

                        default:
                            log.warn("登录过程中收到意外的消息类型：{}", response.getType());
                            continue;  // 继续等待登录响应
                    }
                    break;  // 收到 LOGIN_FAILURE_USERNAME_TAKEN 后退出内层循环
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("登录过程中发生错误: {}", e.getMessage());
            System.out.println("连接异常，请重试...");
            return false;
        }
        return false;
    }

    /**
     * 处理用户输入
     */
    private void processUserInput() {
        displayHelp();

        while (running) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // 清除用户输入的命令行
            System.out.print("\033[1A");  // 光标上移一行
            System.out.print("\033[2K");  // 清除整行

            try {
                if (input.startsWith("/")) {
                    // 处理命令
                    String[] parts = input.split(" ", 2);
                    String command = parts[0];
                    String args = parts.length > 1 ? parts[1].trim() : "";

                    switch (command) {
                        case "/help":
                            displayHelp();
                            break;
                            
                        case "/exit":
                            sendMessage(Message.builder()
                                .type(MessageType.LOGOUT_REQUEST)
                                .sender(username)
                                .build());
                            break;
                            
                        case "/list":
                            sendMessage(Message.createUserListRequest(username));
                            break;
                            
                        case "/rooms":
                            sendMessage(Message.createListRoomsRequest(username));
                            break;
                            
                        case "/create-room":
                            if (!args.isEmpty()) {
                                handleCreateRoom(args);
                            } else {
                                System.out.println("创建聊天室格式：/create-room <房间名>");
                            }
                            break;
                            
                        case "/join":
                            if (!args.isEmpty()) {
                                handleJoinRoom(args);
                            } else {
                                System.out.println("加入聊天室格式：/join <房间名>");
                            }
                            break;
                            
                        case "/leave":
                            if (currentRoom == null) {
                                System.out.println("您当前不在任何聊天室中");
                            } else {
                                handleLeaveRoom(currentRoom);
                            }
                            break;
                            
                        case "/pm":
                            if (!args.isEmpty()) {
                                handlePrivateMessage("/pm " + args);  // 保持原有格式
                            } else {
                                System.out.println("私聊格式：/pm <用户名> <消息>");
                            }
                            break;
                            
                        default:
                            System.out.println("无效的命令。使用 /help 查看可用命令。");
                            break;
                    }
                } else {
                    // 非命令消息发送到当前聊天室
                    if (currentRoom == null) {
                        System.out.println("请先加入一个聊天室再发送消息（使用 /join <房间名>）");
                    } else {
                        sendMessage(Message.createRoomMessage(input, username, currentRoom));
                    }
                }
            } catch (IOException e) {
                log.error("发送消息失败: {}", e.getMessage());
                break;
            }
        }
    }

    /**
     * 处理创建聊天室命令
     */
    private void handleCreateRoom(String roomName) throws IOException {
        sendMessage(Message.createCreateRoomRequest(roomName, username));
    }

    /**
     * 处理加入聊天室命令
     */
    private void handleJoinRoom(String roomName) throws IOException {
        sendMessage(Message.createJoinRoomRequest(roomName, username));
        // 注意：实际的房间切换会在收到 JOIN_ROOM_SUCCESS 消息时完成
    }

    /**
     * 处理离开聊天室命令
     */
    private void handleLeaveRoom(String roomName) throws IOException {
        sendMessage(Message.createLeaveRoomRequest(roomName, username));
        // 注意：实际的房间状态清除会在收到 LEAVE_ROOM_SUCCESS 消息时完成
    }

    /**
     * 处理私聊消息
     */
    private void handlePrivateMessage(String input) throws IOException {
        String[] parts = input.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("私聊格式：/pm <用户名> <消息>");
            return;
        }
        String targetUser = parts[1];
        String content = parts[2];

        // 检查是否试图给自己发送私聊消息
        if (targetUser.equals(username)) {
            System.out.println(MessageFormatter.formatMessage(
                Message.builder()
                    .type(MessageType.ERROR_MESSAGE)
                    .content("不能向自己发送私聊消息")
                    .build(),
                username));
            return;
        }

        sendMessage(Message.createPrivateMessage(content, username, targetUser));
    }

    /**
     * 接收服务器消息的线程
     */
    private void receiveMessages() {
        try {
            while (running) {
                Message message = (Message) input.readObject();
                
                // 显示消息
                displayMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                log.error("接收消息失败: {}", e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            log.error("消息类型转换错误: {}", e.getMessage());
        }
    }

    /**
     * 显示接收到的消息
     */
    private void displayMessage(Message message) {
        String formattedMessage = MessageFormatter.formatMessage(message, username);
        System.out.println(formattedMessage);
        
        // 处理加入房间成功的消息
        if (message.getType() == MessageType.JOIN_ROOM_SUCCESS) {
            currentRoom = message.getRoomName();
        } else if (message.getType() == MessageType.LEAVE_ROOM_SUCCESS) {
            currentRoom = null;
        } else if (message.getType() == MessageType.SERVER_SHUTDOWN_NOTIFICATION) {
            close();
        }
    }

    /**
     * 发送消息到服务器
     */
    private synchronized void sendMessage(Message message) throws IOException {
        output.writeObject(message);
        output.flush();
    }

    /**
     * 关闭客户端连接
     */
    private void close() {
        running = false;
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            scanner.close();
        } catch (IOException e) {
            log.error("关闭连接时发生错误: {}", e.getMessage());
        }
        System.out.println("已断开与服务器的连接");
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
        client.start();
    }

    /**
     * 显示帮助信息
     */
    private void displayHelp() {
        System.out.println("\n=== 聊天室命令说明 ===");
        System.out.println("/help              - 显示此帮助信息");
        System.out.println("/exit              - 退出聊天室");
        System.out.println("/list              - 查看在线用户");
        System.out.println("/rooms             - 查看可用聊天室");
        System.out.println("/create-room <名称> - 创建新聊天室");
        System.out.println("/join <房间名>      - 加入聊天室");
        System.out.println("/leave             - 离开当前聊天室");
        System.out.println("/pm <用户名> <消息>  - 发送私聊消息");
        System.out.println("直接输入消息         - 在当前聊天室发言");
        System.out.println("==================\n");
    }
}