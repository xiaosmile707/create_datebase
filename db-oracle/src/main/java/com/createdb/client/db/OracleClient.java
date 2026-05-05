package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Oracle 数据库客户端，通过 JDBC 连接远程 Oracle 实例。
 * <p>
 * 使用 {@code ojdbc8} 驱动，默认连接 {@code FREE} 服务名（Oracle 免费版）。
 * SSL 启用时使用 TCPS 协议和完整连接描述符。
 */
public class OracleClient implements DatabaseClient {

    /**
     * 建立 Oracle JDBC 连接。
     */
    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        // 加载 Oracle JDBC 驱动
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 构建 Oracle JDBC URL。
     * <p>
     * 非 SSL 使用简易格式：{@code jdbc:oracle:thin:@host:port:FREE}。
     * SSL 启用时使用完整 TNS 连接描述符和 TCPS 协议。
     */
    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        if (ssl != null && ssl.hasAny()) {
            // SSL TCPS 加密连接，使用完整连接描述符
            return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST="
                    + host + ")(PORT=" + port + "))(CONNECT_DATA=(SERVICE_NAME=FREE)))";
        }
        // 非 SSL 简易连接格式
        return "jdbc:oracle:thin:@" + host + ":" + port + ":FREE";
    }
}
