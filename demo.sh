#!/bin/bash

# 聊天应用程序演示脚本
echo "=== 聊天应用程序演示 ==="
echo ""
echo "可用的组件："
echo "1. 服务器端"
echo "2. 命令行客户端"
echo "3. GUI 客户端"
echo ""

# 检查 jar 文件是否存在
if [ ! -f "target/chat-server.jar" ]; then
    echo "错误：未找到编译后的 jar 文件。请先运行 'mvn package'"
    exit 1
fi

echo "选择要启动的组件："
echo "1) 启动服务器"
echo "2) 启动命令行客户端"
echo "3) 启动 GUI 客户端"
echo "4) 显示使用说明"
echo ""

read -p "请输入选择 (1-4): " choice

case $choice in
    1)
        echo "启动服务器..."
        java -jar target/chat-server.jar
        ;;
    2)
        echo "启动命令行客户端..."
        java -jar target/chat-client.jar
        ;;
    3)
        echo "启动 GUI 客户端..."
        java -jar target/chat-gui-client.jar
        ;;
    4)
        echo ""
        echo "=== 使用说明 ==="
        echo ""
        echo "1. 首先启动服务器："
        echo "   java -jar target/chat-server.jar"
        echo ""
        echo "2. 然后启动客户端（可以启动多个）："
        echo "   命令行版本：java -jar target/chat-client.jar"
        echo "   GUI 版本：  java -jar target/chat-gui-client.jar"
        echo ""
        echo "3. GUI 客户端功能："
        echo "   - 登录时输入用户名、服务器地址和端口"
        echo "   - 使用菜单创建房间或修改密码"
        echo "   - 双击房间列表加入房间"
        echo "   - 在聊天区域发送消息"
        echo ""
        echo "4. 字体要求："
        echo "   GUI 使用 Noto Sans SC 字体，确保系统已安装"
        echo ""
        ;;
    *)
        echo "无效选择，退出"
        exit 1
        ;;
esac