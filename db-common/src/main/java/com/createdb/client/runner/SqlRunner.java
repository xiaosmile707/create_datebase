package com.createdb.client.runner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * SQL 脚本执行器，负责从 classpath 加载 SQL 文件并按分号拆分逐条执行。
 * <p>
 * 支持 DDL（建表）、DML（插入/更新）以及 SELECT 查询。
 * 每条语句执行后会打印影响行数或查询结果表格。
 */
public class SqlRunner {

    /**
     * 从 classpath 加载并执行一个 SQL 脚本文件。
     * <p>
     * 脚本以分号（;）为语句分隔符，自动跳过空行和以 {@code --} 开头的注释行。
     *
     * @param conn       数据库连接
     * @param scriptPath classpath 中的脚本路径，如 {@code sql/8.0/schema.sql}
     */
    public void runScript(Connection conn, String scriptPath) {
        System.out.println("Executing: " + scriptPath);
        try {
            String sql = readResource(scriptPath);
            String[] statements = sql.split(";");
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                // 跳过空行和 SQL 注释
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                try (Statement s = conn.createStatement()) {
                    boolean hasResultSet = s.execute(trimmed);
                    if (hasResultSet) {
                        // SELECT 查询：打印结果集表格
                        try (ResultSet rs = s.getResultSet()) {
                            int cols = rs.getMetaData().getColumnCount();
                            while (rs.next()) {
                                StringBuilder row = new StringBuilder();
                                for (int i = 1; i <= cols; i++) {
                                    row.append(rs.getString(i));
                                    if (i < cols) row.append(" | ");
                                }
                                System.out.println("  " + row);
                            }
                        }
                    } else {
                        // INSERT/UPDATE/DELETE：打印影响行数
                        int count = s.getUpdateCount();
                        if (count >= 0) {
                            System.out.println("  " + count + " row(s) affected");
                        }
                    }
                }
            }
            System.out.println("  OK");
        } catch (Exception e) {
            System.err.println("  FAILED: " + e.getMessage());
        }
    }

    /**
     * 执行单条 SELECT 查询并打印结果。
     *
     * @param conn  数据库连接
     * @param sql   要执行的 SQL 查询语句
     * @param label 打印时的标签（如 "Connection test:"）
     */
    public void runQuery(Connection conn, String sql, String label) {
        System.out.println(label);
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder("  ");
                for (int i = 1; i <= cols; i++) {
                    row.append(rs.getString(i));
                    if (i < cols) row.append(" | ");
                }
                System.out.println(row);
            }
        } catch (Exception e) {
            System.err.println("  FAILED: " + e.getMessage());
        }
    }

    /**
     * 从 classpath 读取资源文件为字符串。
     */
    private String readResource(String path) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
