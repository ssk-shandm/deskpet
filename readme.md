# DeskPet - 你的桌面虚拟宠物

这是一个基于 Java 开发的客户端/服务器架构的桌面宠物应用程序。它会在你的桌面上显示一个可爱的虚拟宠物，你可以与它互动。所有的数据都将保存在服务端，允许多用户登录。

## ✨ 功能特性

- ✅ **桌面宠物显示**: 在你的电脑桌面上拥有一个虚拟伙伴。
- ✅ **用户系统**: 支持用户注册与登录，数据保存在服务端。
- 🔄 **(规划中)** **宠物互动**: 通过喂食、抚摸、玩耍等方式与你的宠物互动。
- 🔄 **(规划中)** **成长系统**: 宠物可以通过互动增加经验值，提升等级，甚至改变外观。
- ✅ **数据持久化**: 用户和宠物的状态被安全地存储在服务端的数据库中。

## 📂 项目架构

本项目采用分层清晰的客户端/服务器架构。

```bash
└── 📂 DeskPetProject/
    ├── 📂 server/  (后端：负责数据存储和核心逻辑)
    │   └── src/com/github/ssk_shandm/deskpet/server/
    │       ├── 📂 main/
    │       │   └── Server.java             # 服务端主入口
    │       ├── 📂 model/
    │       │   ├── User.java               # 用户数据模型
    │       │   └── Pet.java                # 宠物数据模型
    │       ├── 📂 dao/
    │       │   ├── DatabaseUtil.java       # 数据库连接工具
    │       │   ├── UserDao.java            # 用户数据访问
    │       │   └── PetDao.java             # 宠物数据访问
    │       └── 📂 service/
    │           ├── UserService.java        # 用户业务逻辑
    │           └── PetService.java         # 宠物业务逻辑
    │
    └── 📂 client/  (前端：负责界面显示和用户交互)
        └── src/com/github/ssk_shandm/deskpet/client/
            ├── 📂 main/
            │   └── Main.java               # 客户端主入口
            ├── 📂 model/
            │   ├── User.java               # 用户数据模型 (本地副本)
            │   └── Pet.java                # 宠物数据模型 (本地副本)
            ├── 📂 view/
            │   ├── LoginWindow.java        # 登录/注册窗口
            │   └── PetWindow.java          # 宠物主窗口
            ├── 📂 controller/
            │   ├── LoginController.java    # 登录逻辑控制
            │   └── PetController.java      # 宠物交互控制
            └── 📂 network/
                └── ApiClient.java          # 封装与服务器的通信
```

## 🛠️ 技术栈

- **主要语言**: Java
- **数据库**: SQLite

## 🚀 如何运行

### 1. 环境要求
- JDK 11 或更高版本。
- IntelliJ IDEA 或 Eclipse 等 Java IDE (推荐)。

### 2. 数据库设置
- 无需手动配置！
- 项目**首次运行**时，服务端会自动在项目根目录下创建 `deskpet.db` 文件以及所需的数据表。
- 数据表结构如下，供参考：
  ```sql
  -- 用户表
  CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL
  );
  
  -- 宠物表 (当前为单宠物模式)
  CREATE TABLE IF NOT EXISTS pets (
      id INTEGER PRIMARY KEY NOT NULL, -- ID 默认为 1
      name TEXT NOT NULL,
      level INTEGER DEFAULT 1,
      experience INTEGER DEFAULT 0
  );
  ```

### 3. 启动服务端
1. 在 IDE 中打开本项目。
2. 找到 `server` 模块下的 `src/com/github/ssk_shandm/deskpet/server/main/Server.java` 文件。
3. 直接运行 `main` 方法。
4. 当看到控制台输出 `服务器启动成功！...` 时，表示服务端已准备就绪。

### 4. 启动客户端
1. 确保服务端正在运行。
2. 找到 `client` 模块下的 `src/com/github/ssk_shandm/deskpet/client/main/Main.java` 文件。
3. 直接运行 `main` 方法启动客户端程序。

## 🤝 贡献

欢迎通过提交 Pull Requests 或 Issues 的方式为本项目做出贡献。

## 📄 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT) 许可证。
