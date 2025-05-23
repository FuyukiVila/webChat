package com.example.chat.client;

import com.example.chat.common.Message;
import com.example.chat.common.MessageType;

/**
 * 消息显示器，负责格式化并显示各种类型的消息
 */
public class MessageDisplay {

    /**
     * 显示消息
     */
    public void display(Message message, String currentUser) {
        System.out.println(MessageFormatter.formatMessage(message, currentUser));
    }

    /**
     * 显示错误消息（红色）
     */
    public void displayError(String content) {
        Message message = Message.builder()
                .type(MessageType.LOCAL_ERROR)
                .content(content)
                .build();
        display(message, null);
    }

    /**
     * 显示提示消息（青色）
     */
    public void displayHint(String content) {
        Message message = Message.builder()
                .type(MessageType.LOCAL_HINT)
                .content(content)
                .build();
        display(message, null);
    }

    /**
     * 显示普通信息（蓝色）
     */
    public void displayInfo(String content) {
        Message message = Message.builder()
                .type(MessageType.LOCAL_INFO)
                .content(content)
                .build();
        display(message, null);
    }

    /**
     * 显示帮助信息
     */
    public void displayHelp() {
        StringBuilder help = new StringBuilder()
                .append("\n=== 聊天室命令说明 ===\n")
                .append("/help                           - 显示此帮助信息\n")
                .append("/clear                          - 清除屏幕\n")
                .append("/exit                           - 退出聊天室\n")
                .append("/list                           - 查看在线用户\n")
                .append("/rooms                          - 查看可用聊天室\n")
                .append("/create-room room-name <密码>    - 创建新聊天室（密码可选）\n")
                .append("/join room-name <密码>           - 加入聊天室（密码可选）\n")
                .append("/passwd room-name <新密码>       - 修改房间密码（仅房主可用，空密码则取消密码）\n")
                .append("/leave                          - 离开当前聊天室\n")
                .append("/room-info                      - 显示当前房间信息和成员列表\n")
                .append("/pm <用户名> <消息>               - 发送私聊消息\n")
                .append("直接输入消息                      - 在当前聊天室发言\n")
                .append("==================\n");

        displayInfo(help.toString());
    }
}