package com.github.ssk_shandm.deskpet.server.dao;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUtil {

    private static final Logger logger = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String URL = "jdbc:sqlite:deskpet.db";

    /**
     * 获取数据库连接
     * 
     * @return Connection对象或null
     */
    public static Connection getConnection() {
        Connection cc = null;
        try {
            // 加载 SQLite 驱动
            Class.forName("org.sqlite.JDBC");
            cc = DriverManager.getConnection(URL);

        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "获取数据库连接失败", e);
        }
        return cc;
    }

    /**
     * 程序首次运行时，用于初始化数据库和表
     */
    public static void initializeDatabase() {

        File dbFile = new File("deskpet.db");
        if (dbFile.exists()) {
            logger.info("数据库文件已存在，无需初始化。");
            return;
        }

        logger.info("首次运行，正在初始化数据库...");
        // String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
        // "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        // "username TEXT NOT NULL UNIQUE," +
        // // "password TEXT NOT NULL," +
        // "points INTEGER NOT NULL DEFAULT 0" +
        // // "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
        // ");";

        String createPetTableSql = "CREATE TABLE IF NOT EXISTS pets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT DEFAULT 'kami'," +
                "rank INTEGER DEFAULT 1," +
                "status TEXT DEFAULT 'health'," +
                "likeability INTEGER DEFAULT 0" +
                ");";

        try (Connection cc = getConnection();
                Statement stmt = cc.createStatement()) {
            // 执行建表
            // stmt.execute(createUserTableSql);
            stmt.execute(createPetTableSql);
            logger.info("数据库表创建成功！");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "数据库初始化失败", e);
        }
    }
}