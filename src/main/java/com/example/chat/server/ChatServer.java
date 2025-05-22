package com.example.chat.server;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 聊天服务器主类
 */
@Slf4j
public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    private final ServerState state;
    private final ServerMessageProcessor messageProcessor;
    private final AtomicReference<CompletableFuture<Void>> shutdownFuture = new AtomicReference<>();

    public ChatServer(int port) {
        this.state = new ServerState(port);
        this.messageProcessor = new ServerMessageProcessor(state);
    }

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    /**
     * 启动服务器
     */
    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(state.getPort());
            state.setServerSocket(serverSocket);
            state.setRunning(true);

            log.info("聊天服务器启动成功，正在监听端口: {}", state.getPort());
            log.info("按 Ctrl+C 可以安全关闭服务器");
            System.out.println("\n=== 聊天服务器已启动 ===");
            System.out.println("* 监听端口: " + state.getPort());
            System.out.println("* 按 Ctrl+C 关闭服务器");
            System.out.println("=====================\n");

            // 创建优雅关闭的Future
            shutdownFuture.set(new CompletableFuture<>());

            while (state.isRunning()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (!state.isRunning()) {
                        clientSocket.close();
                        break;
                    }
                    System.out.println("新的客户端连接：" + clientSocket.getRemoteSocketAddress());

                    // 为新客户端创建一个处理器并在线程池中执行
                    ClientHandler clientHandler = new ClientHandler(clientSocket, state, messageProcessor);
                    state.getExecutorService().execute(clientHandler);
                } catch (IOException e) {
                    if (state.isRunning()) {
                        log.error("接受客户端连接时发生错误: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("服务器启动失败: {}", e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        CompletableFuture<Void> future = shutdownFuture.get();
        if (future != null && !future.isDone()) {
            try {
                log.info("开始服务器关闭流程...");
                
                // 先设置状态为不运行，阻止新的连接
                state.setRunning(false);

                // 通知所有客户端服务器关闭
                log.info("通知所有客户端服务器即将关闭...");
                state.getOnlineUsers().values().forEach(handler -> 
                    handler.sendMessage(Message.createSystemMessage(
                        MessageType.SERVER_SHUTDOWN_NOTIFICATION,
                        "服务器即将关闭..."
                    ))
                );

                // 等待消息发送完成
                Thread.sleep(100);

                // 关闭服务器套接字
                if (state.getServerSocket() != null && !state.getServerSocket().isClosed()) {
                    log.info("关闭服务器套接字...");
                    state.getServerSocket().close();
                }

                // 关闭服务器状态（这会关闭所有客户端连接和线程池）
                state.shutdown();
                
                log.info("服务器关闭完成");
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
                log.error("服务器关闭过程中发生错误: {}", e.getMessage());
            }
        }
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
        
        // 添加关闭钩子，确保在Ctrl+C时正确关闭服务器
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭服务器...");
            server.shutdown();
            try {
                // 等待关闭完成
                CompletableFuture<Void> future = server.shutdownFuture.get();
                if (future != null) {
                    future.get();
                }
            } catch (Exception e) {
                System.err.println("等待服务器关闭时发生错误: " + e.getMessage());
            }
        }));
        
        server.start();
    }
}