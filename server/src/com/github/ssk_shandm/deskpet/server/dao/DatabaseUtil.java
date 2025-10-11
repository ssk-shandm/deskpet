package com.github.ssk_shandm.deskpet.server.dao;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUtil {

    private static final Logger logger = Logger.getLogger(DatabaseUtil.class.getName());

    // 1. 新的数据库地址：它指向项目运行目录下的一个名叫 deskpet.db 的文件
    // 如果这个文件不存在，JDBC 驱动会自动创建它！
    private static final String URL = "jdbc:sqlite:deskpet.db";

    /**
     * 获取数据库连接
     * 
     * @return Connection对象或null
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            // 2. 驱动类名换成 SQLite 的
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "数据库连接或驱动加载失败", e);
        }
        return connection;
    }

    /**
     * 程序首次运行时，用于初始化数据库和表的静态方法
     */
    public static void initializeDatabase() {
        // 检查数据库文件是否已经存在
        File dbFile = new File("deskpet.db");
        if (dbFile.exists()) {
            logger.info("数据库文件已存在，无需初始化。");
            return;
        }

        logger.info("首次运行，正在初始化数据库...");
        // 这是 SQLite 的建表语句，和 MySQL 略有不同
        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "points INTEGER NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createPetTableSql = "CREATE TABLE IF NOT EXISTS pets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "name TEXT DEFAULT '我的宠物'," +
                "hunger INTEGER DEFAULT 100," +
                "mood INTEGER DEFAULT 100," +
                "level INTEGER DEFAULT 1," +
                "last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            // 执行建表
            stmt.execute(createUserTableSql);
            stmt.execute(createPetTableSql);
            logger.info("数据库表创建成功！");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "数据库初始化失败", e);
        }
    }
}