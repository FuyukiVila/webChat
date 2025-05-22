package com.example.chat.server;

import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表示一个聊天室
 * 该类负责管理单个聊天室的状态，包括成员列表和基本属性
 * 设计为线程安全的，因为会被多个 ClientHandler 并发访问
 */
@Getter
@ToString(of = { "name", "creator" })
public class ChatRoom {
    private final String name; // 聊天室名称（唯一标识）
    private final String creator; // 创建者用户名
    private final long creationTime; // 创建时间
    private final Set<String> members; // 当前成员列表（用户名）
    private final AtomicInteger memberCount; // 成员计数器，避免频繁计算size

    /**
     * 创建一个新的聊天室
     * 
     * @param name    聊天室名称
     * @param creator 创建者用户名
     */
    public ChatRoom(String name, String creator) {
        this.name = name;
        this.creator = creator;
        this.creationTime = System.currentTimeMillis();
        // 使用 ConcurrentHashMap 的 newKeySet 来创建线程安全的 Set
        this.members = ConcurrentHashMap.newKeySet();
        this.memberCount = new AtomicInteger(0);
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
}