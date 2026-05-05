package com.createdb.client.config;

/**
 * SSL/TLS 证书路径配置，用于建立加密的数据库连接。
 * <p>
 * 三个证书字段均为可选：若都未配置，则 JDBC 连接不启用 SSL。
 * 证书文件统一存放于项目根目录的 {@code certs/<type>/} 下。
 */
public class SslConfig {

    /** CA 根证书路径 */
    private final String caPath;
    /** 客户端证书路径 */
    private final String certPath;
    /** 客户端私钥路径 */
    private final String keyPath;

    /**
     * @param caPath   CA 证书文件路径，可为 null
     * @param certPath 客户端证书文件路径，可为 null
     * @param keyPath  客户端私钥文件路径，可为 null
     */
    public SslConfig(String caPath, String certPath, String keyPath) {
        this.caPath = caPath;
        this.certPath = certPath;
        this.keyPath = keyPath;
    }

    public String getCaPath() {
        return caPath;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getKeyPath() {
        return keyPath;
    }

    /**
     * 判断是否配置了至少一个证书字段。
     *
     * @return 若 ca / cert / key 任一不为 null 则返回 true
     */
    public boolean hasAny() {
        return caPath != null || certPath != null || keyPath != null;
    }
}
