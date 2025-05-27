package com.example.chat.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import com.example.chat.client.ClientState;
import com.example.chat.client.MessageHandler;
import com.example.chat.client.gui.controller.ChatController;
import com.example.chat.client.gui.controller.LoginController;

/**
 * JavaFX 聊天客户端主应用程序
 */
@Slf4j
public class ChatApplication extends Application {
    private static final String FONT_FAMILY = "Noto Sans SC";
    private static final double FONT_SIZE = 14;
    
    private Stage primaryStage;
    private ChatController chatController;
    private LoginController loginController;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // 设置应用程序标题
        primaryStage.setTitle("聊天客户端");
        
        // 设置默认字体
        setDefaultFont();
        
        // 显示登录对话框
        showLoginDialog();
    }
    
    /**
     * 设置默认字体
     */
    private void setDefaultFont() {
        Font defaultFont = Font.font(FONT_FAMILY, FONT_SIZE);
        // 这里可以通过 CSS 设置全局字体
    }
    
    /**
     * 显示登录对话框
     */
    private void showLoginDialog() {
        loginController = new LoginController(primaryStage, this::onLoginSuccess);
        loginController.show();
    }
    
    /**
     * 登录成功后的回调
     */
    private void onLoginSuccess(ClientState clientState, MessageHandler messageHandler) {
        Platform.runLater(() -> {
            try {
                // 创建主聊天界面
                chatController = new ChatController(clientState, messageHandler);
                Scene scene = new Scene(chatController.getRoot(), 1000, 700);
                
                // 尝试加载 CSS 文件，如果不存在则跳过
                try {
                    var cssResource = getClass().getResource("/css/chat.css");
                    if (cssResource != null) {
                        scene.getStylesheets().add(cssResource.toExternalForm());
                    }
                } catch (Exception e) {
                    log.warn("无法加载 CSS 文件: {}", e.getMessage());
                }
                
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(800);
                primaryStage.setMinHeight(600);
                primaryStage.show();
                
                // 设置关闭事件
                primaryStage.setOnCloseRequest(event -> {
                    chatController.cleanup();
                    Platform.exit();
                });
                
            } catch (Exception e) {
                log.error("创建主界面失败", e);
                showErrorAlert("创建主界面失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}