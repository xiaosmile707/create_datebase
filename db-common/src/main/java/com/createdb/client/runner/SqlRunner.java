package com.createdb.client.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 每条语句执行后会通过日志输出影响行数或查询结果。
 * <p>
 * 默认 {@code stopOnError = true}，SQL 执行失败时抛出异常而非静默吞掉，
 * 以便调用方（如 {@link DatabaseBootstrap}）统一回滚事务。
 */
public class SqlRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlRunner.class);

    /** 遇到 SQL 执行错误时是否立即抛出异常，默认 true。 */
    private final boolean stopOnError;

    public SqlRunner() {
        this(true);
    }

    /**
     * @param stopOnError true 表示 SQL 失败时抛出异常；false 表示仅记录错误日志并继续
     */
    public SqlRunner(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    /**
     * 从 classpath 加载并执行一个 SQL 脚本文件。
     * <p>
     * 脚本以分号（;）为语句分隔符，自动跳过空行和以 {@code --} 开头的注释行。
     *
     * @param conn       数据库连接
     * @param scriptPath classpath 中的脚本路径，如 {@code sql/8.0/schema.sql}
     * @throws RuntimeException 当 stopOnError=true 且 SQL 执行失败时抛出
     */
    public void runScript(Connection conn, String scriptPath) {
        log.info("Executing: {}", scriptPath);
        try {
            String sql = readResource(scriptPath);
            String[] statements = splitStatements(sql);
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
                                log.info("  {}", row);
                            }
                        }
                    } else {
                        // INSERT/UPDATE/DELETE：打印影响行数
                        int count = s.getUpdateCount();
                        if (count >= 0) {
                            log.info("  {} row(s) affected", count);
                        }
                    }
                } catch (Exception stmtEx) {
                    if (stopOnError) {
                        throw new RuntimeException("SQL execution failed in " + scriptPath + ": " + stmtEx.getMessage(), stmtEx);
                    }
                    log.error("  FAILED: {}", stmtEx.getMessage());
                }
            }
            log.info("  OK");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (stopOnError) {
                throw new RuntimeException("Failed to run script: " + scriptPath, e);
            }
            log.error("  FAILED: {}", e.getMessage());
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
        log.info(label);
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder("  ");
                for (int i = 1; i <= cols; i++) {
                    row.append(rs.getString(i));
                    if (i < cols) row.append(" | ");
                }
                log.info(row.toString());
            }
        } catch (Exception e) {
            log.error("  FAILED: {}", e.getMessage());
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

    /**
     * 按分号拆分 SQL 语句，正确处理字符串字面量中的分号。
     * <p>
     * 跟踪单引号 {@code '} 内的内容，在引号内的分号不会被当作语句分隔符。
     * 同时处理双引号（标识符引用）和转义的单引号 {@code ''}。
     */
    private static String[] splitStatements(String sql) {
        java.util.List<String> result = new java.util.ArrayList<>();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int start = 0;
        int len = sql.length();

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : 0;

            if (inSingleQuote) {
                if (c == '\'' && next == '\'') {
                    i++; // 跳过转义的单引号
                } else if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"' && next == '"') {
                    i++; // 跳过转义的双引号
                } else if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ';') {
                    result.add(sql.substring(start, i));
                    start = i + 1;
                }
            }
        }
        // 添加最后一段（最后一个分号之后的内容）
        if (start < len) {
            result.add(sql.substring(start));
        }
        return result.toArray(new String[0]);
    }
}
