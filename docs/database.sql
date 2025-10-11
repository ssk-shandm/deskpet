-- 创建数据库
CREATE DATABASE IF NOT EXISTS deskpet_db;

-- 切换到该数据库
USE deskpet_db;

-- 创建 users 表
CREATE TABLE IF NOT EXISTS
    users (
        id INT PRIMARY KEY AUTO_INCREMENT,
        username VARCHAR(50) NOT NULL UNIQUE,
        password VARCHAR(50) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        POINTS INT NOT NULL DEFAULT 0
    );

-- 为 users 表添加 points 字段 (如果表已存在但没有这个字段)
-- ALTER TABLE users
-- ADD COLUMN IF NOT EXISTS points INT NOT NULL DEFAULT 0;

-- 创建 pets 表
CREATE TABLE IF NOT EXISTS
    pets (
        id INT PRIMARY KEY AUTO_INCREMENT,
        RANK INT NOT NULL DEFAULT 0,
        STATUS VARCHAR(20) NOT NULL,
        NAME VARCHAR(20) NOT NULL,
    )