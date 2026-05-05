package com.createdb.client.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 配置加载器，统一管理数据库连接参数的读取。
 * <p>
 * 采用双源策略：
 * <ol>
 *   <li><b>db-secret.properties</b>：主要配置源，含 host/port/version/user/password/ssl 等</li>
 *   <li><b>db-connection.json</b>：AI Skill 生成的连接信息，作为 fallback</li>
 * </ol>
 * 读取优先级：{@code db-secret.properties} &gt; {@code db-connection.json} &gt; 默认值。
 * 密码仅从 {@code db-secret.properties} 读取，不会出现在 JSON 文件中。
 */
public class ConfigLoader {

    /** 默认数据库属性文件路径 */
    private static final String DEFAULT_CONFIG_PATH = "db-secret.properties";
    /** AI 生成的连接信息 JSON 文件路径 */
    private static final String CONNECTION_JSON = "db-connection.json";

    private final Properties props;
    private final String prefix;
    /** 从 db-connection.json 解析的 host（fallback） */
    private final String jsonHost;
    /** 从 db-connection.json 解析的 port（fallback） */
    private final int jsonPort;
    /** 从 db-connection.json 解析的 version（fallback） */
    private final String jsonVersion;
    /** 从 db-connection.json 解析的 user（fallback） */
    private final String jsonUser;
    /** 从 db-connection.json 解析的 database（fallback） */
    private final String jsonDatabase;

    /**
     * 使用默认路径的便捷构造器。
     *
     * @param type 数据库类型标识（如 "mysql"、"postgresql"）
     */
    public ConfigLoader(String type) {
        this(DEFAULT_CONFIG_PATH, type, CONNECTION_JSON);
    }

    /**
     * 指定属性文件路径的构造器。
     *
     * @param configPath 属性文件路径
     * @param type       数据库类型标识
     */
    public ConfigLoader(String configPath, String type) {
        this(configPath, type, CONNECTION_JSON);
    }

    /**
     * 完整构造器，同时加载属性文件和 JSON 连接信息。
     *
     * @param configPath          属性文件路径
     * @param type                数据库类型标识
     * @param connectionJsonPath  db-connection.json 路径，为 null 则不加载
     */
    public ConfigLoader(String configPath, String type, String connectionJsonPath) {
        this.prefix = type + ".";
        props = new Properties();
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("Warning: Failed to load config: " + configPath + ", will try db-connection.json");
        }

        // 从 db-connection.json 读取 AI 生成的连接信息作为 fallback
        String host = null;
        int port = -1;
        String version = null;
        String user = null;
        String database = null;
        if (connectionJsonPath != null) {
            String json = readFile(connectionJsonPath);
            if (json != null) {
                host = extractJsonString(json, "host");
                String portStr = extractJsonString(json, "port");
                if (portStr != null) {
                    try { port = Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
                }
                version = extractJsonString(json, "version");
                user = extractJsonString(json, "user");
                database = extractJsonString(json, "database");
            }
        }
        this.jsonHost = host;
        this.jsonPort = port;
        this.jsonVersion = version;
        this.jsonUser = user;
        this.jsonDatabase = database;
    }

    /**
     * 获取数据库主机地址（props 优先，JSON fallback，最后默认 localhost）。
     */
    public String getHost() {
        String v = props.getProperty(prefix + "host");
        if (v != null && !v.isEmpty()) return v;
        return jsonHost != null ? jsonHost : "localhost";
    }

    /**
     * 获取数据库端口（props 优先，JSON fallback，最后默认 0）。
     */
    public int getPort() {
        String v = props.getProperty(prefix + "port");
        if (v != null && !v.isEmpty()) return Integer.parseInt(v);
        return jsonPort > 0 ? jsonPort : 0;
    }

    /**
     * 获取数据库版本号（props 优先，JSON fallback，最后默认空串）。
     */
    public String getVersion() {
        String v = props.getProperty(prefix + "version");
        if (v != null && !v.isEmpty()) return v;
        return jsonVersion != null ? jsonVersion : "";
    }

    /**
     * 获取登录用户名（props 优先，JSON fallback，最后默认空串）。
     */
    public String getUser() {
        String v = props.getProperty(prefix + "user");
        if (v != null && !v.isEmpty()) return v;
        return jsonUser != null ? jsonUser : "";
    }

    /**
     * 获取登录密码（仅从 props 读取，JSON 不含密码）。
     */
    public String getPassword() {
        return props.getProperty(prefix + "password", "");
    }

    /**
     * 获取数据库名（props 优先，JSON fallback，最后默认空串）。
     */
    public String getDatabase() {
        String v = props.getProperty(prefix + "database");
        if (v != null && !v.isEmpty()) return v;
        return jsonDatabase != null ? jsonDatabase : "";
    }

    /**
     * 获取 SSL 配置，若未配置任何证书则返回 null。
     */
    public SslConfig getSslConfig() {
        String ca = props.getProperty(prefix + "ssl.ca");
        String cert = props.getProperty(prefix + "ssl.cert");
        String key = props.getProperty(prefix + "ssl.key");
        if (ca == null && cert == null && key == null) {
            return null;
        }
        return new SslConfig(ca, cert, key);
    }

    private static String readFile(String path) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            return null;
        }
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = json.indexOf(pattern);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(":", keyIdx + pattern.length());
        if (colonIdx < 0) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) {
            String num = json.substring(colonIdx + 1).trim().split("[,\\}]")[0].trim();
            return num;
        }
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
