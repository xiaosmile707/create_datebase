package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * H2 数据库客户端，通过 TCP 协议连接 Docker 容器中的 H2 Server 模式。
 * <p>
 * H2 默认管理员用户为 sa，无密码，TCP 端口为 9092。
 * 数据库在首次 TCP 连接时自动创建，无需手动执行 CREATE DATABASE。
 */
public class H2Client implements DatabaseClient {

    /**
     * 建立 H2 TCP 连接。
     *
     * @param host     数据库主机地址
     * @param port     H2 TCP 端口（默认 9092）
     * @param version  数据库版本
     * @param user     用户名（默认 sa）
     * @param password 密码（H2 默认为空）
     * @param ssl      SSL 配置（H2 TCP 模式通常不使用）
     * @return JDBC 连接对象
     * @throws Exception 连接失败时抛出
     */
    @Override
    public Connection getConnection(String host, int port, String database, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("org.h2.Driver");
        String url = getJdbcUrl(host, port, database, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 构建 H2 TCP 模式的 JDBC URL。
     * <p>
     * 格式：jdbc:h2:tcp://{host}:{port}/{database};DB_CLOSE_DELAY=-1
     */
    @Override
    public String getJdbcUrl(String host, int port, String database, String version, SslConfig ssl) {
        // H2 TCP Server 模式，默认连接 testdb 数据库
        String db = (database != null && !database.isEmpty()) ? database : "testdb";
        return "jdbc:h2:tcp://" + host + ":" + port + "/" + db + ";DB_CLOSE_DELAY=-1";
    }
}
