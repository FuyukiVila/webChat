package com.example.chat.client.gui.controller;

import com.example.chat.client.gui.util.AlertUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;

/**
 * 加入房间对话框
 */
public class JoinRoomDialog extends Dialog<String> {
    private static final String FONT_FAMILY = "Noto Sans SC";
    
    private final String roomName;
    private PasswordField passwordField;
    
    public JoinRoomDialog(String roomName) {
        this.roomName = roomName;
        initializeDialog();
        createContent();
        setupValidation();
    }
    
    /**
     * 初始化对话框
     */
    private void initializeDialog() {
        setTitle("加入房间");
        setHeaderText("加入房间 \"" + roomName + "\"");
        initModality(Modality.APPLICATION_MODAL);
        
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String password = passwordField.getText();
                return password.isEmpty() ? "" : password; // 返回密码，空密码返回空字符串
            }
            return null;
        });
    }
    
    /**
     * 创建对话框内容
     */
    private void createContent() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        Label roomLabel = new Label("房间名称: " + roomName);
        roomLabel.setFont(Font.font(FONT_FAMILY, 14));
        roomLabel.setStyle("-fx-font-weight: bold;");
        
        passwordField = new PasswordField();
        passwordField.setPromptText("房间密码 (字母数字下划线)");
        passwordField.setFont(Font.font(FONT_FAMILY, 14));
        
        Label hintLabel = new Label("提示: 如果房间没有设置密码，请留空");
        hintLabel.setStyle("-fx-text-fill: #666;");
        hintLabel.setFont(Font.font(FONT_FAMILY, 12));
        
        grid.add(roomLabel, 0, 0, 2, 1);
        grid.add(new Label("密码:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(hintLabel, 0, 2, 2, 1);
        
        getDialogPane().setContent(grid);
        
        // 默认焦点设置到密码输入框
        Platform.runLater(() -> passwordField.requestFocus());
    }
    
    /**
     * 设置验证
     */
    private void setupValidation() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
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
        String password = passwordField.getText();
        
        // 验证密码格式（如果设置了密码）
        if (!password.isEmpty() && !isValidName(password)) {
            showError("房间密码只能包含大小写字母、数字和下划线");
            return false;
        }
        
        if (password.length() > 20) {
            showError("房间密码不能超过20个字符");
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证名称格式（用户名或房间名或密码）
     * 只允许使用大小写字母、数字和下划线
     */
    private boolean isValidName(String name) {
        return name != null && !name.isEmpty() && name.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        AlertUtil.showError(getDialogPane().getScene().getWindow(), "输入错误", message);
    }
}