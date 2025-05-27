package com.example.chat.client.gui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * 弹窗提示工具类
 */
public class AlertUtil {
    private static final String FONT_FAMILY = "Noto Sans SC";
    
    /**
     * 显示错误提示弹窗
     * @param owner 父窗口
     * @param title 标题
     * @param message 错误信息
     */
    public static void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置字体
        alert.getDialogPane().setStyle("-fx-font-family: '" + FONT_FAMILY + "';");
        
        // 只保留确定按钮
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.OK);
        
        alert.showAndWait();
    }
    
    /**
     * 显示错误提示弹窗（默认标题）
     * @param owner 父窗口
     * @param message 错误信息
     */
    public static void showError(Window owner, String message) {
        showError(owner, "错误", message);
    }
    
    /**
     * 显示信息提示弹窗
     * @param owner 父窗口
     * @param title 标题
     * @param message 信息内容
     */
    public static void showInfo(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置字体
        alert.getDialogPane().setStyle("-fx-font-family: '" + FONT_FAMILY + "';");
        
        alert.showAndWait();
    }
    
    /**
     * 显示警告提示弹窗
     * @param owner 父窗口
     * @param title 标题
     * @param message 警告信息
     */
    public static void showWarning(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        // 设置字体
        alert.getDialogPane().setStyle("-fx-font-family: '" + FONT_FAMILY + "';");
        
        alert.showAndWait();
    }
}