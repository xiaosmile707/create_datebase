package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class OracleClient implements DatabaseClient {

    @Override
    public Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String url = getJdbcUrl(host, port, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public String getJdbcUrl(String host, int port, String version, SslConfig ssl) {
        if (ssl != null && ssl.hasAny()) {
            return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST="
                    + host + ")(PORT=" + port + "))(CONNECT_DATA=(SERVICE_NAME=FREE)))";
        }
        return "jdbc:oracle:thin:@" + host + ":" + port + ":FREE";
    }
}
