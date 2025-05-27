package com.example.chat.client.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

import com.example.chat.client.ClientState;
import com.example.chat.client.MessageHandler;
import com.example.chat.client.gui.util.AlertUtil;
import com.example.chat.common.Message;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 主聊天界面控制器
 */
@Slf4j
public class ChatController {
    private static final String FONT_FAMILY = "Noto Sans SC";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final ClientState clientState;
    private final MessageHandler messageHandler;

    // UI 组件
    private BorderPane root;
    private ListView<String> roomListView;
    private TextFlow chatDisplay;
    private ScrollPane chatScrollPane;
    private TextArea messageInput;
    private Button sendButton;
    private Label currentRoomLabel;

    // 数据
    private final ObservableList<String> roomList = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> messageHistory = FXCollections.observableArrayList();
    private String currentRoom = null;

    // 消息接收线程
    private Thread messageReceiver;
    private volatile boolean running = true;

    public ChatController(ClientState clientState, MessageHandler messageHandler) {
        this.clientState = clientState;
        this.messageHandler = messageHandler;

        initializeUI();
        startMessageReceiver();
        requestRoomList();
        setupMessageHistoryListener();
    }

    /**
     * 获取根节点
     */
    public BorderPane getRoot() {
        return root;
    }

    /**
     * 初始化 UI
     */
    private void initializeUI() {
        root = new BorderPane();

        // 创建菜单栏
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 创建左侧房间列表
        VBox leftPanel = createLeftPanel();
        root.setLeft(leftPanel);

        // 创建右侧聊天区域
        VBox rightPanel = createRightPanel();
        root.setCenter(rightPanel);
    }

