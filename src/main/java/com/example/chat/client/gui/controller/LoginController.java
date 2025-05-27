package com.example.chat.client.gui.controller;

import com.example.chat.client.gui.util.AlertUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import com.example.chat.client.ClientState;
import com.example.chat.client.MessageHandler;
import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * 登录控制器
 */
@Slf4j
public class LoginController {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String FONT_FAMILY = "Noto Sans SC";
    
    private final Stage parentStage;
    private final BiConsumer<ClientState, MessageHandler> loginSuccessCallback;
    
    private Dialog<ButtonType> loginDialog;
    private TextField usernameField;
    private TextField hostField;
    private TextField portField;
    
    private ClientState clientState;
    private MessageHandler messageHandler;
    
    public LoginController(Stage parentStage, BiConsumer<ClientState, MessageHandler> loginSuccessCallback) {
        this.parentStage = parentStage;
        this.loginSuccessCallback = loginSuccessCallback;
    }
    
    /**
     * 显示登录对话框
     */
    public void show() {
        createLoginDialog();
        
        Optional<ButtonType> result = loginDialog.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            handleLogin();
        } else {
            // 用户取消登录，退出应用程序
            Platform.exit();
        }
    }
    
    /**
     * 创建登录对话框
     */
    private void createLoginDialog() {
        loginDialog = new Dialog<>();
        loginDialog.setTitle("登录聊天室");
        loginDialog.setHeaderText("请输入您的登录信息");
        // 不设置 owner，避免在 Scene 未设置时出错
        loginDialog.initModality(Modality.APPLICATION_MODAL);
        
        // 设置按钮
        loginDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // 创建输入表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        usernameField = new TextField();
        usernameField.setPromptText("用户名 (字母、数字、下划线)");
        usernameField.setFont(Font.font(FONT_FAMILY, 14));
        
        hostField = new TextField(DEFAULT_HOST);
        hostField.setPromptText("服务器地址");
        hostField.setFont(Font.font(FONT_FAMILY, 14));
        
        portField = new TextField(String.valueOf(DEFAULT_PORT));
        portField.setPromptText("端口号");
        portField.setFont(Font.font(FONT_FAMILY, 14));
        
        grid.add(new Label("用户名:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("服务器:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("端口:"), 0, 2);
        grid.add(portField, 1, 2);
        
        loginDialog.getDialogPane().setContent(grid);
        
        // 默认焦点设置到用户名输入框
        Platform.runLater(() -> usernameField.requestFocus());
        
        // 验证输入
        Button okButton = (Button) loginDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateInput()) {
                event.consume(); // 阻止对话框关闭
            }
        });
    }
    
    /**
     * 验证输入
     */
    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();
        
        // 验证用户名
        if (username.isEmpty()) {
            showError("用户名不能为空");
            return false;
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("用户名只能包含大小写字母、数字和下划线");
            return false;
        }
        
        // 验证服务器地址
        if (host.isEmpty()) {
            showError("服务器地址不能为空");
            return false;
        }
        
        // 验证端口号
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showError("端口号必须在 1-65535 之间");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("端口号必须是有效的数字");
            return false;
        }
        
        return true;
    }
    
    /**
     * 处理登录
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());

        clientState = new ClientState(host, port);
        messageHandler = new MessageHandler(clientState);

        try {
            connectToServer();
            String loginError = sendLoginRequest(username);

            if (loginError == null) { // Success
                loginSuccessCallback.accept(clientState, messageHandler);
            } else { // Failure
                closeClientResources();
                showError(loginError); // showError is now blocking
                Platform.runLater(this::show); // Re-show login dialog
            }
        } catch (IOException e) { // Connection failed in connectToServer
            log.error("连接服务器失败", e);
            closeClientResources();
            showError("连接服务器失败: " + e.getMessage());
            Platform.runLater(this::show);
        } catch (Exception e) { // Other unexpected errors during login setup
            log.error("登录过程中发生未知错误", e);
            closeClientResources();
            showError("登录时发生未知错误: " + e.getMessage());
            Platform.runLater(this::show);
        }
    }

    /**
     * 关闭客户端资源
     */
    private void closeClientResources() {
        if (clientState != null) {
            clientState.setRunning(false); // Signal message reading loops to stop
            try {
                if (clientState.getSocket() != null && !clientState.getSocket().isClosed()) {
                    clientState.getSocket().close();
                }
            } catch (IOException e) {
                log.warn("关闭socket时出错: {}", e.getMessage());
            }
            // Streams are typically closed when the socket is closed.
            // Explicitly closing them can sometimes cause issues if the socket is already closed.
            clientState.setSocket(null);
            clientState.setInput(null);
            clientState.setOutput(null);
        }
    }

    /**
     * 连接服务器
     */
    private void connectToServer() throws IOException {
        Socket socket = new Socket(clientState.getHost(), clientState.getPort());
        clientState.setSocket(socket);
        // Ensure output stream is flushed immediately for ObjectInputStream on the other side
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        clientState.setOutput(oos);
        clientState.setInput(new ObjectInputStream(socket.getInputStream()));
        clientState.setRunning(true);
    }

    /**
     * 发送登录请求.
     * @return null on success, error message string on failure.
     */
    private String sendLoginRequest(String username) {
        try {
            messageHandler.sendMessage(Message.createLoginRequest(username));

            while (clientState.isRunning()) { // Loop should ideally have a timeout or break condition
                Message response = (Message) clientState.getInput().readObject(); // This blocks

                switch (response.getType()) {
                    case LOGIN_SUCCESS:
                        clientState.setUsername(username);
                        return null; // Success

                    case LOGIN_FAILURE_USERNAME_TAKEN:
                        return "用户名已被占用，请选择其他用户名";

                    case ERROR_MESSAGE:
                        return "登录失败: " + response.getContent();

                    default:
                        log.warn("登录过程中收到意外的消息类型：{}", response.getType());
                        return "收到意外的服务器响应: " + response.getType();
                }
            }
            return "登录过程被中断或连接丢失";

        } catch (IOException | ClassNotFoundException e) {
            log.error("登录请求失败", e);
            return "登录请求通信失败: " + e.getMessage();
        }
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Window alertOwner = null;
        if (parentStage != null && parentStage.isShowing() && parentStage.getScene() != null) {
            alertOwner = parentStage;
        }
        AlertUtil.showError(alertOwner, "登录错误", message);
    }
}
