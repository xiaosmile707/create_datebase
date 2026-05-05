package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class MysqlClient implements DatabaseClient {

    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(host).append(":").append(port);
        if (ssl != null && ssl.hasAny()) {
            url.append("?useSSL=true&requireSSL=true&verifyServerCertificate=true");
            if (ssl.getCaPath() != null) {
                url.append("&trustCertificateKeyStoreUrl=file:").append(ssl.getCaPath());
            }
            if (ssl.getCertPath() != null) {
                url.append("&clientCertificateKeyStoreUrl=file:").append(ssl.getCertPath());
            }
        } else {
            url.append("?useSSL=false&allowPublicKeyRetrieval=true");
        }
        return url.toString();
    }
}