    /**
     * 创建菜单栏
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-font-family: '" + FONT_FAMILY + "';");

        Menu roomMenu = new Menu("房间操作");

        MenuItem createRoomItem = new MenuItem("创建房间");
        createRoomItem.setOnAction(e -> showCreateRoomDialog());

        MenuItem changePasswordItem = new MenuItem("修改房间密码");
        changePasswordItem.setOnAction(e -> showChangePasswordDialog());

        roomMenu.getItems().addAll(createRoomItem, changePasswordItem);
        menuBar.getMenus().add(roomMenu);

        return menuBar;
    }

    /**
     * 创建左侧面板（房间列表）
     */
    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200);
        leftPanel.setStyle("-fx-background-color: #f5f5f5;");

        Label roomListLabel = new Label("聊天室列表");
        roomListLabel.setFont(Font.font(FONT_FAMILY, 16));
        roomListLabel.setStyle("-fx-font-weight: bold;");

        roomListView = new ListView<>(roomList);
        roomListView.setStyle("-fx-font-family: '" + FONT_FAMILY + "';");
        roomListView.setCellFactory(listView -> new RoomListCell());
        roomListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedRoom = roomListView.getSelectionModel().getSelectedItem();
                if (selectedRoom != null && !selectedRoom.equals(currentRoom)) {
                    joinRoom(selectedRoom);
                }
            }
        });

        VBox.setVgrow(roomListView, Priority.ALWAYS);

        leftPanel.getChildren().addAll(roomListLabel, roomListView);
        return leftPanel;
    }

    /**
     * 创建右侧面板（聊天区域）
     */
    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));

        // 当前房间标签
        currentRoomLabel = new Label("请选择一个聊天室");
        currentRoomLabel.setFont(Font.font(FONT_FAMILY, 16));
        currentRoomLabel.setStyle("-fx-font-weight: bold;");

        // 聊天显示区域
        chatDisplay = new TextFlow();
        chatDisplay.setPadding(new Insets(10));
        chatDisplay.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1px;");

        chatScrollPane = new ScrollPane(chatDisplay);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // 消息输入区域
        HBox inputArea = createInputArea();

        rightPanel.getChildren().addAll(currentRoomLabel, chatScrollPane, inputArea);
        return rightPanel;
    }

    /**
     * 创建输入区域
     */
    private HBox createInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setAlignment(Pos.CENTER);

        messageInput = new TextArea();
        messageInput.setPromptText("输入消息...");
        messageInput.setPrefRowCount(3);
        messageInput.setMaxHeight(80);
        messageInput.setFont(Font.font(FONT_FAMILY, 14));
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && !event.isShiftDown()) {
                event.consume();
                sendMessage();
            }
        });
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        sendButton = new Button("发送");
        sendButton.setFont(Font.font(FONT_FAMILY, 14));
        sendButton.setPrefHeight(50);
        sendButton.setOnAction(e -> sendMessage());
        sendButton.setDisable(true);

        inputArea.getChildren().addAll(messageInput, sendButton);
        return inputArea;
    }

    /**
     * 自定义房间列表单元格
     */
    private class RoomListCell extends ListCell<String> {
        @Override
        protected void updateItem(String room, boolean empty) {
            super.updateItem(room, empty);

            if (empty || room == null) {
                setText(null);
                setStyle("");
            } else {
                setText(room);
                setFont(Font.font(FONT_FAMILY, 14));

                if (room.equals(currentRoom)) {
                    setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
                } else {
                    setStyle("-fx-background-color: white; -fx-text-fill: black;");
                }
            }
        }
    }

    /**
     * 聊天消息类
     */
    private static class ChatMessage {
        public final String sender;
        public final String content;
        public final Date timestamp;
        public final boolean isSystem;

        public ChatMessage(String sender, String content, Date timestamp, boolean isSystem) {
            this.sender = sender;
            this.content = content;
            this.timestamp = timestamp;
            this.isSystem = isSystem;
        }
    }

    /**
     * 显示创建房间对话框
     */
    private void showCreateRoomDialog() {
        CreateRoomDialog dialog = new CreateRoomDialog();
        Optional<CreateRoomDialog.RoomInfo> result = dialog.showAndWait();

        result.ifPresent(roomInfo -> {
            try {
                // 检查是否试图创建当前已在的房间
                if (currentRoom != null && currentRoom.equals(roomInfo.roomName)) {
                    showErrorAlert("您已经在房间 \"" + roomInfo.roomName + "\" 中，无法重复创建");
                    return;
                }

                // 如果已经在某个聊天室中，则先离开当前聊天室
                if (currentRoom != null) {
                    leaveCurrentRoom();
                }

                Message createRequest = Message.createCreateRoomRequest(
                        roomInfo.roomName, clientState.getUsername(), roomInfo.password);
                messageHandler.sendMessage(createRequest);
            } catch (Exception e) {
                log.error("创建房间失败", e);
                showErrorAlert("创建房间失败: " + e.getMessage());
            }
        });
    }

    /**
     * 显示修改密码对话框
     */
    private void showChangePasswordDialog() {
        if (currentRoom == null) {
            showErrorAlert("请先选择一个房间");
            return;
        }

        ChangePasswordDialog dialog = new ChangePasswordDialog(currentRoom);
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newPassword -> {
            try {
                Message changeRequest = Message.createChangePasswordRequest(
                        currentRoom, clientState.getUsername(), newPassword);
                messageHandler.sendMessage(changeRequest);
            } catch (Exception e) {
                log.error("修改密码失败", e);
                showErrorAlert("修改密码失败: " + e.getMessage());
            }
        });
    }

    /**
     * 加入房间
     */
    private void joinRoom(String roomName) {
        // 检查是否试图加入当前已在的房间
        if (currentRoom != null && currentRoom.equals(roomName)) {
            showErrorAlert("您已经在房间 \"" + roomName + "\" 中，无法重复加入");
            return;
        }

        JoinRoomDialog dialog = new JoinRoomDialog(roomName);
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(password -> {
            try {
                // 如果已经在某个聊天室中，则先离开当前聊天室
                if (currentRoom != null) {
                    leaveCurrentRoom();
                }

                Message joinRequest = Message.createJoinRoomRequest(roomName, clientState.getUsername(), password);
                messageHandler.sendMessage(joinRequest);
            } catch (Exception e) {
                log.error("加入房间失败", e);
                showErrorAlert("加入房间失败: " + e.getMessage());
            }
        });
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        if (currentRoom == null) {
            showErrorAlert("请先选择一个房间");
            return;
        }

        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            Message message = Message.createRoomMessage(content, clientState.getUsername(), currentRoom);
            messageHandler.sendMessage(message);
            messageInput.clear();
        } catch (Exception e) {
            log.error("发送消息失败", e);
            showErrorAlert("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 请求房间列表
     */
    private void requestRoomList() {
        try {
            Message request = Message.createListRoomsRequest(clientState.getUsername());
            messageHandler.sendMessage(request);
        } catch (Exception e) {
            log.error("请求房间列表失败", e);
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageReceiver = new Thread(() -> {
            try {
                while (running && clientState.isRunning()) {
                    Message message = (Message) clientState.getInput().readObject();
                    Platform.runLater(() -> handleMessage(message));
                }
            } catch (Exception e) {
                if (running) {
                    log.error("接收消息失败", e);
                }
            }
        });
        messageReceiver.setDaemon(true);
        messageReceiver.start();
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LIST_ROOMS_RESPONSE:
                handleRoomListResponse(message);
                break;
            case JOIN_ROOM_SUCCESS:
                handleJoinRoomSuccess(message);
                break;
            case JOIN_ROOM_FAILURE:
                showErrorAlert("加入房间失败: " + message.getContent());
                break;
            case CREATE_ROOM_SUCCESS:
                // 房间创建成功的提示只在当前客户端显示
                showInfoAlert(message.getContent());
                break;
            case ROOM_CREATED_NOTIFICATION:
            case ROOM_DESTROYED_NOTIFICATION:
                requestRoomList();
                break;
            case CREATE_ROOM_FAILURE:
                showErrorAlert("创建房间失败: " + message.getContent());
                break;
            case ROOM_MESSAGE_BROADCAST:
                handleRoomMessage(message);
                break;
            case USER_JOINED_ROOM_NOTIFICATION:
                handleUserJoinedRoom(message);
                break;
            case USER_LEFT_ROOM_NOTIFICATION:
                handleUserLeftRoom(message);
                break;
            case LEAVE_ROOM_SUCCESS:
                handleLeaveRoomSuccess(message);
                break;
            case ROOM_HISTORY_RESPONSE:
                handleRoomHistoryResponse(message);
                break;
            case CHANGE_ROOM_PASSWORD_SUCCESS:
                showInfoAlert("密码修改成功");
                break;
            case CHANGE_ROOM_PASSWORD_FAILURE:
                showErrorAlert("密码修改失败: " + message.getContent());
                break;
            default:
                log.debug("收到未处理的消息类型: {}", message.getType());
                break;
        }
    }

    /**
     * 处理房间列表响应
     */
    @SuppressWarnings("unchecked")
    private void handleRoomListResponse(Message message) {
        List<String> rooms = (List<String>) message.getData();
        Platform.runLater(() -> {
            roomList.clear();
            if (rooms != null) {
                roomList.addAll(rooms);
            }
        });
    }

    /**
     * 处理加入房间成功
     */
    private void handleJoinRoomSuccess(Message message) {
        currentRoom = message.getRoomName();
        clientState.setCurrentRoom(Optional.of(currentRoom));

        Platform.runLater(() -> {
            currentRoomLabel.setText("当前房间: " + currentRoom);
            sendButton.setDisable(false);
            roomListView.refresh(); // 刷新房间列表样式

            // 清空聊天显示和消息历史
            chatDisplay.getChildren().clear();
            messageHistory.clear();
            // 消息会通过监听器自动显示
        });
    }

    /**
     * 处理房间消息
     */
    private void handleRoomMessage(Message message) {
        if (message.getRoomName().equals(currentRoom)) {
            ChatMessage chatMessage = new ChatMessage(
                    message.getSender(), message.getContent(), message.getTimestamp(), false);

            // 保存消息
            messageHistory.add(chatMessage);
        }
    }

    /**
     * 处理用户加入房间通知
     */
    private void handleUserJoinedRoom(Message message) {
        if (message.getRoomName().equals(currentRoom)) {
            ChatMessage systemMessage = new ChatMessage(
                    "系统", message.getContent(), message.getTimestamp(), true);

            messageHistory.add(systemMessage);
        }
    }

    /**
     * 处理用户离开房间通知
     */
    private void handleUserLeftRoom(Message message) {
        if (message.getRoomName().equals(currentRoom)) {
            ChatMessage systemMessage = new ChatMessage(
                    "系统", message.getContent(), message.getTimestamp(), true);

            messageHistory.add(systemMessage);
        }
    }

    /**
     * 处理离开房间成功
     */
    private void handleLeaveRoomSuccess(Message message) {
        currentRoom = null;
        clientState.setCurrentRoom(Optional.empty());

        Platform.runLater(() -> {
            currentRoomLabel.setText("请选择一个聊天室");
            sendButton.setDisable(true);
            roomListView.refresh(); // 刷新房间列表样式

            // 清空聊天显示和消息历史
            chatDisplay.getChildren().clear();
            messageHistory.clear();
        });
    }

    /**
     * 处理房间历史消息响应
     */
    @SuppressWarnings("unchecked")
    private void handleRoomHistoryResponse(Message message) {
        if (message.getRoomName().equals(currentRoom)) {
            List<Message> history = (List<Message>) message.getData();
            if (history != null && !history.isEmpty()) {
                // 将历史消息转换为ChatMessage并保存
                List<ChatMessage> chatMessages = new ArrayList<>();
                for (Message msg : history) {
                    ChatMessage chatMessage = new ChatMessage(
                            msg.getSender(), msg.getContent(), msg.getTimestamp(), false);
                    chatMessages.add(chatMessage);
                }

                // 保存到房间消息列表，监听器会自动显示消息
                messageHistory.addAll(chatMessages);
            }
        }
    }

    /**
     * 将消息添加到显示区域
     */
    private void setupMessageHistoryListener() {
        messageHistory.addListener((ListChangeListener.Change<? extends ChatMessage> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (ChatMessage msg : c.getAddedSubList()) {
                        Platform.runLater(() -> addSingleMessageToDisplay(msg));
                    }
                }
            }
        });
    }

    private void addSingleMessageToDisplay(ChatMessage message) {
        Text timeText = new Text("[" + TIME_FORMAT.format(message.timestamp) + "] ");
        timeText.setStyle("-fx-fill: #666; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 12px;");

        Text senderText = new Text(message.sender + ": ");
        if (message.isSystem) {
            senderText.setStyle("-fx-fill: #ff6b35; -fx-font-weight: bold; -fx-font-family: '" + FONT_FAMILY
                    + "'; -fx-font-size: 14px;");
        } else {
            senderText.setStyle("-fx-fill: #333; -fx-font-weight: bold; -fx-font-family: '" + FONT_FAMILY
                    + "'; -fx-font-size: 14px;");
        }

        Text contentText = new Text(message.content + "\n");
        contentText.setStyle("-fx-fill: black; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 14px;");

        chatDisplay.getChildren().addAll(timeText, senderText, contentText);

        // 滚动到底部
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    /**
     * 显示错误对话框
     */
    private void showErrorAlert(String message) {
        AlertUtil.showError(root.getScene().getWindow(), "错误", message);
    }

    /**
     * 显示信息对话框
     */
    private void showInfoAlert(String message) {
        AlertUtil.showInfo(root.getScene().getWindow(), "信息", message);
    }

    /**
     * 离开当前房间
     */
    private void leaveCurrentRoom() {
        if (currentRoom != null) {
            try {
                Message leaveRequest = Message.createLeaveRoomRequest(currentRoom, clientState.getUsername());
                messageHandler.sendMessage(leaveRequest);
            } catch (Exception e) {
                log.error("离开房间失败", e);
            }
        }
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        running = false;
        if (messageReceiver != null) {
            messageReceiver.interrupt();
        }
        clientState.close();
    }
}