package com.example.chat.client.gui.controller;

import com.example.chat.client.gui.util.AlertUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;

import java.util.Optional;

/**
 * 创建房间对话框
 */
public class CreateRoomDialog extends Dialog<CreateRoomDialog.RoomInfo> {
    private static final String FONT_FAMILY = "Noto Sans SC";
    
    private TextField roomNameField;
    private PasswordField passwordField;
    
    public CreateRoomDialog() {
        initializeDialog();
        createContent();
        setupValidation();
    }
    
    /**
     * 房间信息类
     */
    public static class RoomInfo {
        public final String roomName;
        public final String password;
        
        public RoomInfo(String roomName, String password) {
            this.roomName = roomName;
            this.password = password;
        }
    }
    
    /**
     * 初始化对话框
     */
    private void initializeDialog() {
        setTitle("创建房间");
        setHeaderText("请输入房间信息");
        initModality(Modality.APPLICATION_MODAL);
        
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String roomName = roomNameField.getText().trim();
                String password = passwordField.getText();
                return new RoomInfo(roomName, password.isEmpty() ? null : password);
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
        
        roomNameField = new TextField();
        roomNameField.setPromptText("房间名称 (必填)");
        roomNameField.setFont(Font.font(FONT_FAMILY, 14));
        
        passwordField = new PasswordField();
        passwordField.setPromptText("房间密码 (可选，字母数字下划线)");
        passwordField.setFont(Font.font(FONT_FAMILY, 14));
        
        grid.add(new Label("房间名称:"), 0, 0);
        grid.add(roomNameField, 1, 0);
        grid.add(new Label("房间密码:"), 0, 1);
        grid.add(passwordField, 1, 1);
        
        getDialogPane().setContent(grid);
        
        // 默认焦点设置到房间名输入框
        Platform.runLater(() -> roomNameField.requestFocus());
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
        String roomName = roomNameField.getText().trim();
        String password = passwordField.getText();
        
        if (roomName.isEmpty()) {
            showError("房间名称不能为空");
            return false;
        }
        
        if (!isValidName(roomName)) {
            showError("房间名称只能包含大小写字母、数字和下划线");
            return false;
        }
        
        if (roomName.length() > 20) {
            showError("房间名称不能超过20个字符");
            return false;
        }
        
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
     * 验证名称格式（用户名或房间名）
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