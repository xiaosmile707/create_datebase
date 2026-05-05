package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;

/**
 * 数据库客户端统一接口，定义 JDBC 连接获取和 URL 构建的契约。
 * <p>
 * 每个数据库模块提供自己的实现（如 {@code MysqlClient}、{@code PostgresqlClient}），
 * 通过 JDBC DriverManager 建立连接并执行 SQL 操作。
 */
public interface DatabaseClient {

    /**
     * 建立数据库 JDBC 连接。
     *
     * @param host     数据库主机地址
     * @param port     数据库端口
     * @param version  数据库版本号
     * @param user     登录用户名
     * @param password 登录密码
     * @param ssl      SSL 配置，为 null 则不启用 SSL
     * @return JDBC 连接对象
     * @throws Exception 驱动加载失败或连接被拒时抛出
     */
    Connection getConnection(String host, int port, String version, String user, String password, SslConfig ssl) throws Exception;

    /**
     * 构建该数据库的 JDBC URL 字符串。
     *
     * @param host    数据库主机地址
     * @param port    数据库端口
     * @param version 数据库版本号
     * @param ssl     SSL 配置，为 null 则不附加 SSL 参数
     * @return 完整的 JDBC URL
     */
    String getJdbcUrl(String host, int port, String version, SslConfig ssl);
}
