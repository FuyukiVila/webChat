package com.example.chat.common;

/**
 * 定义所有可能的消息类型
 */
public enum MessageType {
    // 登录相关
    LOGIN_REQUEST, // C->S: 客户端请求登录，sender=欲使用的昵称
    LOGIN_SUCCESS, // S->C: 服务器通知登录成功
    LOGIN_FAILURE_USERNAME_TAKEN, // S->C: 服务器通知登录失败，昵称已被占用

    // 私聊消息相关
    PRIVATE_MESSAGE_REQUEST, // C->S: 客户端发送私聊消息请求
    PRIVATE_MESSAGE_DELIVERY, // S->C: 服务器向目标客户端投递私聊消息

    // 用户管理相关
    USER_LIST_REQUEST, // C->S: 客户端请求在线用户列表
    USER_LIST_RESPONSE, // S->C: 服务器响应用户列表请求
    USER_JOINED_NOTIFICATION, // S->C: 服务器通知有新用户加入
    USER_LEFT_NOTIFICATION, // S->C: 服务器通知有用户离开

    // 登出相关
    LOGOUT_REQUEST, // C->S: 客户端请求登出
    LOGOUT_CONFIRMATION, // S->C: 服务器确认登出

    // 错误和系统消息
    ERROR_MESSAGE, // S->C: 服务器发送错误信息
    SERVER_SHUTDOWN_NOTIFICATION, // S->C: 服务器通知即将关闭

    // 聊天室相关消息类型
    CREATE_ROOM_REQUEST, // C->S: 客户端请求创建聊天室
    CREATE_ROOM_SUCCESS, // S->C: 服务器通知聊天室创建成功
    CREATE_ROOM_FAILURE, // S->C: 服务器通知聊天室创建失败（如名称已存在）

    JOIN_ROOM_REQUEST, // C->S: 客户端请求加入聊天室
    JOIN_ROOM_SUCCESS, // S->C: 服务器通知成功加入聊天室
    JOIN_ROOM_FAILURE, // S->C: 服务器通知加入聊天室失败（如房间不存在）

    LEAVE_ROOM_REQUEST, // C->S: 客户端请求离开聊天室
    LEAVE_ROOM_SUCCESS, // S->C: 服务器通知成功离开聊天室

    ROOM_MESSAGE_REQUEST, // C->S: 客户端发送聊天室消息请求
    ROOM_MESSAGE_BROADCAST, // S->C: 服务器广播聊天室消息给所有房间成员

    LIST_ROOMS_REQUEST, // C->S: 客户端请求获取可用聊天室列表
    LIST_ROOMS_RESPONSE, // S->C: 服务器响应聊天室列表

    // 聊天室状态变更通知
    USER_JOINED_ROOM_NOTIFICATION, // S->C: 服务器通知房间内有新用户加入
    USER_LEFT_ROOM_NOTIFICATION, // S->C: 服务器通知房间内有用户离开
    ROOM_CREATED_NOTIFICATION, // S->C: 服务器通知聊天室已被创建
    ROOM_DESTROYED_NOTIFICATION, // S->C: 服务器通知聊天室已被销毁（当房间空了之后）

    // 房间信息相关
    ROOM_INFO_REQUEST, // C->S: 客户端请求获取当前房间信息
    ROOM_INFO_RESPONSE, // S->C: 服务器响应房间信息
    ROOM_HISTORY_RESPONSE, // S->C: 服务器发送房间历史消息

    // 房间密码相关
    CHANGE_ROOM_PASSWORD_REQUEST, // C->S: 房主请求修改房间密码
    CHANGE_ROOM_PASSWORD_SUCCESS, // S->C: 服务器通知修改密码成功
    CHANGE_ROOM_PASSWORD_FAILURE, // S->C: 服务器通知修改密码失败（如非房主）

    // 本地消息类型（客户端内部使用）
    LOCAL_ERROR, // 本地错误提示（红色）
    LOCAL_HINT, // 本地操作提示（青色）
    LOCAL_INFO // 本地普通提示（蓝色）
}