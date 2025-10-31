# 🚀 DeskPet - 桌面宠物项目 🚀

`DeskPet` 是一个基于 Java 实现的客户端-服务器（C/S）架构的桌面宠物应用程序。客户端使用 Java Swing 构建，服务器使用 Java Sockets 和 SQLite 数据库实现。这是一款以碧蓝档案游戏中角色seia（圣娅）模型为基础的桌宠程序

## 🌟 项目简介

`DeskPet` 是一个基于 Java 实现的【客户端-服务器】架构的桌面宠物应用程序。

它能在你的桌面上召唤一个动画角色（seia），她具有数个与用户的互动行为，有好感度系统，还有日常对话和语言气泡。

-   **客户端 (`client/`)**: 一个 Java Swing 桌面应用，负责在桌面上渲染宠物模型、播放动画和音频。
-   **服务器 (`server/`)**:  一个 Java Socket 服务器，用于处理用户认证、宠物数据同步，并使用 SQLite 进行数据持久化。

## ✨ 主要技术栈

* **☕ 核心语言**: Java
* **🖥️ 客户端 (GUI)**:
    * Java Swing (用于构建桌面窗口)
    * [FlatLaf](https://www.formdev.com/flatlaf/) (Swing 的现代化外观（Look and Feel）库，作用于右键菜单的框架)
* **🌐 后端 (Server)**:
    * Java Sockets (用于实现自定义的C/S网络通信)
    * SQLite (通过 JDBC 驱动，用于服务器端的数据持久化（用户信息、宠物状态等）)
* **🎵 媒体**: Java `javax.sound.sampled` (用于播放WAV音频)
* 🧸**其他**：利用 blender 解析人物模型捕获动作序列帧

## 📁 项目结构

deskpet/
├── client/                                             # 客户端模块
│   ├── lib/                                             # 客户端依赖 (flatlaf-3.6.2.jar)
│   ├── resources/                                # 客户端资源 (音频, 动画帧, 图标)
│   │   ├── audio/                                  # 音频文件
│   │   ├── mod/                                    # 模型序列帧文件
│   │   └── tools/                                    #右键菜单交互工具图
│   └── src/                                             # 客户端源码 (com.github.ssk_shandm.deskpet.client)
│       ├── main/                                     # 启动类
│       ├── network/                               # ApiClient (网络请求封装)
│       └── view/                                      # PetWindow, AudioManager 等 (Swing 视图)
│
├── server/                                            # 服务端模块
│   ├── lib/                                             # 服务端依赖 (sqlite-jdbc-3.50.3.0.jar)
│   └── src/                                            # 服务端源码 (com.github.ssk_shandm.deskpet.server)
│       ├── dao/                                      # 数据库访问对象 (UserDao, PetDao...)
│       ├── main/                                    # Server (主服务), ClientHandler (多线程处理)
│       ├── model/                                 # 数据实体 (User, Pet...)
│       └── service/                                # 业务逻辑层
│
├── .gitignore
└── ... (IntelliJ IDEA 配置文件 .iml)

## 🚀 如何启动 (简易指南)

### 1.用户使用指南 (打包后)

> **(TODO: 开发者填写)**
>
> 请在你完成项目打包（例如，打包为可执行的 `.jar` 文件，并提供 `.bat` 或 `.sh` 启动脚本）后，在此处更新最终用户的启动说明。

**【启动说明示例】**

*(请根据你的实际打包方式替换以下内容)*

1. **启动服务器**:
   - Windows: 双击 `run-server.bat`。
   - macOS/Linux: 在终端执行 `sh run-server.sh`。
   - 或通过 Java 命令: `java -jar deskpet-server.jar`
2. **启动客户端**:
   - Windows: 双击 `run-client.bat`。
   - macOS/Linux: 在终端执行 `sh run-client.sh`。
   - 或通过 Java 命令: `java -jar deskpet-client.jar`
3. **使用**: 启动客户端后，注册并登录即可。



### 4.2. 开发者构建指南 (从源码)

本指南用于从源代码编译和运行项目，适用于开发和调试。

**先决条件**:

- JDK (推荐 1.8 或更高版本)
- IDE (如 IntelliJ IDEA 或 Eclipse)
- 已正确配置 `client/lib` 和 `server/lib` 中的 JAR 依赖。

**启动步骤**:

1. **启动服务器**:
   - 在 IDE 中找到并运行 `server/src/com/github/ssk_shandm/deskpet/server/main/Server.java` 的 `main` 方法。
   - 看到服务器已在指定端口启动的日志。
2. **启动客户端**:
   - 在 IDE 中找到并运行 `client/src/com/github/ssk_shandm/deskpet/client/main/Main.java` 的 `main` 方法。



## 5. 许可证

