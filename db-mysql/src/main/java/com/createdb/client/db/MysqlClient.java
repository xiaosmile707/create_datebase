package com.createdb.client.db;

import com.createdb.client.config.SslConfig;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * MySQL 数据库客户端，通过 JDBC 连接远程 MySQL 实例。
 * <p>
 * 使用 {@code mysql-connector-j} 驱动，默认禁用 SSL，
 * 当配置 SSL 证书时自动启用加密连接。
 */
public class MysqlClient implements DatabaseClient {

    /**
     * 建立 MySQL JDBC 连接。
     */
    @Override
    public Connection getConnection(String host, int port, String database, String version, String user, String password, SslConfig ssl) throws Exception {
        // 加载 MySQL JDBC 驱动
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = getJdbcUrl(host, port, database, version, ssl);
        return DriverManager.getConnection(url, user, password);
    }

    /** KeyStore 默认密码（与生成 JKS 时使用的密码一致） */
    private static final String KEYSTORE_PASSWORD = "changeit";

    /**
     * 构建 MySQL JDBC URL。
     * <p>
     * 无 SSL 时附加 {@code useSSL=false} 和 {@code allowPublicKeyRetrieval=true}，
     * 避免连接警告。配置 SSL 后切换为加密参数，包含 KeyStore 密码。
     * <p>
     * SSL 模式下的 JDBC 参数：
     * <ul>
     *   <li>{@code trustCertificateKeyStoreUrl} — CA TrustStore（JKS）路径</li>
     *   <li>{@code trustCertificateKeyStorePassword} — TrustStore 密码</li>
     *   <li>{@code clientCertificateKeyStoreUrl} — 客户端 KeyStore（JKS）路径</li>
     *   <li>{@code clientCertificateKeyStorePassword} — 客户端 KeyStore 密码</li>
     * </ul>
     */
    @Override
    public String getJdbcUrl(String host, int port, String database, String version, SslConfig ssl) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(host).append(":").append(port);
        if (database != null && !database.isEmpty()) {
            url.append("/").append(database);
        }
        if (ssl != null && ssl.hasAny()) {
            // SSL 加密连接
            url.append("?useSSL=true&requireSSL=true&verifyServerCertificate=true");
            if (ssl.getCaPath() != null) {
                url.append("&trustCertificateKeyStoreUrl=file:").append(toAbsolutePath(ssl.getCaPath()));
                url.append("&trustCertificateKeyStorePassword=").append(KEYSTORE_PASSWORD);
            }
            if (ssl.getCertPath() != null) {
                url.append("&clientCertificateKeyStoreUrl=file:").append(toAbsolutePath(ssl.getCertPath()));
                url.append("&clientCertificateKeyStorePassword=").append(KEYSTORE_PASSWORD);
            }
        } else {
            // 非 SSL 连接，禁用 SSL 并允许公钥检索
            url.append("?useSSL=false&allowPublicKeyRetrieval=true");
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
