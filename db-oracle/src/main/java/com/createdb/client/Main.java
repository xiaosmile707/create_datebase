package com.createdb.client;

import com.createdb.client.config.ConfigLoader;
import com.createdb.client.db.DatabaseClient;
import com.createdb.client.db.OracleClient;
import com.createdb.client.runner.DataSeeder;
import com.createdb.client.runner.SqlRunner;

import java.sql.Connection;

/**
 * Oracle 模块入口，执行建表 → 测试查询 → 种子数据灌入的完整流程。
 * <p>
 * 使用方式：{@code mvn exec:java -pl db-oracle}
 * <p>
 * 配置来源：从 {@code db-connection.json} 读取所有连接信息。
 */
public class Main {

    /** Oracle 数据库客户端 */
    private static final DatabaseClient CLIENT = new OracleClient();
    /** 数据库类型标识 */
    private static final String DB_TYPE = "oracle";

    public static void main(String[] args) {
        ConfigLoader config = new ConfigLoader();
        String version = config.getVersion();
        String host = config.getHost();
        int port = config.getPort();
        String user = config.getUser();
        String password = config.getPassword();

        System.out.println("===== db-oracle =====");
        System.out.println("Type: " + DB_TYPE + " | Version: " + version);
        System.out.println("Host: " + host + ":" + port);

        SqlRunner sqlRunner = new SqlRunner();
        DataSeeder seeder = new DataSeeder();

        // 1. 连接数据库
        // 2. 连接测试（Oracle 使用 SELECT ... FROM DUAL）
        // 3. 执行 DDL 建表
        // 4. 执行测试查询
        // 5. 灌入种子数据
        try (Connection conn = CLIENT.getConnection(host, port, version, user, password, config.getSslConfig())) {
            System.out.println("Connected.");

            sqlRunner.runQuery(conn, "SELECT 1 FROM DUAL", "Connection test:");

            String basePath = "sql/" + version + "/";

            sqlRunner.runScript(conn, basePath + "schema.sql");
            sqlRunner.runScript(conn, basePath + "test.sql");

            seeder.seed(conn, basePath + "seed.sql");

            System.out.println("Done.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
