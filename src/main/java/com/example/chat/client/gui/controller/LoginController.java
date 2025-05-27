package com.example.chat.client.gui.controller;

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
    private Label errorLabel;
    
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
        
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setFont(Font.font(FONT_FAMILY, 12));
        
        grid.add(new Label("用户名:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("服务器:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("端口:"), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(errorLabel, 1, 3);
        
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
        errorLabel.setText("");
        
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
        
        // 创建客户端状态
        clientState = new ClientState(host, port);
        messageHandler = new MessageHandler(clientState);
        
        try {
            // 连接服务器
            connectToServer();
            
            // 发送登录请求
            if (sendLoginRequest(username)) {
                // 登录成功，调用回调
                loginSuccessCallback.accept(clientState, messageHandler);
            } else {
                // 登录失败，重新显示对话框
                Platform.runLater(this::show);
            }
            
        } catch (Exception e) {
            log.error("登录过程中发生错误", e);
            showError("连接服务器失败: " + e.getMessage());
            Platform.runLater(this::show);
        }
    }
    
    /**
     * 连接服务器
     */
    private void connectToServer() throws IOException {
        Socket socket = new Socket(clientState.getHost(), clientState.getPort());
        clientState.setSocket(socket);
        clientState.setOutput(new ObjectOutputStream(socket.getOutputStream()));
        clientState.setInput(new ObjectInputStream(socket.getInputStream()));
        clientState.setRunning(true);
    }
    
    /**
     * 发送登录请求
     */
    private boolean sendLoginRequest(String username) {
        try {
            messageHandler.sendMessage(Message.createLoginRequest(username));
            
            while (clientState.isRunning()) {
                Message response = (Message) clientState.getInput().readObject();
                
                switch (response.getType()) {
                    case LOGIN_SUCCESS:
                        clientState.setUsername(username);
                        return true;
                        
                    case LOGIN_FAILURE_USERNAME_TAKEN:
                        showError("用户名已被占用，请选择其他用户名");
                        return false;
                        
                    case ERROR_MESSAGE:
                        showError("登录失败: " + response.getContent());
                        return false;
                        
                    default:
                        log.warn("登录过程中收到意外的消息类型：{}", response.getType());
                        break;
                }
            }
            
        } catch (IOException | ClassNotFoundException e) {
            log.error("登录请求失败", e);
            showError("登录请求失败: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Platform.runLater(() -> errorLabel.setText(message));
    }
}