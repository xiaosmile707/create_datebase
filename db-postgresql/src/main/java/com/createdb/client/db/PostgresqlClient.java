package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class PostgresqlClient implements DatabaseClient {

    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        StringBuilder url = new StringBuilder("jdbc:postgresql://")
                .append(host).append(":").append(port).append("/postgres");
        if (ssl != null && ssl.hasAny()) {
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
