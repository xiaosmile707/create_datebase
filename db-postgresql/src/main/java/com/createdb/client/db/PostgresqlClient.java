package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * PostgreSQL 数据库客户端，通过 JDBC 连接远程 PostgreSQL 实例。
 * <p>
 * 使用 {@code postgresql} 驱动，默认连接 {@code postgres} 数据库，
 * 当配置 SSL 证书时自动启用 {@code verify-ca} 模式。
 */
public class PostgresqlClient implements DatabaseClient {

    /**
     * 建立 PostgreSQL JDBC 连接。
     */
    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        // 加载 PostgreSQL JDBC 驱动
        Class.forName("org.postgresql.Driver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 构建 PostgreSQL JDBC URL。
     * <p>
     * 默认连接 {@code postgres} 数据库，SSL 启用时使用 {@code verify-ca} 模式
     * 并附加 CA / 客户端证书 / 私钥路径。
     */
    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        StringBuilder url = new StringBuilder("jdbc:postgresql://")
                .append(host).append(":").append(port).append("/postgres");
        if (ssl != null && ssl.hasAny()) {
            // SSL 加密连接，证书验证模式
            url.append("?ssl=true&sslmode=verify-ca");
            if (ssl.getCaPath() != null) {
                url.append("&sslrootcert=").append(ssl.getCaPath());
            }
            if (ssl.getCertPath() != null) {
                url.append("&sslcert=").append(ssl.getCertPath());
            }
            if (ssl.getKeyPath() != null) {
                url.append("&sslkey=").append(ssl.getKeyPath());
            }
        }
        return url.toString();
    }
}
