package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class H2Client implements DatabaseClient {

    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("org.h2.Driver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        return "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    }
}
