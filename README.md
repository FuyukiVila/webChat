# WebChat - Java网络聊天系统

这是一个基于Java Socket的网络聊天系统，支持多人聊天室和私人消息功能。

## 主要功能

- **用户认证系统**：用户可以使用唯一的用户名登录
- **聊天室系统**：
  - 创建聊天室（可选设置密码保护）
  - 加入现有聊天室
  - 在聊天室内发送消息
  - 查看聊天室成员信息
  - 聊天室历史消息记录
  - 修改房间密码（仅房主可用）
- **私人消息**：用户可以向特定在线用户发送私聊消息
- **在线用户管理**：查看当前在线用户列表
- **丰富的命令系统**：通过命令行界面执行各种操作

## 技术特点

- **基于Java Socket的网络通信**：通过TCP/IP提供可靠的通信
- **消息序列化传输**：使用`ObjectInputStream`和`ObjectOutputStream`序列化消息对象
- **并发处理**：服务器使用线程池处理多客户端连接
- **函数式编程**：使用Java 8+的函数式特性处理消息和命令
- **线程安全设计**：确保在多线程环境下数据一致性
- **优雅的关闭机制**：服务器和客户端都支持优雅退出

## 系统架构

采用经典的客户端-服务器模式：

- **服务器端**：
  - 管理用户连接和会话
  - 处理消息路由
  - 维护聊天室状态
  - 管理在线用户列表
- **客户端**：
  - 提供命令行界面
  - 处理用户输入
  - 显示接收的消息
  - 管理本地会话状态

## 安装和运行

### 系统要求

- Java 21 或更高版本
- Maven 3.6 或更高版本

### 构建项目

```bash
mvn clean package
```

### 运行服务器

```bash
# Linux/macOS
java -jar target/chat-server.jar [port]

# Windows (推荐，解决中文乱码)
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar target/chat-server.jar [port]
```

默认端口: 8888

### 运行客户端

```bash
# Linux/macOS
java -jar target/chat-client.jar [host] [port]

# Windows (推荐，解决中文乱码)
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar target/chat-client.jar [host] [port]
```

默认连接: localhost:8888

### 运行GUI客户端

```bash
# Linux/macOS
java -jar target/chat-gui-client.jar

# Windows (推荐，解决中文乱码)
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -jar target/chat-gui-client.jar
```

### 快速启动（推荐）

使用提供的启动脚本：

```bash
# Linux/macOS
./demo.sh

# Windows
demo.bat
```

### 中文显示问题解决

如果在Windows环境下遇到中文乱码问题，请：

1. **设置控制台代码页**：
   ```cmd
   chcp 65001
   ```

2. **使用UTF-8启动参数**：
   ```cmd
   java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Duser.language=zh -Duser.country=CN -jar [jar文件]
   ```

3. **推荐使用Windows Terminal**获得更好的中文显示效果

4. **GUI客户端字体要求**：
   - 系统需要安装 Noto Sans SC 字体以获得最佳中文显示效果
   - 如果没有该字体，系统将使用默认字体

## 客户端命令

| 命令 | 描述 |
|------|------|
| `/help` | 显示帮助信息 |
| `/clear` | 清除屏幕 |
| `/exit` | 退出聊天室 |
| `/list` | 查看在线用户 |
| `/rooms` | 查看可用聊天室 |
| `/create-room room-name <密码>` | 创建新聊天室（密码可选） |
| `/join room-name <密码>` | 加入聊天室（密码可选） |
| `/passwd room-name <新密码>` | 修改房间密码（仅房主可用，空密码则取消密码） |
| `/leave` | 离开当前聊天室 |
| `/room-info` | 显示当前房间信息和成员列表 |
| `/pm <用户名> <消息>` | 发送私聊消息 |

## 许可证

请参阅项目中的[LICENSE](LICENSE)文件。
