package com.example.chat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表示一个聊天室
 * 该类负责管理单个聊天室的状态，包括成员列表和基本属性
 * 设计为线程安全的，因为会被多个 ClientHandler 并发访问
 */
public class ChatRoom {
    private final String name;               // 聊天室名称（唯一标识）
    private final String creator;            // 创建者用户名
    private final long creationTime;         // 创建时间
    private final Set<String> members;       // 当前成员列表（用户名）

    /**
     * 创建一个新的聊天室
     * @param name 聊天室名称
     * @param creator 创建者用户名
     */
    public ChatRoom(String name, String creator) {
        this.name = name;
        this.creator = creator;
        this.creationTime = System.currentTimeMillis();
        // 使用 ConcurrentHashMap 的 newKeySet 来创建线程安全的 Set
        this.members = ConcurrentHashMap.newKeySet();
    }

    /**
     * 添加成员到聊天室
     * @param username 要添加的用户名
     * @return true 如果添加成功，false 如果用户已在房间中
     */
    public boolean addMember(String username) {
        return members.add(username);
    }

    /**
     * 从聊天室移除成员
     * @param username 要移除的用户名
     * @return true 如果移除成功，false 如果用户不在房间中
     */
    public boolean removeMember(String username) {
        return members.remove(username);
    }

    /**
     * 判断用户是否在聊天室中
     * @param username 要检查的用户名
     * @return true 如果用户在房间中
     */
    public boolean hasMember(String username) {
        return members.contains(username);
    }

    /**
     * 获取聊天室当前成员数
     * @return 成员数量
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * 检查聊天室是否为空
     * @return true 如果没有成员
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * 获取所有成员的用户名集合（副本）
     * @return 成员用户名集合的副本
     */
    public Set<String> getMembers() {
        return Set.copyOf(members);
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String toString() {
        return String.format("ChatRoom{name='%s', creator='%s', members=%d}", 
            name, creator, members.size());
    }
}