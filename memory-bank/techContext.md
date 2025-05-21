# 技术背景 (Tech Context)

## 1. 使用的技术栈 (Technology Stack)
*   **核心语言:** Java (JDK 11 或更高版本推荐)
*   **构建工具:** Apache Maven - 用于项目构建、依赖管理和打包。
*   **网络通信:** Java Sockets API (java.net.Socket, java.net.ServerSocket)
*   **对象序列化:** Java Object Serialization (`java.io.ObjectInputStream`, `java.io.ObjectOutputStream`) - 用于在客户端和服务器之间传输 `Message` 对象。
*   **并发:** Java Threads (`java.lang.Thread`, `java.util.concurrent` 包中的类，如果需要更高级的并发控制)
*   **日志:** Logback (通过 SLF4J) - 用于记录应用程序事件和调试信息。

## 2. 开发环境设置 (Development Environment Setup)
*   **IDE:** 推荐使用 IntelliJ IDEA, Eclipse, 或 VS Code (配置了 Java 开发扩展)。
*   **JDK:** 安装 Java Development Kit (JDK), 版本 11 或更高。确保 `JAVA_HOME` 环境变量已正确设置，并且 `java` 和 `javac` 命令在系统路径中。
*   **Maven:** 安装 Apache Maven。确保 `mvn` 命令在系统路径中。
*   **项目克隆/导入:**
    *   从版本控制系统 (如 Git) 克隆项目。
    *   将项目作为 Maven 项目导入到 IDE 中。IDE 通常会自动识别 `pom.xml` 文件并下载相关依赖。

## 3. 构建与运行 (Building and Running)
*   **构建项目:**
    *   在项目根目录下运行 Maven 命令: `mvn clean package`
    *   这将编译代码、运行测试（如果配置了）并将应用程序打包成 JAR 文件（例如，`target/webChat-1.0-SNAPSHOT.jar`，以及可能单独的客户端和服务端 JAR）。
*   **运行服务器:**
    *   使用以下命令运行服务器 JAR (假设服务器的主类是 `com.example.chat.server.ChatServer`，并且已通过 `maven-jar-plugin` 或 `maven-assembly-plugin` 正确配置了 Main-Class 属性):
        ```bash
        java -jar target/chat-server.jar [port_number]
        ```
    *   如果端口号未提供，服务器将使用默认端口 (例如，8080)。
*   **运行客户端:**
    *   使用以下命令运行客户端 JAR (假设客户端的主类是 `com.example.chat.client.ChatClient`):
        ```bash
        java -jar target/chat-client.jar [server_host] [server_port]
        ```
    *   如果服务器主机和端口未提供，客户端将尝试连接到默认地址 (例如 `localhost:8080`)。

## 4. 技术约束 (Technical Constraints)
*   **网络依赖:** 应用程序的运行强依赖于稳定的网络连接。
*   **Java 运行时:** 客户端和服务器都需要安装兼容版本的 Java Runtime Environment (JRE)。
*   **序列化兼容性:** 如果 `Message` 类的结构发生不兼容的更改 (例如，删除字段或更改字段类型而未处理 `serialVersionUID`)，可能会导致不同版本的客户端和服务器之间通信失败。
*   **无状态消息:** 当前设计中，消息是短暂的。服务器不存储消息历史记录。如果客户端断开连接，它将丢失在断开期间发送的消息。
*   **单点故障:** 当前服务器是单点。如果服务器崩溃，所有客户端都将断开连接，服务将不可用。
*   **安全性:** 当前实现未包含任何加密或强大的身份验证机制。通信是明文的，用户名也未加密存储或验证。

## 5. 主要依赖项 (Key Dependencies - from pom.xml)
*   **SLF4J API (`org.slf4j:slf4j-api`):** 简单的日志门面，允许在部署时插入所需的日志实现。
*   **Logback Classic (`ch.qos.logback:logback-classic`):** SLF4J 的一个强大且流行的原生实现。
*   **Logback Core (`ch.qos.logback:logback-core`):** Logback Classic 的核心组件。
*   **(可选) JUnit (`org.junit.jupiter:junit-jupiter-api`, `org.junit.jupiter:junit-jupiter-engine`):** 如果项目包含单元测试。

## 6. 版本控制 (Version Control)
*   推荐使用 Git 进行版本控制。
*   维护清晰的提交历史和分支策略（例如，GitFlow）。