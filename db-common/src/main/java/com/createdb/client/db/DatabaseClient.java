package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;

public interface DatabaseClient {

    Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception;

    String getJdbcUrl(String host, int port, String version, SslConfig ssl);
}
