package com.createdb.client;

import com.createdb.client.config.ConfigLoader;
import com.createdb.client.db.DatabaseClient;
import com.createdb.client.db.MysqlClient;
import com.createdb.client.runner.DataSeeder;
import com.createdb.client.runner.SqlRunner;

import java.sql.Connection;

/**
 * MySQL 模块入口，执行建表 → 测试查询 → 种子数据灌入的完整流程。
 * <p>
 * 使用方式：{@code mvn exec:java -pl db-mysql}
 * <p>
 * 配置来源：先读取 {@code db-secret.properties}，缺失字段 fallback 到
 * AI Skill 生成的 {@code db-connection.json}。
 */
public class Main {

    /** MySQL 数据库客户端 */
    private static final DatabaseClient CLIENT = new MysqlClient();
    /** 数据库类型标识，对应 properties 中的 key 前缀 */
    private static final String DB_TYPE = "mysql";

    /**
     * @param args 可选，第一个参数为自定义配置路径，默认 {@code db-secret.properties}
     */
    public static void main(String[] args) {
        // 可指定自定义配置文件路径
        String configPath = args.length > 0 ? args[0] : "db-secret.properties";

        // 加载配置（props 优先，db-connection.json fallback）
        ConfigLoader config = new ConfigLoader(configPath, DB_TYPE);
        String version = config.getVersion();
        String host = config.getHost();
        int port = config.getPort();
        String user = config.getUser();
        String password = config.getPassword();

        System.out.println("===== db-mysql =====");
        System.out.println("Type: " + DB_TYPE + " | Version: " + version);
        System.out.println("Host: " + host + ":" + port);

        SqlRunner sqlRunner = new SqlRunner();
        DataSeeder seeder = new DataSeeder();

        // 1. 连接数据库
        // 2. 连接测试
        // 3. 执行 DDL 建表
        // 4. 执行测试查询
        // 5. 灌入种子数据
        try (Connection conn = CLIENT.getConnection(host, port, version, user, password, config.getSslConfig())) {
            System.out.println("Connected.");

            sqlRunner.runQuery(conn, "SELECT 1", "Connection test:");

            String basePath = "sql/" + version + "/";

            sqlRunner.runScript(conn, basePath + "schema.sql");
            sqlRunner.runScript(conn, basePath + "test.sql");

            seeder.seed(conn, basePath + "seed.sql");

            System.out.println("Done.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
