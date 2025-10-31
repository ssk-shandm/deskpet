# 🚀 DeskPet - 桌面宠物项目

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

```text
deskpet/
├── client/                                       # 客户端模块
│   ├── lib/                                      # 客户端依赖 (flatlaf-3.6.2.jar)
│   ├── resources/                                # 客户端资源 (音频, 动画帧, 图标)
│   │   ├── audio/                                # 音频文件
│   │   ├── mod/                                  # 模型序列帧文件
│   │   └── tools/                                # 右键菜单交互工具图
│   └── src/                                      # 客户端源码 (com.github.ssk_shandm.deskpet.client)
│       ├── main/                                 # 启动类
│       ├── network/                              # ApiClient (网络请求封装)
│       └── view/                                 # PetWindow, AudioManager 等 (Swing 视图)
│
├── server/                                       # 服务端模块
│   ├── lib/                                      # 服务端依赖 (sqlite-jdbc-3.50.3.0.jar)
│   └── src/                                      # 服务端源码 (com.github.ssk_shandm.deskpet.server)
│       ├── dao/                                  # 数据库访问对象 (UserDao, PetDao...)
│       ├── main/                                 # Server (主服务), ClientHandler (多线程处理)
│       ├── model/                                # 数据实体 (User, Pet...)
│       └── service/                              # 业务逻辑层
│
├── .gitignore
└── ... (IntelliJ IDEA 配置文件 .iml)
```

## 🚀 如何启动 (简易指南)

### 1. 用户使用指南 (jpackage 绿色版)

本程序已使用 `jpackage` 打包为绿色版，**无需**用户电脑上预先安装 Java 运行环境 (JRE)。

1.  **下载**: 获取打包后的 `.zip` 压缩文件。
2.  **解压**: 将压缩包完整解压到你希望的任意位置 (例如 `D:\DeskPet`)。
3.  **运行**:
    * 进入解压后的文件夹 (例如 `D:\DeskPet\DeskPet`)。
    * 双击运行 `DeskPet.exe` (或你打包时命名的 `.exe` 文件)。
4.  **完成**: 程序会自动在后台启动本地数据库服务，并显示桌面宠物。

**【注意事项】**
* 启动 `DeskPet.exe` 就会同时启动客户端和服务端，**无需**任何其他操作。
* 程序数据（例如 `deskpet.db` 数据库文件）会自动创建在 `.exe` 所在的目录中。
* 如需退出，请在宠物身上**单击右键**，在菜单中选择“退出”。

### 2. 开发者构建指南 (从源码)

本指南用于从源代码编译和运行项目，适用于开发和调试。

**先决条件**:

* JDK (推荐 1.8 或更高版本)
* IDE (如 IntelliJ IDEA 或 Eclipse)
* 已正确配置 `client/lib` 和 `server/lib` 中的 JAR 依赖。

**启动步骤 (二选一)**:

**方式一 (推荐：All-in-One 启动)**:
1.  在 IDE 中找到并运行 `client/src/com/github/ssk_shandm/deskpet/client/main/Main.java` 的 `main` 方法。
2.  该 `main` 方法会**自动**先启动服务器线程，等待2秒后，再启动客户端 UI。

**方式二 (传统：分开启动)**:
1.  **启动服务器**:
    * 在 IDE 中找到并运行 `server/src/com/github/ssk_shandm/deskpet/server/main/Server.java` 的 `main` 方法。
    * 看到服务器已在指定端口启动的日志。
2.  **启动客户端**:
    * (确保服务器已运行)
    * 在 IDE 中找到并运行 `client/src/com/github/ssk_shandm/deskpet/client/main/Main.java` 的 `main` 方法。
    * *(注意：如果使用此方式，你可能需要注释掉 `client/main/Main.java` 中启动服务器线程和 `Thread.sleep` 的代码)*


## 5. 许可证

