package com.createdb.client;

import com.createdb.client.db.PostgresqlClient;
import com.createdb.client.runner.DatabaseBootstrap;

/**
 * PostgreSQL 模块入口，委托 {@link DatabaseBootstrap} 执行建表 → 测试查询 → 种子数据灌入的完整流程。
 * <p>
 * 使用方式：{@code mvn exec:java -pl db-postgresql}
 * <p>
 * 配置来源：从 {@code db-connection.json} 读取所有连接信息。
 */
public class Main {

    public static void main(String[] args) {
        DatabaseBootstrap.run(new PostgresqlClient(), "postgresql");
    }
}
