package com.createdb.client.runner;

import java.sql.Connection;

/**
 * 数据灌入器，将预定义的种子数据写入数据库表。
 * <p>
 * 内部委托 {@link SqlRunner} 执行 SQL 脚本，通常用于在测试环境中
 * 填充初始数据（如用户、订单等样例记录）。
 */
public class DataSeeder {

    private final SqlRunner sqlRunner;

    public DataSeeder() {
        this.sqlRunner = new SqlRunner();
    }

    /**
     * 执行种子数据脚本。
     *
     * @param conn       数据库连接
     * @param scriptPath classpath 中的种子脚本路径，如 {@code sql/8.0/seed.sql}
     */
    public void seed(Connection conn, String scriptPath) {
        sqlRunner.runScript(conn, scriptPath);
    }
}
