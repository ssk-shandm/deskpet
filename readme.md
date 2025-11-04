# DeskPet - 桌面宠物项目

`DeskPet` 是一个基于 Java 实现的客户端-服务器（C/S）架构的桌面宠物应用程序。客户端使用 Java Swing 构建，服务器使用 Java Sockets 和 SQLite 数据库实现。这是一款以碧蓝档案游戏中角色seia（圣娅）模型为基础的桌宠程序

## 🌟 项目简介

`DeskPet` 是一个基于 Java 实现的【客户端-服务器】架构的桌面宠物应用程序。

它能在你的桌面上召唤一个动画角色（seia），她具有数个与用户的互动行为，有好感度系统，还有日常对话和语言气泡。

-   **客户端 (`client/`)**: 一个 Java Swing 桌面应用，负责在桌面上渲染宠物模型、播放动画和音频。
-   **服务器 (`server/`)**:  一个 Java Socket 服务器，用于处理用户认证、宠物数据同步，并使用 SQLite 进行数据持久化。

## ✨ 主要技术栈

* **☕ 核心语言**: **Java 21**
* **🛠️ 构建工具**: **Apache Maven**
* **🖥️ 客户端 (GUI)**:
    * Java Swing (用于构建桌面窗口)
    * [FlatLaf](https://www.formdev.com/flatlaf/) (Swing 的现代化外观（Look and Feel）库，作用于右键菜单的框架)
* **🌐 后端 (Server)**:
    * Java Sockets (用于实现自定义的C/S网络通信)
    * SQLite (通过 JDBC 驱动，用于服务器端的数据持久化（用户信息、宠物状态等）)
* **🎵 媒体**: Java `javax.sound.sampled` (用于播放WAV音频)
* **📦 打包**: `jpackage` (通过 `jpackage-maven-plugin` 创建“绿色版”可执行程序)
* 🧸**其他**：利用 blender 解析人物模型捕获动作序列帧

## 📁 项目结构

```text
deskpet/
├── src/
│   ├── main/
│   │   ├── java/com/github/ssk_shandm/deskpet/
│   │   │   ├── client/                 # 客户端 (View 和 Network)
│   │   │   │   ├── main/               # 客户端主入口 (Main.java)
│   │   │   │   ├── network/            # ApiClient (网络请求封装)
│   │   │   │   └── view/               # PetWindow, AudioManager 等 (Swing 视图)
│   │   │   └── server/                 # 服务端 (DAO, Service, Model)
│   │   │       ├── dao/                # 数据库访问对象 (PetDao, AudioDao...)
│   │   │       ├── main/               # Server (主服务), ClientHandler
│   │   │       ├── model/              # 数据实体 (Pet, User...)
│   │   │       └── service/            # 业务逻辑层 (PetService...)
│   │   └── resources/                  # 资源文件
│   │       ├── audio/                  # 音频文件
│   │       ├── mod/                    # 模型序列帧文件
│   │       └── tools/                  # 右键菜单交互工具图
│
├── .gitignore                      
├── pom.xml                             # Maven 配置文件
└── readme.md
```

## 🎮 玩法与功能

### ❤️ 好感度系统

- **动态变化**: 圣娅拥有一个好感度系统。如果你长时间（每 5 分钟）不与她互动，她的好感度会下降 5 点。
- **状态动画**: 她的待机动画会根据当前的好感度变化，从高到极低。
- **闲置状态**: 如果你 5 分钟没有任何操作，圣娅会进入疑惑状态，直到你再次与她互动。

### 🖱️ 核心交互

- **点击互动**: 左键单击圣娅，可以触发动画 并 **+15** 好感度。这个互动有 24 小时的冷却时间。
- **拖动宠物**: 按住鼠标左键拖动圣娅，可以将她在屏幕上移动。
- **投喂文件**: 将桌面上的任意文件拖拽到圣娅身上，她会“吃掉”它（文件将被移至回收站），增加 **+2** 好感度。
- **特殊投喂**: 如果你投喂的文件名为 **"街头混混系列第1本"**（不含扩展名），她会触发特殊语音并且 **+15** 好感度。

### 🔧 道具交互 (右键菜单)

- **特殊互动**: 这是一个有冷却（1分钟） 的动作，可 **+10** 好感度。
- **敲击! (锤子)**: 使用锤子（图标 `hammer.png`）敲击她，会 **-5** 好感度。
- **我推! (活塞)**: 用活塞（图标 `piston.gif`）推她，会 **-7** 好感度。

### 🔊 自动语音与设置

- **自动说话**: 圣娅每 3 分钟会随机说一句话（并显示气泡）。
- **设置**: 通过右键菜单，你可以：
  - 开启或关闭**静音**。
  - 通过滑动条**调整音量** (0%-100%)。
  - 开启或关闭**语音气泡**。
  - **退出**程序。

## 🚀 如何启动 (简易指南)

### 1. 🥇 用户使用指南 (推荐)

本程序已使用 `jpackage` 打包为“绿色版”，**无需**用户电脑上预先安装 Java 运行环境 (JRE)。

1. **下载**: 前往本项目的 **[Releases](https://github.com/ssk-shandm/deskpet/releases)** 页面。在最新的版本中，下载 `DeskPet-v1.0.0.zip` 压缩文件。
2. **解压**: 将压缩包完整解压。
3. **运行**: 双击运行 `DeskPet.exe`。
4. **完成**: 程序会自动在后台启动本地数据库服务，并显示桌面宠物。

### 2. 开发者构建指南 (从源码)

本指南用于从源代码编译和运行项目，适用于开发和调试。

**先决条件**:

* JDK (推荐 1.8 或更高版本)
* IDE (如 IntelliJ IDEA 或 Eclipse)
* 已正确配置 `client/lib` 和 `server/lib` 中的 JAR 依赖。

**方式一：在 IDE 中运行 (用于调试)**:

1. 使用 IDE 打开 `pom.xml` 导入 Maven 项目。
2. 等待 Maven 自动下载依赖 (FlatLaf, SQLite-JDBC)。
3. 找到并运行 `src/main/java/com/github/ssk_shandm/deskpet/client/main/Main.java` 中的 `main` 方法。
4. `Main.java` 会自动先启动后台服务器线程，然后再启动客户端 UI。

**方式二：使用 Maven 构建 (用于打包)**:

1. 打开命令行/终端，进入项目根目录 ( `pom.xml` 所在的目录)。

2. 运行 Maven 命令：

   ```bash
   # 运行所有阶段，包括打包和 jpackage
   mvn clean verify
   ```
   
   ```bash
   # 只打包成可运行的 JAR
   mvn clean package
   ```
   

3.构建产物:

- 可运行 JAR: 构建完成后，你会在 `target/` 目录下找到 `deskpet-1.0.0.jar` (这是一个“胖 JAR”，包含了所有依赖)。
- “绿色版”安装包: 如果你运行了 `verify` 阶段，你会在 `target/dist/DeskPet/` 目录下找到 `jpackage` 打包好的完整程序。

## ⚖️ 许可证

本项目基于 [**MIT License**](https://choosealicense.com/licenses/mit/) 许可证开源。

