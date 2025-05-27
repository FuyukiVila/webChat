package com.example.chat.client.gui.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;

/**
 * 修改房间密码对话框
 */
public class ChangePasswordDialog extends Dialog<String> {
    private static final String FONT_FAMILY = "Noto Sans SC";
    
    private final String roomName;
    private PasswordField newPasswordField;
    private Label errorLabel;
    
    public ChangePasswordDialog(String roomName) {
        this.roomName = roomName;
        initializeDialog();
        createContent();
        setupValidation();
    }
    
    /**
     * 初始化对话框
     */
    private void initializeDialog() {
        setTitle("修改房间密码");
        setHeaderText("修改房间 \"" + roomName + "\" 的密码");
        initModality(Modality.APPLICATION_MODAL);
        
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String password = newPasswordField.getText();
                return password.isEmpty() ? null : password; // 空密码返回 null
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
        
        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("新密码 (留空则取消密码保护)");
        newPasswordField.setFont(Font.font(FONT_FAMILY, 14));
        
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setFont(Font.font(FONT_FAMILY, 12));
        
        Label hintLabel = new Label("提示: 只有房间创建者可以修改密码");
        hintLabel.setStyle("-fx-text-fill: #666;");
        hintLabel.setFont(Font.font(FONT_FAMILY, 12));
        
        grid.add(roomLabel, 0, 0, 2, 1);
        grid.add(new Label("新密码:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(errorLabel, 1, 2);
        grid.add(hintLabel, 0, 3, 2, 1);
        
        getDialogPane().setContent(grid);
        
        // 默认焦点设置到密码输入框
        Platform.runLater(() -> newPasswordField.requestFocus());
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
        errorLabel.setText("");
        
        String password = newPasswordField.getText();
        
        // 密码可以为空（表示取消密码保护）
        if (!password.isEmpty() && password.length() > 50) {
            showError("密码长度不能超过50个字符");
            return false;
        }
        
        return true;
    }
    
    /**
     * 显示错误信息
     */
    private void showError(String message) {
        errorLabel.setText(message);
    }
}