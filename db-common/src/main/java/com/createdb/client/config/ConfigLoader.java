package com.createdb.client.config;

import com.alibaba.fastjson2.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 配置加载器，从 {@code db-connection.json} 统一加载所有数据库连接参数。
 *
 * <h3>配置加载优先级</h3>
 * <ul>
 *   <li><b>连接信息</b>（host/port/version/user/database/type/containerName/password/ssl）：
 *       classpath {@code db-connection.json} → 文件系统 → 默认值</li>
 *   <li><b>密码覆盖</b>：
 *       环境变量 {@code DB_{TYPE}_PASSWORD} 可覆盖 JSON 中的 password 字段</li>
 * </ul>
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String CONNECTION_JSON = "db-connection.json";
    private static final String ENV_PASSWORD_PREFIX = "DB_";
    private static final String ENV_PASSWORD_SUFFIX = "_PASSWORD";

    /** JSON 文件对应的 POJO，供 FastJSON 2 反序列化使用。 */
    private static class ConnectionConfig {
        public String host;
        public int port;
        public String type;
        public String version;
        public String containerName;
        public String user;
        public String password;
        public String database;
        public SslConfigJson ssl;
    }

    /** JSON 中可选的 SSL 配置块。 */
    private static class SslConfigJson {
        public String caPath;
        public String certPath;
        public String keyPath;
    }

    private final String host;
    private final int port;
    private final String version;
    private final String user;
    private final String password;
    private final String database;
    private final String type;
    private final String containerName;
    private final SslConfig sslConfig;

    /**
     * 使用默认 JSON 路径的构造器。
     */
    public ConfigLoader() {
        this(CONNECTION_JSON);
    }

    /**
     * 指定 JSON 文件路径的构造器。
     *
     * @param connectionJsonPath db-connection.json 路径（classpath 或文件系统相对路径）
     */
    public ConfigLoader(String connectionJsonPath) {
        ConnectionConfig cfg = parseConfig(connectionJsonPath);

        this.host = (cfg != null && cfg.host != null && !cfg.host.isEmpty()) ? cfg.host : "localhost";
        this.port = (cfg != null && cfg.port > 0) ? cfg.port : 0;
        this.version = (cfg != null && cfg.version != null) ? cfg.version : "";
        this.user = (cfg != null && cfg.user != null) ? cfg.user : "";
        this.database = (cfg != null && cfg.database != null) ? cfg.database : "";
        this.type = (cfg != null && cfg.type != null) ? cfg.type : "";
        this.containerName = (cfg != null && cfg.containerName != null) ? cfg.containerName : "";

        // 密码：环境变量 > JSON password 字段 > 空字符串
        this.password = resolvePassword(this.type, cfg);

        // SSL：JSON ssl 字段 > null
        this.sslConfig = resolveSslConfig(cfg);
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getVersion() { return version; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
    public String getType() { return type; }
    public String getContainerName() { return containerName; }

    /**
     * 获取 SSL 配置。若 JSON 中未配置 ssl 字段或全部为空，
     * 则返回 null 表示不启用 SSL。
     *
     * @return SSL 配置对象或 null
     */
    public SslConfig getSslConfig() {
        return sslConfig;
    }

    // ==================== 密码解析 ====================

    /**
     * 按优先级加载密码：环境变量 → JSON password 字段 → 空字符串。
     * <p>
     * 环境变量命名规则：{@code DB_{TYPE}_PASSWORD}，type 转为大写。
     * 例如 MySQL → {@code DB_MYSQL_PASSWORD}。
     */
    private static String resolvePassword(String dbType, ConnectionConfig cfg) {
        // 1. 环境变量（最高优先级，覆盖 JSON 中的密码）
        String envKey = ENV_PASSWORD_PREFIX + dbType.toUpperCase() + ENV_PASSWORD_SUFFIX;
        String envPassword = System.getenv(envKey);
        if (envPassword != null && !envPassword.isEmpty()) {
            log.debug("Password loaded from environment variable: {}", envKey);
            return envPassword;
        }

        // 2. JSON password 字段
        if (cfg != null && cfg.password != null && !cfg.password.isEmpty()) {
            log.debug("Password loaded from db-connection.json for type: {}", dbType);
            return cfg.password;
        }

        // 3. 兜底：空字符串
        log.warn("No password found from env or db-connection.json for type: {}. Using empty password.", dbType);
        return "";
    }

    // ==================== SSL 解析 ====================

    /**
     * 从 JSON {@code ssl} 字段加载 SSL 证书路径，未配置则返回 null。
     */
    private static SslConfig resolveSslConfig(ConnectionConfig cfg) {
        if (cfg == null || cfg.ssl == null) {
            return null;
        }

        String caPath = emptyToNull(cfg.ssl.caPath);
        String certPath = emptyToNull(cfg.ssl.certPath);
        String keyPath = emptyToNull(cfg.ssl.keyPath);

        if (caPath != null || certPath != null || keyPath != null) {
            log.debug("SSL enabled (ca={}, cert={}, key={})", caPath, certPath, keyPath);
            return new SslConfig(caPath, certPath, keyPath);
        }
        return null;
    }

    private static String emptyToNull(String value) {
        return (value != null && !value.isEmpty()) ? value : null;
    }

    // ==================== 文件加载 ====================

    /**
     * 解析 db-connection.json。
     */
    private static ConnectionConfig parseConfig(String path) {
        String json = readFile(path);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, ConnectionConfig.class);
        } catch (Exception e) {
            log.warn("Failed to parse connection config: {} - {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 读取文件内容：优先从 classpath 加载，fallback 到文件系统。
     */
    private static String readFile(String path) {
        // 1. classpath 优先
        InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path);
        if (is != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String content = reader.lines().collect(Collectors.joining());
                log.debug("Loaded config from classpath: {}", path);
                return content;
            } catch (IOException e) {
                log.warn("Failed to read config from classpath: {} - {}", path, e.getMessage());
            }
        }

        // 2. 文件系统 fallback
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(path))))) {
            String content = reader.lines().collect(Collectors.joining());
            log.debug("Loaded config from filesystem: {}", path);
            return content;
        } catch (IOException e) {
            log.warn("Failed to load connection config: {}", path);
            return null;
        }
    }

}
