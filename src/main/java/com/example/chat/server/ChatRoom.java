package com.example.chat.server;

import lombok.Getter;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.chat.common.Message;

/**
 * 表示一个聊天室
 * 该类负责管理单个聊天室的状态，包括成员列表和基本属性
 * 设计为线程安全的，因为会被多个 ClientHandler 并发访问
 */
@Getter
public class ChatRoom {
    private final String name; // 聊天室名称（唯一标识）
    private final String creator; // 创建者用户名
    private final long creationTime; // 创建时间
    private String password; // 房间密码，如果为null或空字符串表示无密码
    private final Set<String> members; // 当前成员列表（用户名）
    private final AtomicInteger memberCount; // 成员计数器，避免频繁计算size
    private final List<Message> messageHistory; // 聊天记录
    private static final int MAX_HISTORY_SIZE = 100; // 最大历史消息数量

    /**
     * 创建一个新的聊天室
     * 
     * @param name    聊天室名称
     * @param creator 创建者用户名
     */
    public ChatRoom(String name, String creator, String password) {
        this.name = name;
        this.creator = creator;
        this.password = password;
        this.creationTime = System.currentTimeMillis();
        // 使用 ConcurrentHashMap 的 newKeySet 来创建线程安全的 Set
        this.members = ConcurrentHashMap.newKeySet();
        this.memberCount = new AtomicInteger(0);
        this.messageHistory = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * 添加成员到聊天室
     * 返回true如果添加成功，false如果用户已在房间中
     */
    public boolean addMember(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        if (members.add(username)) {
            memberCount.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * 从聊天室移除成员
     * 返回true如果移除成功，false如果用户不在房间中
     */
    public boolean removeMember(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        if (members.remove(username)) {
            memberCount.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * 判断用户是否在聊天室中
     */
    public boolean hasMember(String username) {
        return username != null && !username.isEmpty() && members.contains(username);
    }

    /**
     * 获取聊天室当前成员数
     * 使用原子计数器，避免频繁计算size
     */
    public int getMemberCount() {
        return memberCount.get();
    }

    /**
     * 检查聊天室是否为空
     */
    public boolean isEmpty() {
        return memberCount.get() == 0;
    }

    /**
     * 获取所有成员的用户名集合的不可变副本
     */
    public Set<String> getMembers() {
        return Set.copyOf(members);
    }

    /**
     * 验证密码是否正确
     * 如果房间没有密码（password为null或空字符串），则始终返回true
     */
    public synchronized boolean validatePassword(String inputPassword) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        return password.equals(inputPassword);
    }

    /**
     * 修改房间密码
     * 只有房主可以修改密码
     * 
     * @param username    请求修改密码的用户
     * @param newPassword 新密码
     * @return true 如果修改成功，false 如果用户不是房主
     */
    public synchronized boolean changePassword(String username, String newPassword) {
        if (!isCreator(username)) {
            return false;
        }
        if (newPassword != null && !newPassword.matches("^[a-zA-Z0-9_]*$")) {
            return false;
        }
        this.password = newPassword;
        return true;
    }

    /**
     * 检查用户是否是房主
     */
    public boolean isCreator(String username) {
        return username != null && username.equals(creator);
    }

    /**
     * 添加一条消息到历史记录
     * 如果历史记录超过最大容量，将移除最旧的消息
     */
    public synchronized void addMessage(Message message) {
        if (messageHistory.size() >= MAX_HISTORY_SIZE) {
            messageHistory.remove(0); // 移除最旧的消息
        }
        messageHistory.add(message);
    }

    /**
     * 获取最近的n条消息历史
     * 如果n大于历史记录数量，则返回所有历史记录
     */
    public List<Message> getRecentMessages(int n) {
        synchronized (messageHistory) {
            int start = Math.max(0, messageHistory.size() - n);
            return new ArrayList<>(messageHistory.subList(start, messageHistory.size()));
        }
    }
}