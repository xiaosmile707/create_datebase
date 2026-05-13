package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * ClickHouse 数据库客户端，通过 JDBC 连接远程 ClickHouse 实例。
 * <p>
 * 使用 {@code clickhouse-jdbc} 驱动（HTTP 协议），默认端口 8123。
 * <p>
 * 注意：ClickHouse 不支持传统 ACID 事务，JDBC 驱动会静默忽略
 * {@code setAutoCommit(false)} 和 {@code rollback()} 调用。
 */
public class ClickhouseClient implements DatabaseClient {

    /**
     * 建立 ClickHouse JDBC 连接。
     */
    @Override
    public Connection getConnection(String host, int port, String database, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("ru.yandex.clickhouse.ClickHouseDriver");
        String url = getJdbcUrl(host, port, database, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 构建 ClickHouse JDBC URL。
     * <p>
     * 格式：{@code jdbc:clickhouse://host:port/database}
     * <p>
     * ClickHouse JDBC 驱动使用 HTTP 协议通信，端口应为 HTTP 端口（默认 8123），
     * 而非原生 TCP 端口（9000）。
     * <p>
     * SSL 模式下附加 {@code ssl=true} 和 {@code sslmode=strict} 参数。
     */
    @Override
    public String getJdbcUrl(String host, int port, String database, String version, SslConfig ssl) {
        StringBuilder url = new StringBuilder("jdbc:clickhouse://")
                .append(host).append(":").append(port);
        if (database != null && !database.isEmpty()) {
            url.append("/").append(database);
        }
        if (ssl != null && ssl.hasAny()) {
            url.append("?ssl=true&sslmode=strict");
            if (ssl.getCaPath() != null) {
                url.append("&sslrootcert=").append(toAbsolutePath(ssl.getCaPath()));
            }
        }
        return url.toString();
    }

    /**
     * 将相对路径转为绝对路径。若已是绝对路径则直接返回。
     */
    private String toAbsolutePath(String path) {
        java.io.File file = new java.io.File(path);
        return file.isAbsolute() ? path : file.getAbsolutePath();
    }
}
