package com.github.ssk_shandm.deskpet.server.dao;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库连接和初始化工具类
 * 负责管理 SQLite 连接和首次运行时的表创建。
 */
public class DatabaseUtil {

    private static final Logger logger = Logger.getLogger(DatabaseUtil.class.getName());
    /** 数据库连接字符串 (本地文件) */
    private static final String URL = "jdbc:sqlite:deskpet.db";

    /**
     * 程序运行时，检查并确保所有表都已创建。
     */
    public static void initializeDatabase() {
        File dbFile = new File("deskpet.db");
        logger.info("数据库文件路径: " + dbFile.getAbsolutePath());

        // 创建 users 表
        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE" +
                ");";

        // 创建 pets 表
        String createPetTableSql = "CREATE TABLE IF NOT EXISTS pets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "likeability INTEGER DEFAULT 100," +
                "last_click_time BIGINT DEFAULT 0" +
                ");";

        // 创建 audio_durations 表
        String createAudioTableSql = "CREATE TABLE IF NOT EXISTS audio_durations (" +
                "key TEXT PRIMARY KEY," +
                "duration_ms INTEGER NOT NULL" +
                ");";

        // 插入默认数据
        String insertDefaultPetSql = "INSERT INTO pets (name, likeability, last_click_time) VALUES ('seia', 100, 0);";
        String insertDefaultUserSql = "INSERT INTO users (username) VALUES ('testuser');";

        // 使用 try-with-resources 自动关闭连接和 Statement
        try (Connection cc = getConnection();
                Statement stmt = cc.createStatement()) {

            // 执行建表 (CREATE TABLE IF NOT EXISTS 是安全的)
            stmt.execute(createUserTableSql);
            stmt.execute(createPetTableSql);
            stmt.execute(createAudioTableSql); 
            logger.info("数据库表检查/创建成功！");

            // 检查 pets 表是否为空
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pets")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute(insertDefaultPetSql);
                    logger.info("默认宠物数据插入成功！");
                }
            }
            // 检查 users 表是否为空
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute(insertDefaultUserSql);
                    logger.info("默认用户数据插入成功！");
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "数据库初始化失败", e);
        }
    }

    /**
     * 获取一个到 SQLite 数据库的连接
     * * @return Connection 对象, 如果失败则返回 null
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
}