package com.createdb.client;

import com.createdb.client.db.H2Client;
import com.createdb.client.runner.DatabaseBootstrap;

/**
 * H2 模块入口，委托 {@link DatabaseBootstrap} 通过 TCP 协议连接 H2 Server，执行建表 → 测试查询 → 种子数据灌入。
 * <p>
 * 使用方式：{@code mvn exec:java -pl db-h2}
 * <p>
 * 配置来源：从 {@code db-connection.json} 读取所有连接信息。
 */
public class Main {

    public static void main(String[] args) {
        DatabaseBootstrap.run(new H2Client(), "h2");
    }
}
