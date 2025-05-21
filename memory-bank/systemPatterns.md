# 系统模式 (System Patterns)

## 1. 系统架构概述 (System Architecture Overview)
本项目采用经典的客户端-服务器 (Client-Server)架构模型。

*   **服务器 (Server):**
    *   作为中心枢纽，负责接收来自所有客户端的消息。
    *   管理用户连接和会话。
    *   根据消息类型（公共或私人）将消息路由到相应的客户端。
    *   维护在线用户列表。
*   **客户端 (Client):**
    *   连接到服务器。
    *   允许用户输入并发送消息。
    *   接收并显示来自服务器的消息。
    *   通过命令行界面与用户交互。

```mermaid
graph TD
    subgraph 客户端 (Clients)
        C1[客户端 1]
        C2[客户端 2]
        C3[客户端 ...]
    end

    subgraph 服务器 (Server)
        S[ChatServer]
        S -- 管理 --> H1[ClientHandler 1]
        S -- 管理 --> H2[ClientHandler 2]
        S -- 管理 --> H3[ClientHandler ...]
    end

    C1 <-- TCP/IP --> H1
    C2 <-- TCP/IP --> H2
    C3 <-- TCP/IP --> H3

    H1 -- 消息处理 --> S
    H2 -- 消息处理 --> S
    H3 -- 消息处理 --> S

    S -- 消息路由 --> H1
    S -- 消息路由 --> H2
    S -- 消息路由 --> H3
```

## 2. 关键技术决策 (Key Technical Decisions)
*   **编程语言:** Java - 因其跨平台性、成熟的生态系统以及对网络编程的良好支持。
*   **网络通信:** TCP/IP套接字 (Sockets) - 提供可靠的、面向连接的通信。客户端和服务器之间通过 `ObjectInputStream` 和 `ObjectOutputStream` 序列化和反序列化 `Message` 对象进行通信。
*   **并发处理:** 服务器为每个连接的客户端创建一个新的线程 (`ClientHandler`)，以并发处理多个客户端请求。
*   **消息格式:** 自定义 `Message` 类 - 封装消息内容、发送者、接收者（可选）和消息类型。这种方式易于扩展和管理。

## 3. 使用的设计模式 (Design Patterns in Use)
*   **观察者模式 (Observer Pattern) (隐式):** 服务器可以被视为主题 (Subject)，当有新消息（特别是公共消息）或用户状态更改时，它会通知所有相关的客户端 (观察者)。`ClientHandler` 负责将这些更新推送给其关联的客户端。
*   **构建者模式 (Builder Pattern):** `Message` 类使用了构建者模式 (`Message.MessageBuilder`) 来创建消息对象，使得消息对象的构造更灵活、可读性更高，特别是当消息属性较多时。
*   **命令模式 (Command Pattern) (部分):** 客户端处理用户输入时，可以解析特定格式的输入（如 `/msg`, `/help`）作为命令，并执行相应的操作。这在 `ChatClient` 的主循环中有所体现。
*   **单例模式 (Singleton Pattern) (潜在):** `ChatServer` 本身可以设计为单例，以确保系统中只有一个聊天服务器实例。目前实现中并非严格单例，但概念上类似。

## 4. 组件关系 (Component Relationships)
*   `ChatServer`:
    *   监听指定端口等待客户端连接。
    *   为每个成功连接的客户端创建一个 `ClientHandler` 实例。
    *   维护一个所有 `ClientHandler` 实例的列表，用于广播消息和管理用户。
*   `ClientHandler` (服务器端):
    *   在独立的线程中运行。
    *   通过其关联的 `Socket` 从客户端读取 `Message` 对象。
    *   处理接收到的消息：
        *   如果是公共消息，则请求 `ChatServer` 广播给所有其他客户端。
        *   如果是私人消息，则请求 `ChatServer` 发送给特定的客户端。
        *   处理登录、登出等特殊消息类型。
    *   通过其关联的 `Socket` 向客户端发送 `Message` 对象。
*   `ChatClient`:
    *   连接到 `ChatServer`。
    *   拥有一个独立的线程用于从服务器读取消息并显示到控制台。
    *   主线程负责从用户控制台读取输入：
        *   解析用户输入。
        *   如果是普通文本，则构建公共 `Message` 对象并发送到服务器。
        *   如果是命令（如 `/msg`），则构建相应的 `Message` 对象（如私人消息）并发送到服务器。
*   `Message`:
    *   数据传输对象 (DTO)，用于在客户端和服务器之间传递信息。
    *   包含发送者、接收者（可选）、消息内容、消息类型等字段。
*   `MessageType`:
    *   枚举类型，定义了不同类型的消息（例如，`PUBLIC_MESSAGE`, `PRIVATE_MESSAGE`, `LOGIN_REQUEST`, `USER_JOINED`, `ERROR_MESSAGE` 等）。

## 5. 数据流 (Data Flow)
1.  **用户登录:**
    *   `ChatClient` -> `LOGIN_REQUEST` (Message) -> `ChatServer`
    *   `ChatServer` -> `ClientHandler` (处理登录)
    *   `ChatServer` -> `LOGIN_SUCCESS` / `LOGIN_FAILURE` (Message) -> `ChatClient`
    *   `ChatServer` -> `USER_JOINED` (Message, 广播) -> 所有其他 `ChatClient`
2.  **发送公共消息:**
    *   `ChatClient` (用户输入) -> `PUBLIC_MESSAGE` (Message) -> `ChatServer`
    *   `ChatServer` -> `ClientHandler` (接收)
    *   `ChatServer` (通过所有 `ClientHandler` 实例) -> `PUBLIC_MESSAGE` (Message, 广播) -> 所有 `ChatClient`
3.  **发送私人消息:**
    *   `ChatClient` (用户输入 `/msg <recipient> <message>`) -> `PRIVATE_MESSAGE` (Message) -> `ChatServer`
    *   `ChatServer` -> `ClientHandler` (接收)
    *   `ChatServer` (通过目标 `ClientHandler`) -> `PRIVATE_MESSAGE` (Message) -> 目标 `ChatClient` (和发送者 `ChatClient` 作为确认或回显)